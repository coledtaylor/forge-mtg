import { X } from 'lucide-react'
import { Button } from '../ui/button'
import { getScryfallImageUrl } from '../../lib/scryfall'
import type { DeckCardEntry } from '../../types/deck'

interface CommanderSlotProps {
  commander: DeckCardEntry | null
  onRemove: () => void
}

export function CommanderSlot({ commander, onRemove }: CommanderSlotProps) {
  if (!commander) {
    return (
      <div className="flex items-center justify-center px-4 py-3 border border-dashed border-muted-foreground/30 rounded-lg">
        <span className="text-[12px] uppercase text-muted-foreground tracking-wide">Set Commander</span>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-3 px-4 py-3 border border-primary rounded-lg relative">
      {commander.setCode && commander.collectorNumber ? (
        <img
          src={getScryfallImageUrl(commander.setCode, commander.collectorNumber, 'small')}
          alt={commander.name}
          className="w-[120px] rounded"
        />
      ) : (
        <div className="w-[120px] aspect-[488/680] bg-card rounded flex items-center justify-center">
          <span className="text-[12px] text-muted-foreground">{commander.name}</span>
        </div>
      )}
      <div className="flex-1 min-w-0">
        <div className="text-[12px] uppercase text-muted-foreground tracking-wide">Commander</div>
        <div className="text-[14px] font-semibold text-foreground truncate">{commander.name}</div>
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="h-6 w-6 absolute top-2 right-2"
        onClick={onRemove}
      >
        <X className="h-3.5 w-3.5" />
      </Button>
    </div>
  )
}
