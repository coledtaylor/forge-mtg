import { useEffect, useState } from 'react'
import { Loader2 } from 'lucide-react'
import { useGameStore } from '../../stores/gameStore'
import { useGameWebSocket } from '../../hooks/useGameWebSocket'
import { PlayerInfoBar } from './PlayerInfoBar'
import { PhaseStrip } from './PhaseStrip'
import { StackPanel } from './StackPanel'
import { Skeleton } from '../ui/skeleton'

interface GameBoardProps {
  gameId: string
  onExit: () => void
}

export function GameBoard({ gameId, onExit }: GameBoardProps) {
  useGameWebSocket(gameId)

  const players = useGameStore((s) => s.players)
  const humanPlayerId = useGameStore((s) => s.humanPlayerId)
  const connected = useGameStore((s) => s.connected)
  const error = useGameStore((s) => s.error)
  const gameOver = useGameStore((s) => s.gameOver)

  // Derive human and opponent players
  const playerIds = Object.keys(players).map(Number)
  const humanPlayer = humanPlayerId !== null ? players[humanPlayerId] : undefined
  const opponentPlayer = playerIds.find((id) => id !== humanPlayerId) !== undefined
    ? players[playerIds.find((id) => id !== humanPlayerId)!]
    : undefined

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
    <div className="h-screen w-screen bg-background overflow-hidden relative">
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
        className="h-full w-full grid"
        style={{
          gridTemplateRows: '36px 1fr 32px 1fr 44px auto 36px',
          gridTemplateColumns: '1fr 220px',
        }}
      >
        {/* Row 1: Opponent info bar (col-span-2) */}
        <div className="col-span-2">
          <PlayerInfoBar player={opponentPlayer} isOpponent />
        </div>

        {/* Row 2, Col 1: Opponent Battlefield placeholder */}
        <div className="flex items-center justify-center text-sm text-muted-foreground border border-border/30 m-1 rounded">
          Opponent Battlefield
        </div>

        {/* Row 2-4, Col 2: Stack Panel */}
        <div className="row-start-2 row-end-5 col-start-2">
          <StackPanel className="h-full" />
        </div>

        {/* Row 3: Phase strip + zone piles (col-span-2 effectively col 1 only since stack spans col 2) */}
        <div className="col-start-1 col-end-2">
          <PhaseStrip />
        </div>

        {/* Row 4, Col 1: Player Battlefield placeholder */}
        <div className="flex items-center justify-center text-sm text-muted-foreground border border-border/30 m-1 rounded">
          Player Battlefield
        </div>

        {/* Row 5: Action Bar placeholder (col-span-2) */}
        <div className="col-span-2 flex items-center justify-center text-sm text-muted-foreground bg-card border-t border-border">
          Action Bar
        </div>

        {/* Row 6: Hand placeholder (col-span-2) */}
        <div className="col-span-2 max-h-[160px] flex items-center justify-center text-sm text-muted-foreground bg-card/50 border-t border-border">
          Hand
        </div>

        {/* Row 7: Player info bar (col-span-2) */}
        <div className="col-span-2">
          <PlayerInfoBar player={humanPlayer} />
        </div>
      </div>
    </div>
  )
}
