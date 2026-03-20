import { useState, useCallback } from 'react'
import { useShallow } from 'zustand/react/shallow'
import { useGameStore } from '../../stores/gameStore'
import type { CardDto } from '../../lib/gameTypes'
import { HandCard } from './HandCard'
import { GameHoverPreview } from './GameHoverPreview'

interface HandZoneProps {
  className?: string
  onCardClick?: (cardId: number) => void
  isPlayable?: boolean
}

export function HandZone({ className, onCardClick, isPlayable }: HandZoneProps) {
  const [hoveredCardName, setHoveredCardName] = useState<string | null>(null)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })

  const humanPlayerId = useGameStore((s) => s.humanPlayerId)

  const handCardIds = useGameStore(
    useShallow((s) => {
      if (humanPlayerId === null) return [] as number[]
      return s.players[humanPlayerId]?.zones.Hand ?? ([] as number[])
    })
  )

  const cards = useGameStore(
    useShallow((s) => {
      return handCardIds.map((id) => s.cards[id]).filter(Boolean) as CardDto[]
    })
  )

  const cardCount = cards.length

  const getRotation = useCallback(
    (index: number) => {
      return (index - (cardCount - 1) / 2) * (10 / Math.max(cardCount, 7))
    },
    [cardCount]
  )

  const getTranslateY = useCallback(
    (index: number) => {
      return Math.abs(index - (cardCount - 1) / 2) * 3
    },
    [cardCount]
  )

  const handleHoverEnter = useCallback((cardName: string, e: React.MouseEvent) => {
    setHoveredCardName(cardName)
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const handleHoverMove = useCallback((e: React.MouseEvent) => {
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const handleHoverLeave = useCallback(() => {
    setHoveredCardName(null)
  }, [])

  return (
    <div className={`flex items-end justify-center max-h-[160px] ${className ?? ''}`}>
      {cards.map((card, i) => (
        <HandCard
          key={card.id}
          card={card}
          rotation={getRotation(i)}
          translateY={getTranslateY(i)}
          index={i}
          isPlayable={isPlayable}
          onHoverEnter={handleHoverEnter}
          onHoverMove={handleHoverMove}
          onHoverLeave={handleHoverLeave}
          onClick={onCardClick}
        />
      ))}
      <GameHoverPreview cardName={hoveredCardName} mousePos={mousePos} />
    </div>
  )
}
