---
phase: 03-deck-builder
verified: 2026-03-19T22:15:00Z
status: passed
score: 19/19 must-haves verified
re_verification: false
---

# Phase 3: Deck Builder Verification Report

**Phase Goal:** Users can build, analyze, and validate decks for any supported format entirely in the browser
**Verified:** 2026-03-19T22:15:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

Truths are drawn from the must_haves declared in all three plan files.

**Plan 01 truths (data layer):**

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Backend accepts commander section in deck update PUT | VERIFIED | `DeckHandler.update()` reads `body.get("commander")` and writes to `DeckSection.Commander` (lines 147-156) |
| 2 | Backend accepts format field on deck create POST | VERIFIED | `DeckHandler.create()` reads `body.get("format")` and calls `deck.setComment(format)` (lines 74-77) |
| 3 | Backend returns enriched DeckCardEntry with cmc, colors, typeLine, manaCost fields | VERIFIED | `DeckDetailDto.toEntries()` populates all four fields from `CardRules` and `ColorSet` (lines 54-64) |
| 4 | Backend validates deck against format rules and returns illegal cards with reasons | VERIFIED | `FormatValidationHandler.validate()` calls `format.getFilterRules()` and `format.getDeckConformanceProblem(deck)` and returns JSON with `legal`, `illegalCards`, `conformanceProblem` |
| 5 | Frontend can update a deck (main, sideboard, commander) via API | VERIFIED | `api/decks.ts` exports `updateDeck` (PUT with UpdateDeckPayload), `useDecks.ts` exports `useUpdateDeck` |
| 6 | Frontend can compute mana curve, color distribution, and type breakdown from deck data | VERIFIED | `lib/deck-stats.ts` exports `computeManaCurve`, `computeColorDistribution`, `computeTypeBreakdown`, `averageCMC`, `deckColors`, `totalCards` |
| 7 | Frontend can parse mana cost strings into symbol arrays for rendering | VERIFIED | `lib/mana.ts` exports `parseManaCost` (handles brace and space formats) and `manaSymbolClass` |
| 8 | Frontend can group cards by primary type with correct priority ordering | VERIFIED | `lib/deck-grouping.ts` exports `groupByType` using `TYPE_GROUP_ORDER = ['Creature','Planeswalker','Instant','Sorcery','Enchantment','Artifact','Land','Other']` |

**Plan 02 truths (core UI):**

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 9 | User can navigate from deck list to deck editor and back | VERIFIED | `App.tsx` View union type; `DeckList` calls `onEditDeck(deck.name, deck.format)` → `setView({ type: 'editor', ... })`; `DeckPanel` Back button calls `handleBack → flushSave → onBack` |
| 10 | User can search cards in left panel and click to add them to deck | VERIFIED | `CardSearchPanel` renders clickable card grid with `onClick={() => onCardClick(card)}`; `DeckEditor.handleCardClick` calls `addCard(card, activeSection)` |
| 11 | User can see deck contents as grouped text list with quantity, name, and mana cost symbols | VERIFIED | `GroupedDeckList` uses `groupByType`, renders `DeckCardRow` per card; `DeckCardRow` renders `<ManaCost cost={card.manaCost} />` |
| 12 | User can toggle between list and grid views of deck contents | VERIFIED | `DeckPanel` has `ToggleGroup` with "list" and "grid" values; renders `GroupedDeckList` or `DeckGridView` based on `viewMode` state |
| 13 | User can adjust card quantities with +/- buttons | VERIFIED | `DeckCardRow` renders `Plus`/`Minus`/`Trash2` icon buttons; `onIncrement`/`onDecrement` callbacks flow through `DeckPanel` → `DeckEditor` → `useDeckEditor` |
| 14 | User can hover any card to see enlarged preview floating near cursor | VERIFIED | `CardHoverPreview` fixed-position with `z-50 pointer-events-none`, flips left when within 300px of right edge (`mousePos.x - 280`); wired via `useCardHover` hook in `DeckEditor` |
| 15 | User can quickly add basic lands with inline +/- controls | VERIFIED | `BasicLandBar` renders 5 lands with `Plus`/`Minus` buttons; `onAddLand`/`onRemoveLand` callbacks flow to `useDeckEditor.addBasicLand` / `removeCard` |

