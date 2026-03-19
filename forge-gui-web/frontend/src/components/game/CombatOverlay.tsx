import { useEffect, useState, useCallback } from 'react'
import { useGameStore } from '../../stores/gameStore'

interface ArrowData {
  fromX: number
  fromY: number
  toX: number
  toY: number
}

interface CombatOverlayProps {
  className?: string
  pendingBlockers?: Map<number, number> // blockerId -> attackerId
}

export function CombatOverlay({ className, pendingBlockers }: CombatOverlayProps) {
  const combat = useGameStore((s) => s.combat)
  const [arrows, setArrows] = useState<ArrowData[]>([])

  const calculateArrows = useCallback(() => {
    const newArrows: ArrowData[] = []

    // Arrows from server combat state
    if (combat) {
      for (const attacker of combat.attackers) {
        for (const blockerId of attacker.blockerCardIds) {
          const arrow = getArrowBetweenCards(blockerId, attacker.cardId)
          if (arrow) newArrows.push(arrow)
        }
      }
    }

    // Arrows from pending local blocker assignments
    if (pendingBlockers) {
      pendingBlockers.forEach((attackerId, blockerId) => {
        const arrow = getArrowBetweenCards(blockerId, attackerId)
        if (arrow) newArrows.push(arrow)
      })
    }

    setArrows(newArrows)
  }, [combat, pendingBlockers])

  useEffect(() => {
    calculateArrows()
  }, [calculateArrows])

  // Recalculate on window resize
  useEffect(() => {
    const onResize = () => calculateArrows()
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [calculateArrows])

  if (arrows.length === 0) return null

  return (
    <svg
      className={`absolute inset-0 w-full h-full pointer-events-none z-10 ${className ?? ''}`}
    >
      <defs>
        <marker
          id="combat-arrowhead"
          markerWidth="8"
          markerHeight="6"
          refX="8"
          refY="3"
          orient="auto"
        >
          <polygon points="0 0, 8 3, 0 6" fill="rgb(250, 204, 21)" />
        </marker>
      </defs>
      {arrows.map((arrow, i) => (
        <line
          key={i}
          x1={arrow.fromX}
          y1={arrow.fromY}
          x2={arrow.toX}
          y2={arrow.toY}
          stroke="rgb(250, 204, 21)"
          strokeWidth="2"
          markerEnd="url(#combat-arrowhead)"
        />
      ))}
    </svg>
  )
}

function getArrowBetweenCards(
  fromCardId: number,
  toCardId: number
): ArrowData | null {
  const fromEl = document.querySelector(`[data-card-id="${fromCardId}"]`)
  const toEl = document.querySelector(`[data-card-id="${toCardId}"]`)
  if (!fromEl || !toEl) return null

  const fromRect = fromEl.getBoundingClientRect()
  const toRect = toEl.getBoundingClientRect()

  // Get the parent overlay/board element for relative positioning
  const boardEl = fromEl.closest('[data-game-board]')
  const boardRect = boardEl?.getBoundingClientRect() ?? { left: 0, top: 0 }

  return {
    fromX: fromRect.left + fromRect.width / 2 - boardRect.left,
    fromY: fromRect.top + fromRect.height / 2 - boardRect.top,
    toX: toRect.left + toRect.width / 2 - boardRect.left,
    toY: toRect.top + toRect.height / 2 - boardRect.top,
  }
}
