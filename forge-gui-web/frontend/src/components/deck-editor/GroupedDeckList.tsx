import { groupByType } from '../../lib/deck-grouping'
import { DeckCardRow } from './DeckCardRow'
import type { DeckCardEntry } from '../../types/deck'

interface GroupedDeckListProps {
  cards: DeckCardEntry[]
  onIncrement: (cardName: string) => void
  onDecrement: (cardName: string) => void
  onCardMouseEnter: (card: DeckCardEntry, e: React.MouseEvent) => void
  onCardMouseMove: (e: React.MouseEvent) => void
  onCardMouseLeave: () => void
  illegalCards?: Map<string, string>
  isCommanderFormat?: boolean
  onSetCommander?: (card: DeckCardEntry) => void
}

export function GroupedDeckList({
  cards, onIncrement, onDecrement,
  onCardMouseEnter, onCardMouseMove, onCardMouseLeave,
  illegalCards, isCommanderFormat, onSetCommander,
}: GroupedDeckListProps) {
  const groups = groupByType(cards)

  if (groups.length === 0) {
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
    <div className="space-y-3">
      {groups.map((group) => (
        <div key={group.type}>
          <div className="text-[12px] uppercase text-muted-foreground tracking-wide px-2 py-1">
            {group.type}s ({group.count})
          </div>
          <div>
            {group.cards.map((card) => (
              <DeckCardRow
                key={card.name}
                card={card}
                onIncrement={() => onIncrement(card.name)}
                onDecrement={() => onDecrement(card.name)}
                onMouseEnter={(e) => onCardMouseEnter(card, e)}
                onMouseMove={onCardMouseMove}
                onMouseLeave={onCardMouseLeave}
                isIllegal={illegalCards?.has(card.name)}
                illegalReason={illegalCards?.get(card.name)}
                isLegendary={isCommanderFormat && card.typeLine.toLowerCase().includes('legendary') && card.typeLine.toLowerCase().includes('creature')}
                onSetCommander={isCommanderFormat ? () => onSetCommander?.(card) : undefined}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
