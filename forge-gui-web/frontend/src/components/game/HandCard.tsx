import { useState, useCallback } from 'react'
import type { CardDto } from '../../lib/gameTypes'
import { GameCardImage } from './GameCardImage'

interface HandCardProps {
  card: CardDto
  rotation: number
  translateY: number
  index: number
  isPlayable?: boolean
  onHoverEnter: (cardName: string, e: React.MouseEvent) => void
  onHoverMove: (e: React.MouseEvent) => void
  onHoverLeave: () => void
  onDoubleClick?: (cardId: number) => void
}

export function HandCard({
  card,
  rotation,
  translateY,
  index,
  isPlayable,
  onHoverEnter,
  onHoverMove,
  onHoverLeave,
  onDoubleClick,
}: HandCardProps) {
  const [isHovered, setIsHovered] = useState(false)
  const [isClicked, setIsClicked] = useState(false)

  const handleMouseEnter = useCallback(
    (e: React.MouseEvent) => {
      setIsHovered(true)
      onHoverEnter(card.name, e)
    },
    [card.name, onHoverEnter]
  )

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      onHoverMove(e)
    },
    [onHoverMove]
  )

  const handleMouseLeave = useCallback(() => {
    setIsHovered(false)
    onHoverLeave()
  }, [onHoverLeave])

  const handleDoubleClick = useCallback(() => {
    if (onDoubleClick) {
      setIsClicked(true)
      onDoubleClick(card.id)
      setTimeout(() => setIsClicked(false), 200)
    }
  }, [card.id, onDoubleClick])

  const transform = isHovered
    ? 'translateY(-40px) scale(1.1)'
    : `rotate(${rotation}deg) translateY(${translateY}px)`

  return (
    <div
      className={`relative cursor-pointer ${index > 0 ? 'ml-[-40px]' : ''}`}
      style={{
        zIndex: isHovered ? 10 : index,
        transform,
        transition: 'transform 150ms ease-out',
      }}
      onMouseEnter={handleMouseEnter}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onDoubleClick={handleDoubleClick}
    >
      <div
        className={`w-[100px] aspect-[5/7] ${
          isPlayable ? 'ring-1 ring-primary/40 rounded-md' : ''
        } ${isClicked ? 'opacity-50 transition-opacity duration-200' : ''}`}
      >
        <GameCardImage name={card.name} width={100} />
      </div>
    </div>
  )
}
