import type { CardDto } from '../../lib/gameTypes'
import { GameCardImage } from './GameCardImage'

interface GameHoverPreviewProps {
  card: CardDto | null
  mousePos: { x: number; y: number }
}

export function GameHoverPreview({ card, mousePos }: GameHoverPreviewProps) {
  if (!card) return null

  const left = mousePos.x + 20
  const flipX = left + 300 > window.innerWidth

  return (
    <div
      className="fixed z-50 pointer-events-none"
      style={{
        left: flipX ? mousePos.x - 280 : left,
        top: Math.max(10, Math.min(mousePos.y - 100, window.innerHeight - 520)),
      }}
    >
      {/* Card image */}
      <GameCardImage
        name={card.name}
        setCode={card.setCode ?? undefined}
        collectorNumber={card.collectorNumber ?? undefined}
        width={260}
        className="rounded-t-lg shadow-2xl"
      />

      {/* Oracle text panel */}
      <div className="w-[260px] bg-card rounded-b-lg border border-t-0 border-border p-3 space-y-1 shadow-2xl">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold text-foreground">{card.name}</span>
          {card.manaCost && (
            <span className="text-xs text-muted-foreground">{card.manaCost}</span>
          )}
        </div>
        {card.type && (
          <div className="text-xs text-muted-foreground">{card.type}</div>
        )}
        {card.oracleText && (
          <div className="text-xs text-foreground whitespace-pre-line leading-relaxed">
            {card.oracleText}
          </div>
        )}
        {card.type?.toLowerCase().includes('creature') && (card.power !== 0 || card.toughness !== 0) && (
          <div className="text-xs text-foreground text-right font-semibold">
            {card.power}/{card.toughness}
          </div>
        )}
      </div>
    </div>
  )
}
