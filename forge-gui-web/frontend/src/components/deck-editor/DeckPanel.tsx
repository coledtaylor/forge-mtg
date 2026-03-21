import { useState, useMemo } from 'react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../ui/tabs'
import { ToggleGroup, ToggleGroupItem } from '../ui/toggle-group'
import { List, LayoutGrid, ArrowLeft, Check, AlertCircle, Loader2, Play, Upload, Download, FlaskConical } from 'lucide-react'
import { Button } from '../ui/button'
import { GroupedDeckList } from './GroupedDeckList'
import { DeckGridView } from './DeckGridView'
import { BasicLandBar } from './BasicLandBar'
import { StatsPanel } from './StatsPanel'
import { MiniStats } from './MiniStats'
import { CommanderSlot } from './CommanderSlot'
import { SideboardPanel } from './SideboardPanel'
import { totalCards } from '../../lib/deck-stats'
import type { DeckDetail, DeckCardEntry, ValidationResult } from '../../types/deck'

type ViewMode = 'list' | 'grid'

interface DeckPanelProps {
  deck: DeckDetail
  onIncrement: (cardName: string) => void
  onDecrement: (cardName: string) => void
  onCardMouseEnter: (card: DeckCardEntry, e: React.MouseEvent) => void
  onCardMouseMove: (e: React.MouseEvent) => void
  onCardMouseLeave: () => void
  onAddLand: (landName: string) => void
  onRemoveLand: (landName: string) => void
  onBack: () => void
  onPlayDeck?: () => void
  isDirty: boolean
  isSaving: boolean
  saveError: boolean
  illegalCards?: Map<string, string>
  isCommanderFormat?: boolean
  onSetCommander?: (card: DeckCardEntry) => void
  format: string
  validation: ValidationResult | null | undefined
  isValidating: boolean
  commander: DeckCardEntry | null
  sideboardCards: DeckCardEntry[]
  onSideboardIncrement: (cardName: string) => void
  onSideboardDecrement: (cardName: string) => void
  onRemoveCommander: () => void
  onTabChange?: (tab: string) => void
  onSimulate?: () => void
  onImportOpen: () => void
  onExportOpen: () => void
}