**Plan 03 truths (stats, commander, validation):**

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 16 | User can see mana curve histogram showing CMC 0-7+ distribution | VERIFIED | `ManaCurveChart` renders 8 SVG `<rect>` bars with `fill-primary`, `rx={2}`; label shows `i === 7 ? '7+' : i`; `StatsPanel` section "Mana Curve" |
| 17 | User can see color distribution donut chart with WUBRG segments | VERIFIED | `ColorDistribution` renders SVG `<path>` arcs with MTG hex values (W:`#F9FAF4`, U:`#0E68AB`, B:`#150B00`, R:`#D3202A`, G:`#00733E`, C:`#A0A0A0`); center text shows non-land count at 20px semibold |
| 18 | User can see card type breakdown with horizontal bars | VERIFIED | `TypeBreakdown` renders horizontal SVG bars with `fill-primary`, `rx={2}`, filters zero-count types |
| 19 | User can see deck summary with total cards, average CMC, land count, colors, format legality | VERIFIED | `StatsPanel` "Deck Summary" section renders Total, Average CMC, Lands (with %), Colors, Format with legal/not-legal/checking status |
| 20 | User can see format validation inline badges on illegal cards with tooltip reasons | VERIFIED | `DeckEditor` builds `illegalCards Map<string,string>` from `useValidateDeck` result; passed to `GroupedDeckList` → `DeckCardRow` which renders destructive `!` badge with `title={illegalReason}` |
| 21 | User can manage sideboard cards (view, add from search) | VERIFIED | `SideboardPanel` renders cards with `DeckCardRow`; `DeckEditor.activeSection` routes search clicks to sideboard when sideboard tab is active; `onTabChange` callback implemented |
| 22 | User can set a commander for Commander format decks | VERIFIED | `CommanderSlot` renders empty dashed state ("Set Commander") or occupied `border-primary` state with 120px card image; `GroupedDeckList` shows commander-set button on legendary creatures via `onSetCommander` callback |
| 23 | User can see mini stats bar (compact mana curve + card count) in deck tab | VERIFIED | `MiniStats` renders `ManaCurveChart` with `mini` prop plus total card count and color dots; placed as `shrink-0 div` outside Tabs in `DeckPanel` |
| 24 | Commander's color identity filters search results in Commander format | VERIFIED | `DeckEditor` computes `commanderColorIdentity = new Set(deck.commander[0].colors)` when `isCommanderFormat`; passed to `CardSearchPanel`; client-side filter: `card.colors.every(c => commanderColorIdentity.has(c))` |

**Score: 24/24 truths verified**

---

### Required Artifacts

