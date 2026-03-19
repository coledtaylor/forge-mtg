interface GameHoverPreviewProps {
  cardName: string | null
  mousePos: { x: number; y: number }
}

export function GameHoverPreview({ cardName, mousePos }: GameHoverPreviewProps) {
  if (!cardName) return null

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
        src={`https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=large`}
        alt={cardName}
        className="w-[260px] rounded-lg shadow-2xl"
      />
    </div>
  )
}
