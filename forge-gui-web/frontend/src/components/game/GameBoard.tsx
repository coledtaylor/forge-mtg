import { useEffect, useState, useCallback } from 'react'
import { Loader2 } from 'lucide-react'
import { useHotkeys } from 'react-hotkeys-hook'
import { useGameStore } from '../../stores/gameStore'
import { useGameWebSocket } from '../../hooks/useGameWebSocket'
import type { GameStartConfig } from '../../types/game'
import { PlayerInfoBar } from './PlayerInfoBar'
import { PhaseStrip } from './PhaseStrip'
import { RightPanel } from './RightPanel'
import { ZonePile } from './ZonePile'
import { BattlefieldZone } from './BattlefieldZone'
import { HandZone } from './HandZone'
import { ActionBar } from './ActionBar'
import { CombatOverlay } from './CombatOverlay'
import { GameOverScreen } from './GameOverScreen'
import { Skeleton } from '../ui/skeleton'
import { Button } from '../ui/button'

interface GameBoardProps {
  gameId: string
  gameConfig?: GameStartConfig
  onExit: () => void
}

export function GameBoard({ gameId, gameConfig, onExit }: GameBoardProps) {
  const wsRef = useGameWebSocket(gameId, gameConfig)

  const players = useGameStore((s) => s.players)
  const humanPlayerId = useGameStore((s) => s.humanPlayerId)
  const connected = useGameStore((s) => s.connected)
  const error = useGameStore((s) => s.error)
  const buttons = useGameStore((s) => s.buttons)
  const prompt = useGameStore((s) => s.prompt)
  const targetingState = useGameStore((s) => s.targetingState)
  const setTargetingState = useGameStore((s) => s.setTargetingState)
  const toggleTargetSelection = useGameStore((s) => s.toggleTargetSelection)

  // Enter targeting mode when PROMPT_CHOICE arrives with choiceIds matching battlefield/hand cards
  useEffect(() => {
    if (!prompt || prompt.type !== 'PROMPT_CHOICE') {
      // Clear targeting when prompt clears
      if (targetingState) setTargetingState(null)
      return
    }

    const choiceIds = prompt.payload.choiceIds
    if (!choiceIds || choiceIds.length === 0) {
      // No card IDs -- not a targeting prompt, let ChoiceDialog handle it
      return
    }

    const cards = useGameStore.getState().cards
    // Check if any choiceIds match battlefield or hand cards
    const battlefieldOrHandIds = choiceIds.filter((id) => {
      if (id === -1) return false
      const card = cards[id]
      return card && (card.zoneType === 'Battlefield' || card.zoneType === 'Hand')
    })

    if (battlefieldOrHandIds.length === 0) {
      // Choices are not cards on the battlefield/hand -- use ChoiceDialog
      return
    }

    // Enter targeting mode
    setTargetingState({
      validTargetIds: choiceIds.filter((id) => id !== -1),
      selectedTargetIds: [],
      min: prompt.payload.min ?? 1,
      max: prompt.payload.max ?? 1,
      promptInputId: prompt.inputId,
    })
  }, [prompt]) // eslint-disable-line react-hooks/exhaustive-deps

  // Confirm multi-target targeting
  const confirmTargeting = useCallback(() => {
    const currentTargeting = useGameStore.getState().targetingState
    const currentPrompt = useGameStore.getState().prompt
    if (!currentTargeting || !currentPrompt) return

    const choiceIds = currentPrompt.payload.choiceIds ?? []
    const indices = currentTargeting.selectedTargetIds
      .map((id) => choiceIds.indexOf(id))
      .filter((i) => i >= 0)

    wsRef.current?.sendChoiceResponse(currentPrompt.inputId, indices)
    useGameStore.getState().setPrompt(null)
    useGameStore.getState().setTargetingState(null)
  }, [wsRef])

  // Cancel targeting
  const cancelTargeting = useCallback(() => {
    wsRef.current?.sendButtonCancel()
    useGameStore.getState().setTargetingState(null)
  }, [wsRef])

  // Keyboard shortcuts: Space/Enter to pass priority or confirm, Escape to cancel
  // Disable during PROMPT_CHOICE (needs specific choice) and PROMPT_AMOUNT (needs number input)
  useHotkeys('space, enter', () => {
    wsRef.current?.sendButtonOk()
  }, { enabled: buttons !== null && buttons.enable1 && prompt?.type !== 'PROMPT_CHOICE' && prompt?.type !== 'PROMPT_AMOUNT' })

  useHotkeys('z', () => {
    wsRef.current?.sendUndo()
  }, { enabled: buttons !== null && buttons.canUndo === true && prompt?.type !== 'PROMPT_CHOICE' && prompt?.type !== 'PROMPT_AMOUNT' })

  useHotkeys('escape', () => {
    if (useGameStore.getState().targetingState) {
      cancelTargeting()
    } else {
      wsRef.current?.sendButtonCancel()
    }
  }, { enabled: (buttons !== null && buttons.enable2) || targetingState !== null })

  // Derive human and opponent players
  const playerIds = Object.keys(players).map(Number)
  const humanPlayer = humanPlayerId !== null ? players[humanPlayerId] : undefined
  const opponentPlayer = playerIds.find((id) => id !== humanPlayerId) !== undefined
    ? players[playerIds.find((id) => id !== humanPlayerId)!]
    : undefined

  // Check if a player has a command zone (Commander format)
  const hasCommandZone = (playerId: number) => {
    const p = players[playerId]
    return p && p.zones.Command && p.zones.Command.length > 0
  }

  // Auto-dismiss non-fatal errors after 5 seconds
  const [dismissedError, setDismissedError] = useState<string | null>(null)

  useEffect(() => {
    if (error && error !== 'Could not reconnect' && error !== dismissedError) {
      const timer = setTimeout(() => {
        setDismissedError(error)
      }, 5000)
      return () => clearTimeout(timer)
    }
  }, [error, dismissedError])

  const visibleError = error && error !== dismissedError ? error : null

  // Battlefield card click: select card (for tapping mana, activating abilities, targeting)
  const handleBattlefieldCardClick = useCallback(
    (cardId: number) => {
      const currentTargeting = useGameStore.getState().targetingState
      const currentPrompt = useGameStore.getState().prompt

      if (currentTargeting && currentPrompt) {
        // Check if this card is a valid target
        if (!currentTargeting.validTargetIds.includes(cardId)) return

        if (currentTargeting.max === 1) {
          // Single-target: auto-confirm immediately
          const choiceIds = currentPrompt.payload.choiceIds ?? []
          const choiceIndex = choiceIds.indexOf(cardId)
          if (choiceIndex >= 0) {
            wsRef.current?.sendChoiceResponse(currentPrompt.inputId, [choiceIndex])
            useGameStore.getState().setPrompt(null)
            useGameStore.getState().setTargetingState(null)
          }
          return
        }

        // Multi-target: toggle selection
        toggleTargetSelection(cardId)
        return
      }

      // Default: select card via game controller (tap land, activate ability, etc.)
      wsRef.current?.sendSelectCard(cardId)
    },
    [wsRef, toggleTargetSelection]
  )

  // Hand card click: select card to play/cast
  const handleHandCardClick = useCallback(
    (cardId: number) => {
      wsRef.current?.sendSelectCard(cardId)
    },
    [wsRef]
  )

  // Playable indicator: when buttons active and enable1 is true
  const showPlayableIndicators = buttons !== null && buttons.enable1

  // Loading state
  if (!connected && Object.keys(players).length === 0) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-background text-foreground gap-4">
        <Skeleton className="h-9 w-full max-w-3xl" />
        <Skeleton className="h-64 w-full max-w-3xl" />
        <Skeleton className="h-8 w-full max-w-md" />
        <Skeleton className="h-64 w-full max-w-3xl" />
        <Skeleton className="h-9 w-full max-w-3xl" />
        <p className="text-sm text-muted-foreground">Connecting to game...</p>
        <button
          onClick={onExit}
          className="text-sm text-muted-foreground hover:text-foreground underline transition-colors"
        >
          Back to deck list
        </button>
      </div>
    )
  }

  return (
    <div className="h-screen w-screen bg-background overflow-hidden relative" data-game-board>
      {/* Error banner */}
      {visibleError && (
        <div className="absolute top-0 left-0 right-0 z-50 bg-amber-900/80 text-amber-200 text-sm text-center py-1 px-4">
          {visibleError}
        </div>
      )}

      {/* Reconnecting banner */}
      {!connected && Object.keys(players).length > 0 && (
        <div className="absolute top-0 left-0 right-0 z-50 bg-amber-900/80 text-amber-200 text-sm text-center py-1 px-4 flex items-center justify-center gap-2">
          <Loader2 className="h-4 w-4 animate-spin" />
          Connection lost. Reconnecting...
        </div>
      )}

      {/* CSS Grid layout */}
      <div
        className="h-full w-full grid relative"
        style={{
          gridTemplateRows: '36px 1fr 32px 1fr 44px auto 36px',
          gridTemplateColumns: '1fr 220px',
        }}
      >
        {/* Row 1: Opponent info bar (col-span-2) */}
        <div className="col-span-2">
          <PlayerInfoBar player={opponentPlayer} isOpponent />
        </div>

        {/* Row 2, Col 1: Opponent Battlefield */}
        <BattlefieldZone
          playerId={opponentPlayer?.id ?? -1}
          flipped
          className="row-start-2 col-start-1"
          onCardClick={handleBattlefieldCardClick}
        />

        {/* Row 2-4, Col 2: Right Panel (Stack + Log tabs) */}
        <div className="row-start-2 row-end-5 col-start-2 border-l border-border bg-card">
          <RightPanel className="h-full" />
        </div>

        {/* Row 3: Phase strip + zone piles */}
        <div className="col-start-1 col-end-2 flex items-center justify-between px-2 gap-2">
          {/* Opponent graveyard + exile + command */}
          <div className="flex items-center gap-2 shrink-0">
            {opponentPlayer && (
              <>
                {hasCommandZone(opponentPlayer.id) && (
                  <ZonePile playerId={opponentPlayer.id} zone="Command" />
                )}
                <ZonePile playerId={opponentPlayer.id} zone="Graveyard" />
                <ZonePile playerId={opponentPlayer.id} zone="Exile" />
              </>
            )}
          </div>

          {/* Phase strip (center) */}
          <div className="flex-1 min-w-0">
            <PhaseStrip />
          </div>

          {/* Opponent library */}
          <div className="flex items-center gap-2 shrink-0">
            {opponentPlayer && (
              <ZonePile playerId={opponentPlayer.id} zone="Library" />
            )}
          </div>
        </div>

        {/* Row 4, Col 1: Player Battlefield */}
        <div className="flex flex-col col-start-1">
          <BattlefieldZone
            playerId={humanPlayer?.id ?? -1}
            className="flex-1"
            onCardClick={handleBattlefieldCardClick}
          />
          {/* Player zone piles at bottom of battlefield */}
          {humanPlayer && (
            <div className="flex items-center gap-2 px-2 pb-1">
              {hasCommandZone(humanPlayer.id) && (
                <ZonePile playerId={humanPlayer.id} zone="Command" />
              )}
              <ZonePile playerId={humanPlayer.id} zone="Graveyard" />
              <ZonePile playerId={humanPlayer.id} zone="Exile" />
              <span className="flex-1" />
              <ZonePile playerId={humanPlayer.id} zone="Library" />
            </div>
          )}
        </div>

        {/* Row 5: Action Bar or Targeting Bar (col-span-2) */}
        {targetingState && targetingState.max > 1 ? (
          <div
            className="h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary col-span-2"
            style={{ animation: 'priority-pulse 2s ease-in-out infinite' }}
          >
            <span className="text-sm text-foreground">
              {prompt?.payload.message ?? 'Select targets'}:{' '}
              {targetingState.selectedTargetIds.length}/{targetingState.max} selected
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="default"
                size="sm"
                disabled={targetingState.selectedTargetIds.length < targetingState.min}
                onClick={confirmTargeting}
              >
                Confirm
              </Button>
              <Button variant="outline" size="sm" onClick={cancelTargeting}>
                Cancel <span className="text-xs opacity-60 ml-1">[Esc]</span>
              </Button>
            </div>
          </div>
        ) : (
          <ActionBar wsRef={wsRef} className="col-span-2" />
        )}

        {/* Row 6: Hand (col-span-2) */}
        <HandZone
          className="col-span-2 max-h-[160px] bg-card/50 border-t border-border"
          onCardClick={handleHandCardClick}
          isPlayable={showPlayableIndicators}
        />

        {/* Row 7: Player info bar (col-span-2) */}
        <div className="col-span-2">
          <PlayerInfoBar player={humanPlayer} />
        </div>

        {/* Combat overlay: SVG arrows over battlefield area */}
        <CombatOverlay />
      </div>

      {/* Game over overlay */}
      <GameOverScreen onReturnToLobby={onExit} />
    </div>
  )
}