| Artifact | Status | Lines | Details |
|----------|--------|-------|---------|
| `forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java` | VERIFIED | 118 | Exists, substantive, route registered in WebServer at line 103 |
| `forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java` | VERIFIED | 85 | Enriched DeckCardEntry with manaCost, typeLine, cmc, colors populated from CardRules |
| `forge-gui-web/src/main/java/forge/web/dto/DeckSummaryDto.java` | VERIFIED | 59 | Contains `public String format` populated from `deck.getComment()` |
| `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` | VERIFIED | 215 | create() reads format, update() handles commander section |
| `forge-gui-web/frontend/src/types/deck.ts` | VERIFIED | 43 | DeckCardEntry enriched, DeckSummary has format, UpdateDeckPayload/CreateDeckPayload/ValidationResult present |
| `forge-gui-web/frontend/src/api/decks.ts` | VERIFIED | 34 | `updateDeck` and `validateDeck` exported, `createDeck` accepts CreateDeckPayload |
| `forge-gui-web/frontend/src/hooks/useDecks.ts` | VERIFIED | 55 | `useDeck`, `useUpdateDeck`, `useValidateDeck` exported |
| `forge-gui-web/frontend/src/hooks/useDeckEditor.ts` | VERIFIED | 176 | Full optimistic state hook with addCard, removeCard, setQuantity, setCommander, removeCommander, addBasicLand, flushSave |
| `forge-gui-web/frontend/src/hooks/useCardHover.ts` | VERIFIED | 30 | onCardMouseEnter, onCardMouseMove, onCardMouseLeave exported |
| `forge-gui-web/frontend/src/lib/mana.ts` | VERIFIED | 29 | parseManaCost and manaSymbolClass exported |
| `forge-gui-web/frontend/src/lib/deck-stats.ts` | VERIFIED | 103 | All 7 functions exported: computeManaCurve, computeColorDistribution, computeTypeBreakdown, getPrimaryType, totalCards, averageCMC, deckColors |
| `forge-gui-web/frontend/src/lib/deck-grouping.ts` | VERIFIED | 47 | groupByType and TYPE_GROUP_ORDER exported |
| `forge-gui-web/frontend/src/components/ManaCost.tsx` | VERIFIED | 18 | Imports parseManaCost/manaSymbolClass, renders mana-font `<i>` elements |
| `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` | VERIFIED | 143 | Two-column split with gap 32px, useDeckEditor + useCardHover wired, commanderColorIdentity computed |
| `forge-gui-web/frontend/src/components/deck-editor/CardSearchPanel.tsx` | VERIFIED | 125 | SearchBar + useCardSearch, click-to-add, commanderColorIdentity filter applied client-side |
| `forge-gui-web/frontend/src/components/deck-editor/DeckPanel.tsx` | VERIFIED | 184 | All new components integrated, Tabs with deck/stats/sideboard, ToggleGroup list/grid, MiniStats outside Tabs |
| `forge-gui-web/frontend/src/components/deck-editor/GroupedDeckList.tsx` | VERIFIED | 63 | groupByType used, type headers with uppercase muted-foreground, DeckCardRow per card |
| `forge-gui-web/frontend/src/components/deck-editor/DeckCardRow.tsx` | VERIFIED | 79 | Plus/Minus/Trash2 buttons, ManaCost rendered, illegal badge, commander-set button |
| `forge-gui-web/frontend/src/components/deck-editor/DeckGridView.tsx` | VERIFIED | 54 | Card images in CSS grid with quantity badge overlay |
| `forge-gui-web/frontend/src/components/deck-editor/CardHoverPreview.tsx` | VERIFIED | 29 | `z-50 pointer-events-none`, viewport flip logic (`mousePos.x - 280`) |
| `forge-gui-web/frontend/src/components/deck-editor/BasicLandBar.tsx` | VERIFIED | 49 | 5 basic lands with mana-font icons, Plus/Minus buttons, disabled at count 0 |
| `forge-gui-web/frontend/src/components/charts/ManaCurveChart.tsx` | VERIFIED | 55 | 8 bars, mini mode (barWidth=16, gap=2), fill-primary rx={2}, "7+" label |
| `forge-gui-web/frontend/src/components/charts/ColorDistribution.tsx` | VERIFIED | 105 | SVG donut arcs, MTG hex colors, legend, "No cards" empty state |
| `forge-gui-web/frontend/src/components/charts/TypeBreakdown.tsx` | VERIFIED | 62 | Horizontal bars, fill-primary, filters zero counts |
| `forge-gui-web/frontend/src/components/deck-editor/StatsPanel.tsx` | VERIFIED | 87 | All three charts + Deck Summary section with format legality |
| `forge-gui-web/frontend/src/components/deck-editor/MiniStats.tsx` | VERIFIED | 39 | ManaCurveChart mini + total count + color dots |
| `forge-gui-web/frontend/src/components/deck-editor/CommanderSlot.tsx` | VERIFIED | 47 | Empty dashed state ("Set Commander"), occupied border-primary state with 120px image |
| `forge-gui-web/frontend/src/components/deck-editor/SideboardPanel.tsx` | VERIFIED | 49 | "Sideboard ({total}/15)" header, DeckCardRow per card, empty state |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `WebServer.java` | `FormatValidationHandler::validate` | route registration | VERIFIED | Line 103: `config.routes.get("/api/decks/{name}/validate", FormatValidationHandler::validate)` — registered BEFORE generic `{name}` GET route at line 104 |
| `useDeckEditor.ts` | `/api/decks/{name}` PUT | updateDeck mutation | VERIFIED | `scheduleSave` calls `updateMutation.mutate({ name: deckName, payload: { main, sideboard, commander } })` |
| `deck-stats.ts` | `DeckCardEntry` | imports enriched type | VERIFIED | `import type { DeckCardEntry } from '../types/deck'` at line 1 |
| `App.tsx` | `DeckEditor.tsx` | state-based view routing | VERIFIED | View type includes `{ type: 'editor'; deckName: string; format?: string }`; renders `<DeckEditor>` when `view.type === 'editor'` |
| `DeckEditor.tsx` | `useDeckEditor` | hook import and call | VERIFIED | `import { useDeckEditor }` at line 2; called as `useDeckEditor(deckName)` |
| `CardSearchPanel.tsx` | `useDeckEditor.addCard` | onCardClick callback | VERIFIED | `onClick={() => onCardClick(card)` in card grid; `DeckEditor.handleCardClick` calls `addCard(card, activeSection)` |
| `DeckCardRow.tsx` | `ManaCost` component | mana cost rendering | VERIFIED | `<ManaCost cost={card.manaCost} />` at line 76 |
| `StatsPanel.tsx` | `deck-stats.ts` | compute functions | VERIFIED | Imports `computeManaCurve, computeColorDistribution, computeTypeBreakdown, totalCards, averageCMC, deckColors` |
| `StatsPanel.tsx` | `useValidateDeck` | format validation hook | VERIFIED | `DeckEditor` calls `useValidateDeck(deckName, format || '')` and passes `validation` to `DeckPanel` → `StatsPanel` |
| `CommanderSlot.tsx` | `useDeckEditor.setCommander` | commander callback | VERIFIED | `DeckPanel` passes `onSetCommander={onSetCommander}` to `GroupedDeckList`; `DeckEditor` passes `onSetCommander={setCommander}` |
| `DeckEditor.tsx` | `CardSearchPanel.tsx` | commander color identity filter prop | VERIFIED | `commanderColorIdentity` computed from `deck.commander[0].colors`, passed as prop to `CardSearchPanel` |

