import { getScryfallImageUrl } from '../../lib/scryfall'

interface CardHoverPreviewProps {
  card: { setCode: string; collectorNumber: string; name: string } | null
  mousePos: { x: number; y: number }
}

export function CardHoverPreview({ card, mousePos }: CardHoverPreviewProps) {
  if (!card || !card.setCode || !card.collectorNumber) return null

  const left = mousePos.x + 20
  const flipX = left + 300 > window.innerWidth

  return (
    <div
      className="fixed z-50 pointer-events-none"
      style={{
        left: flipX ? mousePos.x - 280 : left,
        top: Math.max(10, Math.min(mousePos.y - 100, window.innerHeight - 420)),
      }}
    >
      <img
        src={getScryfallImageUrl(card.setCode, card.collectorNumber, 'large')}
        alt={card.name}
        className="w-[260px] rounded-lg shadow-2xl"
      />
    </div>
  )
}
