import { useState, useCallback, useMemo } from 'react'
import { ImportDeckDialog } from './ImportDeckDialog'
import { ExportDeckDialog } from './ExportDeckDialog'
import type { ParseToken } from '../../types/deck'
import { useDeckEditor } from '../../hooks/useDeckEditor'
import { useValidateDeck } from '../../hooks/useDecks'
import { useCardHover } from '../../hooks/useCardHover'
import { CardSearchPanel } from './CardSearchPanel'
import { DeckPanel } from './DeckPanel'
import { CardHoverPreview } from './CardHoverPreview'
import { Skeleton } from '../ui/skeleton'
import type { CardSearchResult } from '../../types/card'

interface DeckEditorProps {
  deckName: string
  format?: string
  onBack: () => void
  onPlayDeck?: () => void
}

export function DeckEditor({ deckName, format, onBack, onPlayDeck }: DeckEditorProps) {
  const {
    deck, isLoading, isDirty, isSaving, saveError,
    addCard, removeCard, setQuantity, setCommander, removeCommander, addBasicLand, importCards, flushSave,
  } = useDeckEditor(deckName, format)

  const { data: validation, isLoading: isValidating } = useValidateDeck(deckName, format || '')

  const { hoverCard, mousePos, onCardMouseEnter, onCardMouseMove, onCardMouseLeave } = useCardHover()

  const [activeSection, setActiveSection] = useState<'main' | 'sideboard'>('main')
  const [importOpen, setImportOpen] = useState(false)
  const [exportOpen, setExportOpen] = useState(false)

  const handleImport = useCallback((tokens: ParseToken[], mode: 'replace' | 'add', rawText: string) => {
    importCards(tokens, mode, rawText)
  }, [importCards])

  const isCommanderFormat = format?.toLowerCase() === 'commander'

  const illegalCards = useMemo(() => {
    if (!validation?.illegalCards) return undefined
    const map = new Map<string, string>()
    for (const card of validation.illegalCards) {
      map.set(card.name, `${card.name} is not legal in ${format}: ${card.reason}`)
    }
    return map
  }, [validation, format])

  const commanderColorIdentity = useMemo(() => {
    if (!isCommanderFormat || !deck || deck.commander.length === 0) return null
    const colors = deck.commander[0].colors
    if (!colors || colors.length === 0) return null
    return new Set(colors)
  }, [isCommanderFormat, deck])

  // Flush save on navigation away
  const handleBack = useCallback(() => {
    flushSave()
    onBack()
  }, [flushSave, onBack])

  const handlePlayDeck = useCallback(() => {
    flushSave()
    onPlayDeck?.()
  }, [flushSave, onPlayDeck])

  const handleCardClick = useCallback((card: CardSearchResult) => {
    addCard(card, activeSection)
  }, [addCard, activeSection])

  const handleIncrement = useCallback((cardName: string) => {
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
            commanderColorIdentity={commanderColorIdentity}
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
            onPlayDeck={onPlayDeck ? handlePlayDeck : undefined}
            isDirty={isDirty}
            isSaving={isSaving}
            saveError={saveError}
            illegalCards={illegalCards}
            isCommanderFormat={isCommanderFormat}
            onSetCommander={setCommander}
            format={format || ''}
            validation={validation ?? null}
            isValidating={isValidating}
            commander={deck.commander.length > 0 ? deck.commander[0] : null}
            sideboardCards={deck.sideboard}
            onSideboardIncrement={(name) => {
              const entry = deck.sideboard.find(c => c.name === name)
              if (entry) setQuantity(name, entry.quantity + 1, 'sideboard')
            }}
            onSideboardDecrement={(name) => removeCard(name, 'sideboard')}
            onRemoveCommander={removeCommander}
            onTabChange={(tab) => {
              if (tab === 'sideboard' && format?.toLowerCase() === 'jumpstart') return
              setActiveSection(tab === 'sideboard' ? 'sideboard' : 'main')
            }}
            onImportOpen={() => setImportOpen(true)}
            onExportOpen={() => setExportOpen(true)}
          />
        </div>
      </div>

      {/* Import/Export Dialogs */}
      <ImportDeckDialog open={importOpen} onOpenChange={setImportOpen} onImport={handleImport} format={format} />
      <ExportDeckDialog open={exportOpen} onOpenChange={setExportOpen} deckName={deckName} />

      {/* Hover Preview (fixed, above everything) */}
      <CardHoverPreview card={hoverCard} mousePos={mousePos} />
    </div>
  )
}