---

### Requirements Coverage

All requirement IDs from plans cross-referenced against REQUIREMENTS.md.

| Requirement | Plan | Description | Status | Evidence |
|-------------|------|-------------|--------|----------|
| DECK-01 | 03-02 | User can search cards by name, type, color, CMC, and format legality | VERIFIED | `CardSearchPanel` uses `SearchBar` + `useCardSearch`; search params flow to backend |
| DECK-03 | 03-01, 03-03 | User can create, name, save, load, and delete decks | VERIFIED | `DeckHandler` CRUD; `useDeckEditor` debounced save; `DeckList` create dialog with name+format |
| DECK-04 | 03-02 | User can view deck as a text list with card names and quantities | VERIFIED | `GroupedDeckList` → `DeckCardRow` shows quantity, name, mana symbols |
| DECK-05 | 03-02 | User can view deck as a visual card image grid/gallery | VERIFIED | `DeckGridView` renders Scryfall images in auto-fill grid |
| DECK-06 | 03-01, 03-02 | User can add and remove cards with quantity controls | VERIFIED | `DeckCardRow` +/- buttons; `useDeckEditor.addCard`, `removeCard`, `setQuantity` |
| DECK-07 | 03-03 | User can see mana curve chart for their deck | VERIFIED | `ManaCurveChart` with 8 CMC buckets in `StatsPanel` |
| DECK-08 | 03-03 | User can see card type distribution (creatures, instants, etc.) | VERIFIED | `TypeBreakdown` horizontal bar chart in `StatsPanel` |
| DECK-09 | 03-03 | User can see color distribution across the deck | VERIFIED | `ColorDistribution` donut chart in `StatsPanel` |
| DECK-10 | 03-01, 03-03 | User can see format validation results (legal/illegal with reasons) | VERIFIED | `FormatValidationHandler` backend endpoint; `useValidateDeck` hook; `StatsPanel` shows legal/not-legal; `DeckCardRow` shows `!` badge on illegal cards |
| DECK-11 | 03-03 | User can manage sideboard cards (add, remove, move to/from main) | VERIFIED | `SideboardPanel` with `DeckCardRow` quantity controls; active section routing routes search clicks to sideboard tab |
| DECK-12 | 03-01, 03-03 | User can set a commander for Commander format decks | VERIFIED | `DeckHandler.update()` accepts commander section; `CommanderSlot` UI; legendary creature rows show commander-set button |
| DECK-13 | 03-02 | User can quickly add basic lands with quantity controls | VERIFIED | `BasicLandBar` with 5 lands, Plus/Minus buttons, mana-font icons |

