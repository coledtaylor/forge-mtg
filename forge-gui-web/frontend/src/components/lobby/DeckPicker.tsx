import type { DeckSummary } from '../../types/deck'
import { Badge } from '../ui/badge'
import { Skeleton } from '../ui/skeleton'

interface DeckPickerProps {
  decks: DeckSummary[]
  selectedDeck: string | null
  onSelect: (deckName: string) => void
  isLoading: boolean
  formatLabel: string
  onCreateDeck?: () => void
}

const COLOR_MAP: Record<string, string> = {
  W: 'bg-amber-100',
  U: 'bg-blue-400',
  B: 'bg-zinc-600',
  R: 'bg-red-500',
  G: 'bg-green-500',
}

export function DeckPicker({
  decks,
  selectedDeck,
  onSelect,
  isLoading,
  formatLabel,
  onCreateDeck,
}: DeckPickerProps) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        <span className="text-[12px] font-normal text-muted-foreground">Your Decks</span>
        <Skeleton className="h-[44px] w-full" />
        <Skeleton className="h-[44px] w-full" />
        <Skeleton className="h-[44px] w-full" />
      </div>
    )
  }

  if (decks.length === 0) {
    return (
      <div className="space-y-2">
        <span className="text-[12px] font-normal text-muted-foreground">Your Decks</span>
        <div className="rounded-lg border border-border p-4 text-center text-sm text-muted-foreground">
          <p>No decks for {formatLabel}.</p>
          {onCreateDeck && (
            <p
              className="mt-1 text-primary cursor-pointer"
              onClick={onCreateDeck}
            >
              Create one in the Deck Builder.
            </p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <span className="text-[12px] font-normal text-muted-foreground">Your Decks</span>
      <div className="max-h-[264px] overflow-y-auto rounded-lg border border-border">
        {decks.map((deck) => {
          const isSelected = deck.name === selectedDeck
          return (
            <div
              key={deck.name}
              onClick={() => onSelect(deck.name)}
              className={`flex items-center gap-2 px-3 h-[44px] cursor-pointer transition-colors ${
                isSelected
                  ? 'border-l-[3px] border-primary bg-primary/10'
                  : 'border-l-[3px] border-transparent hover:bg-muted border-b border-b-border last:border-b-0'
              }`}
            >
              <span className="flex-1 text-[14px] font-normal truncate">
                {deck.name}
              </span>
              <div className="flex items-center gap-1">
                {deck.colors.map((color) => (
                  <span
                    key={color}
                    className={`inline-block size-2.5 rounded-full ${COLOR_MAP[color] ?? 'bg-muted-foreground'}`}
                  />
                ))}
              </div>
              <Badge variant="secondary" className="text-[12px] font-normal">
                {deck.cardCount}
              </Badge>
            </div>
          )
        })}
      </div>
    </div>
  )
}
