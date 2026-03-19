import { getScryfallImageUrl } from '../../lib/scryfall'
import type { DeckCardEntry } from '../../types/deck'

interface DeckGridViewProps {
  cards: DeckCardEntry[]
  onCardMouseEnter: (card: DeckCardEntry, e: React.MouseEvent) => void
  onCardMouseMove: (e: React.MouseEvent) => void
  onCardMouseLeave: () => void
}

export function DeckGridView({ cards, onCardMouseEnter, onCardMouseMove, onCardMouseLeave }: DeckGridViewProps) {
  if (cards.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <h3 className="text-[20px] font-semibold text-foreground">Empty deck</h3>
        <p className="text-[14px] text-muted-foreground mt-2">
          Search for cards on the left and click to add them.
        </p>
      </div>
    )
  }

  return (
    <div className="grid gap-2" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))' }}>
      {cards.map((card) => (
        <div
          key={card.name}
          className="relative"
          onMouseEnter={(e) => onCardMouseEnter(card, e)}
          onMouseMove={onCardMouseMove}
          onMouseLeave={onCardMouseLeave}
        >
          {card.setCode && card.collectorNumber ? (
            <img
              src={getScryfallImageUrl(card.setCode, card.collectorNumber, 'small')}
              alt={card.name}
              loading="lazy"
              className="w-full rounded"
            />
          ) : (
            <div className="w-full aspect-[488/680] bg-card rounded flex items-center justify-center p-2">
              <span className="text-[12px] text-muted-foreground text-center">{card.name}</span>
            </div>
          )}
          {card.quantity > 1 && (
            <span className="absolute top-1 right-1 bg-background/80 text-[12px] font-semibold px-1.5 rounded">
              x{card.quantity}
            </span>
          )}
        </div>
      ))}
    </div>
  )
}
