import { useState, useEffect, useRef, useCallback } from 'react'
import { useShallow } from 'zustand/react/shallow'
import type { CardDto } from '../../lib/gameTypes'
import { useGameStore } from '../../stores/gameStore'
import { GameCardImage } from './GameCardImage'

type HighlightMode = 'valid-target' | 'invalid' | 'attacker' | 'blocker' | 'playable' | null

interface GameCardProps {
  card: CardDto
  width?: number
  className?: string
  onClick?: (cardId: number) => void
  onDoubleClick?: (cardId: number) => void
  onHoverEnter: (cardName: string, e: React.MouseEvent) => void
  onHoverMove: (e: React.MouseEvent) => void
  onHoverLeave: () => void
  highlightMode?: HighlightMode
}

function formatCounterName(key: string): string {
  if (key === 'P1P1') return '+1/+1'
  if (key === 'M1M1') return '-1/-1'
  if (key === 'LOYALTY') return 'Loyalty'
  return key.charAt(0).toUpperCase() + key.slice(1).toLowerCase()
}

const highlightClasses: Record<string, string> = {
  'valid-target': 'ring-2 ring-primary cursor-pointer',
  invalid: 'opacity-40 cursor-not-allowed',
  attacker: 'shadow-lg shadow-red-500/50 ring-2 ring-red-500 translate-y-[-12px]',
  blocker: 'ring-2 ring-yellow-400 translate-y-[-8px]',
  playable: 'ring-1 ring-primary/40',
}

export function GameCard({
  card,
  width = 100,
  className,
  onClick,
  onDoubleClick,
  onHoverEnter,
  onHoverMove,
  onHoverLeave,
  highlightMode,
}: GameCardProps) {
  const height = width * 1.4
  const [isDying, setIsDying] = useState(false)
  const prevZoneRef = useRef(card.zoneType)

  // Dying animation: detect when card leaves battlefield
  useEffect(() => {
    if (prevZoneRef.current === 'Battlefield' && card.zoneType !== 'Battlefield') {
      setIsDying(true)
      const timer = setTimeout(() => setIsDying(false), 500)
      return () => clearTimeout(timer)
    }
    prevZoneRef.current = card.zoneType
  }, [card.zoneType])

  // Look up attachment CardDtos from store (useShallow prevents infinite re-render)
  const attachmentCards = useGameStore(
    useShallow((s) => {
      if (!card.attachmentIds || card.attachmentIds.length === 0) return [] as CardDto[]
      return card.attachmentIds
        .map((id) => s.cards[id])
        .filter(Boolean) as CardDto[]
    })
  )

  const handleMouseEnter = useCallback(
    (e: React.MouseEvent) => onHoverEnter(card.name, e),
    [card.name, onHoverEnter]
  )

  const handleClick = useCallback(() => {
    if (highlightMode !== 'invalid') onClick?.(card.id)
  }, [card.id, onClick, highlightMode])

  const handleDoubleClick = useCallback(() => {
    if (highlightMode !== 'invalid') onDoubleClick?.(card.id)
  }, [card.id, onDoubleClick, highlightMode])

  const isTapped = card.tapped
  const isCreature = card.type?.toLowerCase().includes('creature')
  const hasCounters = card.counters && Object.keys(card.counters).length > 0
  const hasAttachments = attachmentCards.length > 0
  const attachmentCount = attachmentCards.length

  // Container dimensions accounting for attachments
  const containerWidth = hasAttachments ? width + attachmentCount * 8 : (isTapped ? height : width)
  const containerHeight = hasAttachments ? height + attachmentCount * 8 : (isTapped ? width : height)

  const highlight = highlightMode ? highlightClasses[highlightMode] ?? '' : ''

  const cardContent = (
    <div
      data-card-id={card.id}
      className={`relative rounded-md overflow-hidden ${highlight} ${className ?? ''}`}
      style={{
        width,
        height,
        transform: isTapped ? 'rotate(90deg)' : undefined,
        transformOrigin: 'center center',
        transition: 'transform 200ms ease-in-out',
      }}
      onMouseEnter={handleMouseEnter}
      onMouseMove={onHoverMove}
      onMouseLeave={onHoverLeave}
      onClick={handleClick}
      onDoubleClick={handleDoubleClick}
    >
      <GameCardImage name={card.name} width={width} />

      {/* P/T overlay for creatures */}
      {isCreature && card.power != null && card.toughness != null && (
        <div className="absolute bottom-1 left-1 bg-black/70 rounded px-1 py-0.5">
          <span className="text-[12px] font-semibold text-white leading-none">
            {card.power}/{card.toughness}
          </span>
        </div>
      )}

      {/* Counter badges */}
      {hasCounters && (
        <div className="absolute bottom-1 right-1 flex flex-col items-end gap-0.5">
          {Object.entries(card.counters!).map(([key, count]) => (
            <div
              key={key}
              className="bg-black/80 rounded-full px-1.5 py-0.5"
            >
              <span className="text-[12px] font-semibold text-white leading-none whitespace-nowrap">
                {formatCounterName(key)} x{count}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Dying animation overlay */}
      {isDying && (
        <div className="absolute inset-0 opacity-0 transition-opacity duration-500" />
      )}
    </div>
  )

  // If card has attachments, render stacked layout
  if (hasAttachments) {
    return (
      <div
        className="relative"
        style={{
          width: containerWidth,
          height: containerHeight,
        }}
      >
        {/* Attachments stacked behind, offset up and right */}
        {attachmentCards.map((attachment, i) => {
          // Guard against infinite recursion
          if (attachment.attachmentIds?.includes(card.id)) return null
          return (
            <div
              key={attachment.id}
              className="absolute"
              style={{
                top: 0,
                left: (attachmentCount - i) * 8,
                zIndex: i,
              }}
            >
              <div
                className="relative rounded-md overflow-hidden"
                style={{ width, height }}
                onMouseEnter={(e) => onHoverEnter(attachment.name, e)}
                onMouseMove={onHoverMove}
                onMouseLeave={onHoverLeave}
              >
                <GameCardImage name={attachment.name} width={width} />
              </div>
            </div>
          )
        })}
        {/* Parent card in front */}
        <div
          className="absolute"
          style={{
            top: attachmentCount * 8,
            left: 0,
            zIndex: attachmentCount + 1,
          }}
        >
          {cardContent}
        </div>
      </div>
    )
  }

  // Tapped card needs a wrapper to prevent layout issues
  if (isTapped) {
    return (
      <div
        style={{
          width: containerWidth,
          height: containerHeight,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {cardContent}
      </div>
    )
  }

  return cardContent
}
