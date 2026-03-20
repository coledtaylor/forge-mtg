import { useState, useCallback } from 'react'
import { useGameStore } from '../../stores/gameStore'
import { GameCardImage } from './GameCardImage'

interface StackPanelProps {
  className?: string
}

/** Floating hover preview for stack items -- shows enlarged card image near cursor */
function StackHoverPreview({
  cardName,
  mousePos,
}: {
  cardName: string | null
  mousePos: { x: number; y: number }
}) {
  if (!cardName) return null

  const left = mousePos.x - 280
  const top = Math.max(10, Math.min(mousePos.y - 100, window.innerHeight - 420))

  return (
    <div
      className="fixed z-50 pointer-events-none"
      style={{ left, top }}
    >
      <img
        src={`https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=normal`}
        alt={cardName}
        className="w-[260px] rounded-lg shadow-2xl"
      />
    </div>
  )
}

export function StackPanel({ className }: StackPanelProps) {
  const stack = useGameStore((s) => s.stack)
  const cards = useGameStore((s) => s.cards)

  const [hoverName, setHoverName] = useState<string | null>(null)
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 })

  const onMouseEnter = useCallback((name: string, e: React.MouseEvent) => {
    setHoverName(name)
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onMouseMove = useCallback((e: React.MouseEvent) => {
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onMouseLeave = useCallback(() => {
    setHoverName(null)
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
                onMouseEnter={(e) => onMouseEnter(cardName, e)}
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

      <StackHoverPreview cardName={hoverName} mousePos={mousePos} />
    </div>
  )
}
