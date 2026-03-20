import { useState, useCallback } from 'react'
import { useGameStore } from '../../stores/gameStore'
import type { CardDto } from '../../lib/gameTypes'
import { GameCardImage } from './GameCardImage'
import { GameHoverPreview } from './GameHoverPreview'

interface StackPanelProps {
  className?: string
}

export function StackPanel({ className }: StackPanelProps) {
  const stack = useGameStore((s) => s.stack)
  const cards = useGameStore((s) => s.cards)

  const [hoveredCard, setHoveredCard] = useState<CardDto | null>(null)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })

  const onMouseEnter = useCallback((card: CardDto, e: React.MouseEvent) => {
    setHoveredCard(card)
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onMouseMove = useCallback((e: React.MouseEvent) => {
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onMouseLeave = useCallback(() => {
    setHoveredCard(null)
  }, [])

  return (
    <div className={`flex flex-col ${className ?? ''}`}>
      {/* Content */}
      {stack.length === 0 ? (
        <div className="flex-1 flex items-center justify-center text-xs text-muted-foreground">
          Stack empty
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto px-4 pb-2 flex flex-col gap-4">
          {stack.map((item) => {
            const sourceCard = cards[item.sourceCardId]
            const cardName = sourceCard?.name ?? item.name ?? 'Unknown'
            return (
              <div
                key={item.id}
                className="flex gap-2 cursor-pointer"
                onMouseEnter={(e) => sourceCard ? onMouseEnter(sourceCard, e) : undefined}
                onMouseMove={onMouseMove}
                onMouseLeave={onMouseLeave}
              >
                <div className="shrink-0">
                  <GameCardImage name={cardName} setCode={sourceCard?.setCode} collectorNumber={sourceCard?.collectorNumber} width={60} />
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-xs text-foreground truncate">
                    {cardName}
                  </span>
                  {item.description && (
                    <span className="text-xs text-muted-foreground line-clamp-2">
                      {item.description}
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      <GameHoverPreview card={hoveredCard} mousePos={mousePos} />
    </div>
  )
}
