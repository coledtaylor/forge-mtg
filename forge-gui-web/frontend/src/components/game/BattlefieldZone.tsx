import { useState, useCallback, useMemo, useRef } from 'react'
import { useShallow } from 'zustand/react/shallow'
import { useGameStore } from '../../stores/gameStore'
import type { CardDto } from '../../lib/gameTypes'
import { GameCard } from './GameCard'
import { GameHoverPreview } from './GameHoverPreview'

interface BattlefieldZoneProps {
  playerId: number
  flipped?: boolean
  className?: string
  onCardClick?: (cardId: number) => void
  onCardDoubleClick?: (cardId: number) => void
}

function LaneRow({
  cards,
  availableWidth,
  onHoverEnter,
  onHoverMove,
  onHoverLeave,
  onCardClick,
  onCardDoubleClick,
}: {
  cards: CardDto[]
  availableWidth: number
  onHoverEnter: (card: CardDto, e: React.MouseEvent) => void
  onHoverMove: (e: React.MouseEvent) => void
  onHoverLeave: () => void
  onCardClick?: (cardId: number) => void
  onCardDoubleClick?: (cardId: number) => void
}) {
  const cardCount = cards.length
  const gap = 8

  // Calculate card width for large battlefields
  const cardWidth =
    cardCount > 0
      ? Math.max(60, Math.min(100, Math.floor((availableWidth - (cardCount - 1) * gap) / cardCount)))
      : 100

  const needsScroll = cardCount > 0 && cardWidth <= 60

  return (
    <div
      className={`flex flex-row flex-wrap gap-[8px] items-end min-h-[70px] ${
        needsScroll ? 'overflow-x-auto flex-nowrap' : ''
      }`}
    >
      {cards.map((card) => (
        <GameCard
          key={card.id}
          card={card}
          width={cardWidth}
          onClick={onCardClick}
          onDoubleClick={onCardDoubleClick}
          onHoverEnter={onHoverEnter}
          onHoverMove={onHoverMove}
          onHoverLeave={onHoverLeave}
        />
      ))}
    </div>
  )
}

export function BattlefieldZone({
  playerId,
  flipped,
  className,
  onCardClick,
  onCardDoubleClick,
}: BattlefieldZoneProps) {
  const [hoveredCard, setHoveredCard] = useState<CardDto | null>(null)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })
  const containerRef = useRef<HTMLDivElement>(null)

  const battlefieldCardIds = useGameStore(
    useShallow((s) => {
      return s.players[playerId]?.zones.Battlefield ?? ([] as number[])
    })
  )

  const allCards = useGameStore(
    useShallow((s) => {
      return battlefieldCardIds.map((id) => s.cards[id]).filter(Boolean) as CardDto[]
    })
  )

  const { lands, creatures } = useMemo(() => {
    const lands: CardDto[] = []
    const creatures: CardDto[] = []
    for (const card of allCards) {
      if (card.type?.toLowerCase().includes('land')) {
        lands.push(card)
      } else {
        creatures.push(card)
      }
    }
    return { lands, creatures }
  }, [allCards])

  const handleHoverEnter = useCallback((card: CardDto, e: React.MouseEvent) => {
    setHoveredCard(card)
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const handleHoverMove = useCallback((e: React.MouseEvent) => {
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const handleHoverLeave = useCallback(() => {
    setHoveredCard(null)
  }, [])

  // Estimate available width from container (fallback to viewport - stack panel)
  const availableWidth = containerRef.current?.clientWidth ?? (window.innerWidth - 220 - 32)

  // Per UI-SPEC: for opponent (flipped), lands first (closer to center), then creatures.
  // For player (not flipped), creatures first, then lands (lands closer to center).
  const firstLane = flipped ? lands : creatures
  const secondLane = flipped ? creatures : lands

  return (
    <div ref={containerRef} className={`flex flex-col gap-[8px] p-[8px] ${className ?? ''}`}>
      <LaneRow
        cards={firstLane}
        availableWidth={availableWidth}
        onHoverEnter={handleHoverEnter}
        onHoverMove={handleHoverMove}
        onHoverLeave={handleHoverLeave}
        onCardClick={onCardClick}
        onCardDoubleClick={onCardDoubleClick}
      />
      <LaneRow
        cards={secondLane}
        availableWidth={availableWidth}
        onHoverEnter={handleHoverEnter}
        onHoverMove={handleHoverMove}
        onHoverLeave={handleHoverLeave}
        onCardClick={onCardClick}
        onCardDoubleClick={onCardDoubleClick}
      />
      <GameHoverPreview card={hoveredCard} mousePos={mousePos} />
    </div>
  )
}
