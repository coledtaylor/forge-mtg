import { useState } from 'react'
import { useGameStore } from '../../stores/gameStore'
import { Button } from '../ui/button'

interface GameOverScreenProps {
  onReturnToLobby: () => void
}

export function GameOverScreen({ onReturnToLobby }: GameOverScreenProps) {
  const gameOver = useGameStore((s) => s.gameOver)
  const humanPlayerId = useGameStore((s) => s.humanPlayerId)
  const players = useGameStore((s) => s.players)
  const [dismissed, setDismissed] = useState(false)

  if (gameOver === null || dismissed) return null

  const humanPlayer = humanPlayerId !== null ? players[humanPlayerId] : undefined
  const humanName = humanPlayer?.name ?? ''

  const isWin = gameOver.winner === humanName
  const isDraw = gameOver.winner === '' || gameOver.winner === 'Draw'

  let headingText: string
  let headingColor: string
  if (isDraw) {
    headingText = 'Draw'
    headingColor = 'text-muted-foreground'
  } else if (isWin) {
    headingText = 'You Won!'
    headingColor = 'text-green-400'
  } else {
    headingText = 'You Lost'
    headingColor = 'text-red-400'
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70">
      <div className="bg-card w-[400px] rounded-lg shadow-2xl p-8 flex flex-col items-center gap-4">
        <h2 className={`text-[20px] font-semibold ${headingColor}`}>
          {headingText}
        </h2>
        <p className="text-sm text-muted-foreground text-center">
          {gameOver.message}
        </p>
        <Button variant="default" onClick={onReturnToLobby}>
          Return to Lobby
        </Button>
        <Button variant="outline" onClick={() => setDismissed(true)}>
          View Board
        </Button>
      </div>
    </div>
  )
}
