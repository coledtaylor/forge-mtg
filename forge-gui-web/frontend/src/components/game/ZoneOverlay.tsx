import { useGameStore } from '../../stores/gameStore'
import { GameCardImage } from './GameCardImage'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog'

interface ZoneOverlayProps {
  playerId: number
  zone: 'Graveyard' | 'Exile'
  open: boolean
  onClose: () => void
}

export function ZoneOverlay({ playerId, zone, open, onClose }: ZoneOverlayProps) {
  const player = useGameStore((s) => s.players[playerId])
  const cards = useGameStore((s) => s.cards)

  const cardIds = player?.zones?.[zone] ?? []
  const zoneCards = cardIds.map((id) => cards[id]).filter(Boolean)

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) onClose() }}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{zone} ({zoneCards.length})</DialogTitle>
        </DialogHeader>
        <div className="max-h-[60vh] overflow-y-auto">
          {zoneCards.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">
              No cards in {zone.toLowerCase()}.
            </p>
          ) : (
            <div className="grid grid-cols-[repeat(auto-fill,minmax(100px,1fr))] gap-2">
              {zoneCards.map((card) => (
                <GameCardImage
                  key={card.id}
                  name={card.name}
                  width={100}
                />
              ))}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
