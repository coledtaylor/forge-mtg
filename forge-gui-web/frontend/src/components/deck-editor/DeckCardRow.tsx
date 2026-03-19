import { Button } from '../ui/button'
import { ManaCost } from '../ManaCost'
import { Minus, Plus, Trash2 } from 'lucide-react'
import type { DeckCardEntry } from '../../types/deck'

interface DeckCardRowProps {
  card: DeckCardEntry
  onIncrement: () => void
  onDecrement: () => void
  onMouseEnter: (e: React.MouseEvent) => void
  onMouseMove: (e: React.MouseEvent) => void
  onMouseLeave: () => void
  isIllegal?: boolean
  illegalReason?: string
  isLegendary?: boolean
  onSetCommander?: () => void
}

export function DeckCardRow({
  card, onIncrement, onDecrement,
  onMouseEnter, onMouseMove, onMouseLeave,
  isIllegal, illegalReason, isLegendary, onSetCommander,
}: DeckCardRowProps) {
  return (
    <div
      className="flex items-center gap-2 px-2 py-1 rounded hover:bg-card transition-colors group"
      onMouseEnter={onMouseEnter}
      onMouseMove={onMouseMove}
      onMouseLeave={onMouseLeave}
    >
      <Button
        variant="ghost"
        size="icon"
        className="h-8 w-8 shrink-0"
        onClick={onIncrement}
      >
        <Plus className="h-3.5 w-3.5" />
      </Button>

      <span className="text-[14px] font-semibold w-6 text-center shrink-0">
        {card.quantity}
      </span>

      <Button
        variant="ghost"
        size="icon"
        className={`h-8 w-8 shrink-0 ${card.quantity === 1 ? 'text-destructive hover:bg-destructive/10' : 'hover:bg-destructive/10'}`}
        onClick={onDecrement}
      >
        {card.quantity === 1 ? <Trash2 className="h-3.5 w-3.5" /> : <Minus className="h-3.5 w-3.5" />}
      </Button>

      <span className="text-[14px] text-foreground truncate flex-1 min-w-0">
        {card.name}
      </span>

      {isIllegal && (
        <span
          className="text-[12px] bg-destructive text-white px-1.5 py-0.5 rounded shrink-0"
          title={illegalReason || 'Not legal in this format'}
        >
          !
        </span>
      )}

      {isLegendary && onSetCommander && (
        <button
          className="text-[12px] text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
          onClick={(e) => { e.stopPropagation(); onSetCommander() }}
          title="Click to set as Commander"
        >
          &#9813;
        </button>
      )}

      <ManaCost cost={card.manaCost} />
    </div>
  )
}
