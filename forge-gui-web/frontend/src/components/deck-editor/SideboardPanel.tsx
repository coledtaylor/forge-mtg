import { DeckCardRow } from './DeckCardRow'
import type { DeckCardEntry } from '../../types/deck'

interface SideboardPanelProps {
  cards: DeckCardEntry[]
  onIncrement: (cardName: string) => void
  onDecrement: (cardName: string) => void
  onCardMouseEnter: (card: DeckCardEntry, e: React.MouseEvent) => void
  onCardMouseMove: (e: React.MouseEvent) => void
  onCardMouseLeave: () => void
}

export function SideboardPanel({
  cards, onIncrement, onDecrement,
  onCardMouseEnter, onCardMouseMove, onCardMouseLeave,
}: SideboardPanelProps) {
  const total = cards.reduce((sum, c) => sum + c.quantity, 0)

  return (
    <div>
      <div className="text-[12px] uppercase text-muted-foreground tracking-wide px-2 py-2">
        Sideboard ({total}/15)
      </div>

      {cards.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12">
          <h3 className="text-[20px] font-semibold text-foreground">No sideboard cards</h3>
          <p className="text-[14px] text-muted-foreground mt-2">
            Add cards from search to build your sideboard.
          </p>
        </div>
      ) : (
        <div>
          {cards.map((card) => (
            <DeckCardRow
              key={card.name}
              card={card}
              onIncrement={() => onIncrement(card.name)}
              onDecrement={() => onDecrement(card.name)}
              onMouseEnter={(e) => onCardMouseEnter(card, e)}
              onMouseMove={onCardMouseMove}
              onMouseLeave={onCardMouseLeave}
            />
          ))}
        </div>
      )}
    </div>
  )
}
