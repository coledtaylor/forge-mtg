interface GameBoardProps {
  gameId: string
  onExit: () => void
}

export function GameBoard({ gameId, onExit }: GameBoardProps) {
  return (
    <div className="h-screen w-screen flex flex-col items-center justify-center bg-background text-foreground gap-4">
      <p className="text-lg text-muted-foreground">
        Connecting to game {gameId}...
      </p>
      <button
        onClick={onExit}
        className="text-sm text-muted-foreground hover:text-foreground underline transition-colors"
      >
        Back to deck list
      </button>
    </div>
  )
}
