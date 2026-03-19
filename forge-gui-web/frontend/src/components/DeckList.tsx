import { useState } from 'react'
import { useDecks, useCreateDeck, useDeleteDeck } from '../hooks/useDecks'
import { Button } from './ui/button'
import { Skeleton } from './ui/skeleton'
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

export function DeckList() {
  const { data: decks, isLoading, isError } = useDecks()
  const createDeck = useCreateDeck()
  const deleteDeckMutation = useDeleteDeck()
  const [deckToDelete, setDeckToDelete] = useState<string | null>(null)

  const handleCreate = () => {
    const name = `New Deck ${Date.now()}`
    createDeck.mutate({ name, format: '' })
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
        <Button onClick={handleCreate} disabled={createDeck.isPending}>
          Create Deck
        </Button>
      </div>

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
              className="flex items-center justify-between px-3 py-2 rounded-lg hover:bg-accent/10 transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="text-[14px] text-foreground">{deck.name}</span>
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
                    onClick={() => setDeckToDelete(deck.name)}
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
