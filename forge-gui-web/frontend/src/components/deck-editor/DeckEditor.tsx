import { useCallback, useMemo } from 'react'
import { useDeckEditor } from '../../hooks/useDeckEditor'
import { useCardHover } from '../../hooks/useCardHover'
import { CardSearchPanel } from './CardSearchPanel'
import { DeckPanel } from './DeckPanel'
import { CardHoverPreview } from './CardHoverPreview'
import { Skeleton } from '../ui/skeleton'
import type { CardSearchResult } from '../../types/card'

interface DeckEditorProps {
  deckName: string
  onBack: () => void
}

export function DeckEditor({ deckName, onBack }: DeckEditorProps) {
  const {
    deck, isLoading, isDirty, isSaving, saveError,
    addCard, removeCard, setQuantity, setCommander, addBasicLand, flushSave,
  } = useDeckEditor(deckName)

  const { hoverCard, mousePos, onCardMouseEnter, onCardMouseMove, onCardMouseLeave } = useCardHover()

  // Flush save on navigation away
  const handleBack = useCallback(() => {
    flushSave()
    onBack()
  }, [flushSave, onBack])

  const handleCardClick = useCallback((card: CardSearchResult) => {
    addCard(card, 'main')
  }, [addCard])

  const handleIncrement = useCallback((cardName: string) => {
    // Find the card in deck to get its data for incrementing
    if (!deck) return
    const entry = deck.main.find(c => c.name === cardName)
    if (entry) {
      setQuantity(cardName, entry.quantity + 1, 'main')
    }
  }, [deck, setQuantity])

  const handleDecrement = useCallback((cardName: string) => {
    removeCard(cardName, 'main')
  }, [removeCard])

  const handleAddLand = useCallback((landName: string) => {
    addBasicLand(landName)
  }, [addBasicLand])

  const handleRemoveLand = useCallback((landName: string) => {
    removeCard(landName, 'main')
  }, [removeCard])

  const deckCardNames = useMemo(() => {
    if (!deck) return new Set<string>()
    return new Set(deck.main.map(c => c.name))
  }, [deck])

  if (isLoading || !deck) {
    return (
      <div className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Skeleton className="w-full max-w-[1200px] h-[600px] rounded-lg" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="flex h-screen" style={{ gap: '32px', padding: '16px' }}>
        {/* Left panel: Card Search */}
        <div className="flex-1 min-w-0">
          <CardSearchPanel
            onCardClick={handleCardClick}
            onCardMouseEnter={(card, e) => onCardMouseEnter(card, e)}
            onCardMouseMove={onCardMouseMove}
            onCardMouseLeave={onCardMouseLeave}
            deckCardNames={deckCardNames}
          />
        </div>

        {/* Right panel: Deck Contents */}
        <div className="flex-1 min-w-0 border border-border rounded-lg overflow-hidden">
          <DeckPanel
            deck={deck}
            onIncrement={handleIncrement}
            onDecrement={handleDecrement}
            onCardMouseEnter={(card, e) => onCardMouseEnter(card, e)}
            onCardMouseMove={onCardMouseMove}
            onCardMouseLeave={onCardMouseLeave}
            onAddLand={handleAddLand}
            onRemoveLand={handleRemoveLand}
            onBack={handleBack}
            isDirty={isDirty}
            isSaving={isSaving}
            saveError={saveError}
          />
        </div>
      </div>

      {/* Hover Preview (fixed, above everything) */}
      <CardHoverPreview card={hoverCard} mousePos={mousePos} />
    </div>
  )
}