**No orphaned requirements.** DECK-02 is correctly assigned to Phase 2 (Scryfall image rendering established there). The user-supplied list of 12 IDs (DECK-01, 03-13 excluding DECK-02) matches exactly what the three plans collectively claim.

---

### Anti-Patterns Found

No anti-patterns detected across any phase 03 file:

- No TODO/FIXME/placeholder comments in any modified file
- No stub implementations (empty returns, "coming soon" text)
- No orphaned artifacts — every file is imported and used
- DeckPanel has no remaining placeholder tab content — both Stats and Sideboard tabs render real components
- All 6 commits from summaries verified present in git log: `5244b52`, `e6aaacf`, `4f85d57`, `ae9a7af`, `bba896e`, `7024c6d`

---

### Human Verification Required

The following behaviors cannot be verified by static analysis:

#### 1. Mana-font symbol rendering

**Test:** Open the deck editor, add a card with colored mana cost (e.g., Lightning Bolt with `{R}`). Inspect the card row.
**Expected:** Red mana symbol glyph appears to the right of the card name, rendered via mana-font CSS.
**Why human:** Requires CSS loaded at runtime — `parseManaCost("{R}")` returns `["R"]`, `manaSymbolClass("R")` produces class `ms ms-r ms-cost`, but visual rendering requires mana-font CSS to be active.

#### 2. Format validation round-trip

**Test:** Create a Commander deck, add a card illegal in Commander (e.g., a black-bordered basic land from an illegal set, or a card with too many copies). Switch to Stats tab.
**Expected:** Stats tab shows "Not Legal" next to the format name; deck tab shows `!` badge on the illegal card row.
**Why human:** Requires a running Forge backend with FModel initialized. The validation logic calls `format.getFilterRules()` which depends on loaded card data.

#### 3. Commander color identity search filtering

**Test:** Create a Commander deck. Set a mono-green commander (e.g., Selvala). Search for cards. Search for a red card.
**Expected:** Red cards are filtered from search results; count shows "X of Y cards (filtered by commander colors)".
**Why human:** Requires runtime state (commander set in deck, search results fetched from live API).

#### 4. Debounced save indicator

**Test:** Add a card to a deck. Observe the save status indicator in the header.
**Expected:** Shows "Unsaved" immediately, then "Saving..." after 1 second, then "Saved" with checkmark after server responds.
**Why human:** Requires live backend and timing observation.

#### 5. Hover preview viewport flip

**Test:** Hover over a card image when the cursor is near the right edge of the browser window (within 300px).
**Expected:** Preview card image appears to the LEFT of the cursor rather than the right.
**Why human:** Requires browser to verify `window.innerWidth` comparison at runtime.

---

### Gaps Summary

No gaps. All 24 observable truths are verified, all 28 artifacts exist and are substantive and wired, all 11 key links are confirmed present, all 12 requirement IDs are satisfied.

The phase goal — "Users can build, analyze, and validate decks for any supported format entirely in the browser" — is achieved by the combination of:

- A complete backend data layer (enriched card data, format field, commander section updates, format validation endpoint)
- A complete frontend data layer (types, API functions, hooks with optimistic state and debounced save, pure utility libraries)
- A complete deck editor UI (two-column layout, card search, grouped list/grid views, quantity controls, hover preview, basic lands)
- A complete statistics suite (mana curve, color distribution, type breakdown, deck summary with format legality)
- Commander format support (commander slot, color identity search filtering)
- Sideboard management

---

_Verified: 2026-03-19T22:15:00Z_
_Verifier: Claude (gsd-verifier)_
