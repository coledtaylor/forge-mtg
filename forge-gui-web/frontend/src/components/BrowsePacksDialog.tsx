import { useState } from 'react'
import { useJumpstartPacks } from '../hooks/useJumpstartPacks'
import { useCreateDeck } from '../hooks/useDecks'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from './ui/dialog'
import { Badge } from './ui/badge'
import { Button } from './ui/button'
import { Input } from './ui/input'
import { Skeleton } from './ui/skeleton'
import type { JumpstartPack } from '../types/jumpstart'

const MTG_COLORS: Record<string, { hex: string; border?: string }> = {
  W: { hex: '#F9FAF4', border: '#d4d4d4' },
  U: { hex: '#0E68AB' },
  B: { hex: '#150B00', border: '#555555' },
  R: { hex: '#D3202A' },
  G: { hex: '#00733E' },
  C: { hex: '#A0A0A0' },
}

function ColorDot({ color }: { color: string }) {
  const colorInfo = MTG_COLORS[color]
  if (!colorInfo) return null
  return (
    <span
      className="inline-block w-3 h-3 rounded-full"
      style={{
        backgroundColor: colorInfo.hex,
        border: colorInfo.border ? `1px solid ${colorInfo.border}` : undefined,
      }}
      title={color}
    />
  )
}

interface BrowsePacksDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onEditDeck: (name: string, format: string) => void
}

export function BrowsePacksDialog({ open, onOpenChange, onEditDeck }: BrowsePacksDialogProps) {
  const { data: packs, isLoading } = useJumpstartPacks()
  const createDeck = useCreateDeck()
  const [filter, setFilter] = useState('')
  const [copyingId, setCopyingId] = useState<string | null>(null)

  const filteredPacks = packs?.filter((pack) =>
    pack.theme.toLowerCase().includes(filter.toLowerCase())
  ) ?? []

  const handleCopy = (pack: JumpstartPack) => {
    const deckName = `${pack.theme} (${pack.setCode})`
    setCopyingId(pack.id)
    createDeck.mutate(
      { name: deckName, format: 'Jumpstart' },
      {
        onSuccess: () => {
          setCopyingId(null)
          onOpenChange(false)
          onEditDeck(deckName, 'Jumpstart')
        },
        onError: () => {
          setCopyingId(null)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[600px]">
        <DialogHeader>
          <DialogTitle>Jumpstart Packs</DialogTitle>
          <DialogDescription>
            Browse Forge's built-in Jumpstart packs. Copy one to your collection to edit it.
          </DialogDescription>
        </DialogHeader>

        <Input
          placeholder="Filter by theme..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="mb-3"
        />

        <div className="max-h-[60vh] overflow-y-auto space-y-1">
          {isLoading ? (
            Array.from({ length: 8 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full rounded-md" />
            ))
          ) : filteredPacks.length === 0 ? (
            <p className="text-[14px] text-muted-foreground py-4 text-center">
              {packs && packs.length > 0 ? 'No packs match your filter.' : 'No packs available.'}
            </p>
          ) : (
            filteredPacks.map((pack) => (
              <div
                key={pack.id}
                className="flex items-center justify-between px-3 py-2 rounded-md hover:bg-accent/10 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <span className="text-[14px] text-foreground font-medium">{pack.theme}</span>
                  <Badge variant="secondary">{pack.setCode}</Badge>
                  <span className="text-[12px] text-muted-foreground">{pack.cardCount} cards</span>
                  <div className="flex items-center gap-1">
                    {pack.colors.map((c) => (
                      <ColorDot key={c} color={c} />
                    ))}
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleCopy(pack)}
                  disabled={copyingId === pack.id}
                >
                  {copyingId === pack.id ? 'Copying...' : 'Copy to My Packs'}
                </Button>
              </div>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
