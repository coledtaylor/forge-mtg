import { useState } from 'react'
import { useDecks, useCreateDeck, useDeleteDeck } from '../hooks/useDecks'
import { BrowsePacksDialog } from './BrowsePacksDialog'
import { Button } from './ui/button'
import { Skeleton } from './ui/skeleton'
import { Input } from './ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from './ui/dialog'

const MTG_COLORS: Record<string, { hex: string; border?: string }> = {
  W: { hex: '#F9FAF4', border: '#d4d4d4' },
  U: { hex: '#0E68AB' },
  B: { hex: '#150B00', border: '#555555' },
  R: { hex: '#D3202A' },
  G: { hex: '#00733E' },
  C: { hex: '#A0A0A0' },
}

const FORMAT_OPTIONS = [
  { value: '', label: 'Casual' },
  { value: 'Standard', label: 'Standard' },
  { value: 'Modern', label: 'Modern' },
  { value: 'Legacy', label: 'Legacy' },
  { value: 'Vintage', label: 'Vintage' },
  { value: 'Commander', label: 'Commander' },
  { value: 'Pioneer', label: 'Pioneer' },
  { value: 'Pauper', label: 'Pauper' },
  { value: 'Jumpstart', label: 'Jumpstart' },
]

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

interface DeckListProps {
  onEditDeck: (name: string, format: string) => void
}

export function DeckList({ onEditDeck }: DeckListProps) {
  const { data: decks, isLoading, isError } = useDecks()
  const createDeck = useCreateDeck()
  const deleteDeckMutation = useDeleteDeck()
  const [deckToDelete, setDeckToDelete] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [browsePacksOpen, setBrowsePacksOpen] = useState(false)
  const [deckName, setDeckName] = useState('')
  const [selectedFormat, setSelectedFormat] = useState('')

  const handleCreate = () => {
    if (!deckName.trim()) return
    createDeck.mutate(
      { name: deckName.trim(), format: selectedFormat },
      {
        onSuccess: () => {
          setCreateOpen(false)
          setDeckName('')
          setSelectedFormat('')
        },
      }
    )
  }

  const handleDelete = () => {
    if (deckToDelete) {
      deleteDeckMutation.mutate(deckToDelete)
      setDeckToDelete(null)
    }
  }

  if (isError) {
    return (
      <div className="py-8">
        <p className="text-[14px] text-destructive">
          Could not load decks. Check that the server is running and try again.
        </p>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-[20px] font-semibold text-foreground">Your Decks</h2>
        <div className="flex items-center gap-2">
        <Button variant="outline" onClick={() => setBrowsePacksOpen(true)}>
          Browse Packs
        </Button>
        <Dialog open={createOpen} onOpenChange={(open) => {
          setCreateOpen(open)
          if (!open) { setDeckName(''); setSelectedFormat('') }
        }}>
          <DialogTrigger render={<Button />}>
            Create Deck
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New Deck</DialogTitle>
              <DialogDescription>
                Give your deck a name and choose a format.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-2">
              <div className="space-y-2">
                <label htmlFor="deck-name" className="text-[14px] font-medium text-foreground">
                  Deck Name
                </label>
                <Input
                  id="deck-name"
                  placeholder="My Deck"
                  value={deckName}
                  onChange={(e) => setDeckName(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleCreate() }}
                />
              </div>
              <div className="space-y-2">
                <label className="text-[14px] font-medium text-foreground">
                  Format
                </label>
                <Select value={selectedFormat} onValueChange={setSelectedFormat}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select a format" />
                  </SelectTrigger>
                  <SelectContent>
                    {FORMAT_OPTIONS.map((opt) => (
                      <SelectItem key={opt.label} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <DialogClose render={<Button variant="outline" />}>
                Cancel
              </DialogClose>
              <Button
                onClick={handleCreate}
                disabled={createDeck.isPending || !deckName.trim()}
              >
                Create Deck
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        </div>
      </div>

      <BrowsePacksDialog open={browsePacksOpen} onOpenChange={setBrowsePacksOpen} onEditDeck={onEditDeck} />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-lg" />
          ))}
        </div>
      ) : !decks || decks.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-8">
          <h3 className="text-[20px] font-semibold text-foreground">No saved decks</h3>
          <p className="text-[14px] text-muted-foreground mt-2">Create a new deck to get started.</p>
        </div>
      ) : (
        <div className="space-y-1">
          {decks.map((deck) => (
            <div
              key={deck.path}
              className="flex items-center justify-between px-3 py-2 rounded-lg hover:bg-accent/10 transition-colors cursor-pointer"
              onClick={() => onEditDeck(deck.name, deck.format || '')}
            >
              <div className="flex items-center gap-3">
                <span className="text-[14px] text-foreground">{deck.name}</span>
                <span className="text-[12px] text-muted-foreground">{deck.format || 'Casual'}</span>
                <span className="text-[12px] text-muted-foreground">{deck.cardCount} cards</span>
                <div className="flex items-center gap-1">
                  {deck.colors.map((c) => (
                    <ColorDot key={c} color={c} />
                  ))}
                </div>
              </div>

              <Dialog open={deckToDelete === deck.name} onOpenChange={(open) => {
                if (!open) setDeckToDelete(null)
              }}>
                <DialogTrigger render={
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={(e) => { e.stopPropagation(); setDeckToDelete(deck.name) }}
                  />
                }>
                  Delete
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Delete Deck</DialogTitle>
                    <DialogDescription>
                      Delete '{deck.name}'? This cannot be undone.
                    </DialogDescription>
                  </DialogHeader>
                  <DialogFooter>
                    <DialogClose render={<Button variant="outline" />}>
                      Keep Deck
                    </DialogClose>
                    <Button variant="destructive" onClick={handleDelete}>
                      Delete
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
