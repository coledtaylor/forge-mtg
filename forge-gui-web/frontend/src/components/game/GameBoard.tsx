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

  // Keyboard shortcuts: Space/Enter to pass priority or confirm, Escape to cancel
  // Disable during PROMPT_CHOICE (needs specific choice) and PROMPT_AMOUNT (needs number input)
  useHotkeys('space, enter', () => {
    wsRef.current?.sendButtonOk()
  }, { enabled: buttons !== null && buttons.enable1 && prompt?.type !== 'PROMPT_CHOICE' && prompt?.type !== 'PROMPT_AMOUNT' })

  useHotkeys('escape', () => {
    wsRef.current?.sendButtonCancel()
  }, { enabled: buttons !== null && buttons.enable2 })

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

  // Targeting mode: when PROMPT_CHOICE is active, battlefield cards become clickable
  const isTargetingMode = prompt?.type === 'PROMPT_CHOICE'

  // Battlefield card click: select card (for tapping mana, activating abilities, targeting)
  const handleBattlefieldCardClick = useCallback(
    (cardId: number) => {
      if (isTargetingMode && prompt) {
        // Try to match card to a prompt choice first
        const choices = prompt.payload.choices ?? prompt.payload.options ?? []
        const cards = useGameStore.getState().cards
        const card = cards[cardId]
        if (card) {
          const choiceIndex = choices.findIndex(
            (c) => c.toLowerCase().includes(card.name.toLowerCase())
          )
          if (choiceIndex >= 0) {
            wsRef.current?.sendChoiceResponse(prompt.inputId, [choiceIndex])
            useGameStore.getState().setPrompt(null)
            return
          }
        }
      }
      // Default: select card via game controller (tap land, activate ability, etc.)
      wsRef.current?.sendSelectCard(cardId)
    },
    [isTargetingMode, prompt, wsRef]
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

        {/* Row 5: Action Bar (col-span-2) */}
        <ActionBar wsRef={wsRef} className="col-span-2" />

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