export function DeckPanel({
  deck, onIncrement, onDecrement,
  onCardMouseEnter, onCardMouseMove, onCardMouseLeave,
  onAddLand, onRemoveLand, onBack, onPlayDeck,
  isDirty, isSaving, saveError,
  illegalCards, isCommanderFormat, onSetCommander,
  format, validation, isValidating,
  commander, sideboardCards,
  onSideboardIncrement, onSideboardDecrement,
  onRemoveCommander, onTabChange,
  onSimulate, onImportOpen, onExportOpen,
}: DeckPanelProps) {
  const [viewMode, setViewMode] = useState<ViewMode>('list')
  const count = useMemo(() => totalCards(deck.main), [deck.main])
  const isJumpstartFormat = format?.toLowerCase() === 'jumpstart'

  const landCounts = useMemo(() => {
    const counts: Record<string, number> = {}
    for (const card of deck.main) {
      if (['Plains', 'Island', 'Swamp', 'Mountain', 'Forest'].includes(card.name)) {
        counts[card.name] = card.quantity
      }
    }
    return counts
  }, [deck.main])

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 shrink-0 border-b border-border">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={onBack}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back to Decks
          </Button>
          <span className="text-[14px] font-semibold text-foreground">{deck.name}</span>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={onImportOpen} title="Import Deck">
            <Upload className="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="icon" onClick={onExportOpen} title="Export Deck">
            <Download className="h-4 w-4" />
          </Button>
          {onSimulate && (
            <Button variant="outline" size="sm" onClick={onSimulate} className="gap-1.5" title="Simulate deck">
              <FlaskConical className="h-3.5 w-3.5" />
              Simulate
            </Button>
          )}
          {onPlayDeck && (
            <Button variant="default" size="sm" onClick={onPlayDeck} className="gap-1.5">
              <Play className="h-3.5 w-3.5" />
              Play This Deck
            </Button>
          )}
          {/* Save status */}
          <span className="text-[12px] text-muted-foreground flex items-center gap-1">
            {saveError ? (
              <><AlertCircle className="h-3 w-3 text-destructive" /><span className="text-destructive">Save failed</span></>
            ) : isSaving ? (
              <><Loader2 className="h-3 w-3 animate-spin" />Saving...</>
            ) : isDirty ? (
              'Unsaved'
            ) : (
              <><Check className="h-3 w-3" />Saved</>
            )}
          </span>
        </div>
      </div>

      {/* Commander Slot */}
      {isCommanderFormat && (
        <div className="px-4 pt-3 shrink-0">
          <CommanderSlot commander={commander} onRemove={onRemoveCommander} />
        </div>
      )}

      {/* Tabs */}
      <Tabs defaultValue="deck" className="flex-1 flex flex-col overflow-hidden" onValueChange={(v) => { if (onTabChange) onTabChange(v) }}>
        <div className="flex items-center justify-between px-4 py-2 shrink-0">
          <TabsList>
            <TabsTrigger value="deck">Deck</TabsTrigger>
            <TabsTrigger value="stats">Stats</TabsTrigger>
            {!isJumpstartFormat && <TabsTrigger value="sideboard">Sideboard</TabsTrigger>}
          </TabsList>

          <div className="flex items-center gap-3">
            <ToggleGroup
              value={[viewMode]}
              onValueChange={(newValue) => {
                if (newValue.length > 0) setViewMode(newValue[newValue.length - 1] as ViewMode)
              }}
            >
              <ToggleGroupItem value="list" aria-label="List view">
                <List className="h-4 w-4" />
              </ToggleGroupItem>
              <ToggleGroupItem value="grid" aria-label="Grid view">
                <LayoutGrid className="h-4 w-4" />
              </ToggleGroupItem>
            </ToggleGroup>
            {isJumpstartFormat ? (
              <span className={`text-[14px] ${count === 20 ? 'text-emerald-500' : 'text-amber-500'}`}>
                {count}/20 cards
              </span>
            ) : (
              <span className="text-[14px] text-muted-foreground">{count} cards</span>
            )}
          </div>
        </div>

        <TabsContent value="deck" className="flex-1 overflow-y-auto px-4 mt-0">
          {viewMode === 'list' ? (
            <GroupedDeckList
              cards={deck.main}
              onIncrement={onIncrement}
              onDecrement={onDecrement}
              onCardMouseEnter={onCardMouseEnter}
              onCardMouseMove={onCardMouseMove}
              onCardMouseLeave={onCardMouseLeave}
              illegalCards={illegalCards}
              isCommanderFormat={isCommanderFormat}
              onSetCommander={onSetCommander}
            />
          ) : (
            <DeckGridView
              cards={deck.main}
              onCardMouseEnter={onCardMouseEnter}
              onCardMouseMove={onCardMouseMove}
              onCardMouseLeave={onCardMouseLeave}
            />
          )}
        </TabsContent>

        <TabsContent value="stats" className="flex-1 overflow-y-auto px-4 mt-0">
          <StatsPanel cards={deck.main} format={format} validation={validation} isValidating={isValidating} />
        </TabsContent>

        {!isJumpstartFormat && (
          <TabsContent value="sideboard" className="flex-1 overflow-y-auto px-4 mt-0">
            <SideboardPanel
              cards={sideboardCards}
              onIncrement={onSideboardIncrement}
              onDecrement={onSideboardDecrement}
              onCardMouseEnter={onCardMouseEnter}
              onCardMouseMove={onCardMouseMove}
              onCardMouseLeave={onCardMouseLeave}
            />
          </TabsContent>
        )}
      </Tabs>

      {/* Mini Stats */}
      <div className="shrink-0">
        <MiniStats cards={deck.main} />
      </div>

      {/* Basic Land Bar */}
      <div className="shrink-0">
        <BasicLandBar
          onAddLand={onAddLand}
          onRemoveLand={onRemoveLand}
          landCounts={landCounts}
        />
      </div>
    </div>
  )
}
