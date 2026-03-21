import { useState, useEffect } from 'react'
import { useGameStore } from '../../stores/gameStore'
import { PHASE_STRIP_ITEMS } from '../../lib/gameTypes'

const phaseFlashStyle = `
@keyframes phase-flash {
  0% { background-color: hsl(var(--primary) / 0.8); color: hsl(var(--primary-foreground)); }
  100% { background-color: transparent; color: hsl(var(--muted-foreground)); }
}
`

export function PhaseStrip() {
  const phase = useGameStore((s) => s.phase)
  const turn = useGameStore((s) => s.turn)
  const activePlayerId = useGameStore((s) => s.activePlayerId)
  const humanPlayerId = useGameStore((s) => s.humanPlayerId)
  const lastPhaseAutoPass = useGameStore((s) => s.lastPhaseAutoPass)

  const isYourTurn = activePlayerId === humanPlayerId

  const [flashPhase, setFlashPhase] = useState<string | null>(null)

  useEffect(() => {
    if (lastPhaseAutoPass && phase) {
      setFlashPhase(phase)
      const timer = setTimeout(() => setFlashPhase(null), 300)
      return () => clearTimeout(timer)
    }
  }, [phase, lastPhaseAutoPass])

  return (
    <>
      <style>{phaseFlashStyle}</style>
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
            const isFlashing = flashPhase !== null && (item.phases as readonly string[]).includes(flashPhase)
            return (
              <span
                key={item.label}
                className={`px-2 py-0.5 rounded-full text-xs transition-colors duration-150 ${
                  isCurrent && !isFlashing
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground'
                }`}
                style={isFlashing ? { animation: 'phase-flash 300ms ease-out' } : undefined}
              >
                {item.label}
              </span>
            )
          })}
        </div>

        {/* Spacer */}
        <span className="flex-1" />
      </div>
    </>
  )
}
