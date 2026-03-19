import { useGameStore } from '../../stores/gameStore'
import { PHASE_STRIP_ITEMS } from '../../lib/gameTypes'

export function PhaseStrip() {
  const phase = useGameStore((s) => s.phase)
  const turn = useGameStore((s) => s.turn)
  const activePlayerId = useGameStore((s) => s.activePlayerId)
  const humanPlayerId = useGameStore((s) => s.humanPlayerId)

  const isYourTurn = activePlayerId === humanPlayerId

  return (
    <div className="h-8 bg-card flex items-center gap-2 px-4">
      {/* Turn indicator */}
      <span className="text-xs shrink-0">
        <span className="text-muted-foreground">Turn {turn}</span>
        {' '}
        <span className={isYourTurn ? 'text-primary font-semibold' : 'text-muted-foreground'}>
          {isYourTurn ? 'Your Turn' : "Opponent's Turn"}
        </span>
      </span>

      {/* Spacer */}
      <span className="flex-1" />

      {/* Phase pills */}
      <div className="flex items-center gap-2">
        {PHASE_STRIP_ITEMS.map((item) => {
          const isCurrent = phase !== null && (item.phases as readonly string[]).includes(phase)
          return (
            <span
              key={item.label}
              className={`px-2 py-0.5 rounded-full text-xs transition-colors duration-150 ${
                isCurrent
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground'
              }`}
            >
              {item.label}
            </span>
          )
        })}
      </div>

      {/* Spacer */}
      <span className="flex-1" />
    </div>
  )
}
