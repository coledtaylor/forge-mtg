import { CardImage } from './CardImage'
import { Skeleton } from './ui/skeleton'
import type { CardSearchResult } from '../types/card'

interface CardGridProps {
  cards: CardSearchResult[]
  isLoading: boolean
}

export function CardGrid({ cards, isLoading }: CardGridProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(244px, 1fr))' }}>
        {Array.from({ length: 20 }).map((_, i) => (
          <Skeleton key={i} className="w-[244px] aspect-[488/680] rounded-lg" />
        ))}
      </div>
    )
  }

  if (cards.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <h2 className="text-[20px] font-semibold text-foreground">No cards found</h2>
        <p className="text-[14px] text-muted-foreground mt-2">Try adjusting your search terms or filters.</p>
      </div>
    )
  }

  return (
    <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(244px, 1fr))' }}>
      {cards.map((card) => (
        <CardImage key={card.name + card.setCode} card={card} />
      ))}
    </div>
  )
}
