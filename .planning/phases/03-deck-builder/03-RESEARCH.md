# Phase 3: Deck Builder - Research

**Researched:** 2026-03-19
**Domain:** React deck builder UI with charting, format validation, and card management
**Confidence:** HIGH

## Summary

Phase 3 builds a full deck editor experience on top of the existing Phase 2 infrastructure (card search, deck CRUD API, Scryfall images). The frontend already has SearchBar, CardGrid, CardImage, and DeckList components plus TanStack Query hooks and a Tailwind/shadcn/ui design system. The backend has deck CRUD endpoints but needs two extensions: (1) commander section support in the update endpoint, and (2) a new format validation endpoint. The frontend needs routing (currently none), an updateDeck API function, and substantial new UI components.

The charting needs are simple (mana curve histogram, color pie chart, type breakdown) with at most 8-10 data points per chart. Custom SVG components are the right approach -- no charting library needed for bar charts and donut charts this simple. Mana cost symbols should use the `mana` CSS font from Andrew Gioia's project (the de facto standard for MTG web apps). The app needs client-side routing to navigate between deck list and deck editor views.

**Primary recommendation:** Build custom SVG chart components (trivial at this data scale), use `mana` font for mana symbols, add simple hash-based routing with React state, and extend the backend to handle commander updates and format validation.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Two-column split layout: left panel = card search with results grid, right panel = deck contents + stats
- Fixed 50/50 split ratio (min viewport 1280px, so ~640px each side)
- Hover on any card shows enlarged card image floating near cursor (Scryfall-style hover preview)
- Click a search result card to add 1 copy to deck
- Default view: grouped text list organized by card type (Creatures, Instants, Sorceries, Enchantments, Artifacts, Planeswalkers, Lands)
- Toggle button (list/grid icon) in deck panel header to switch between grouped text list and visual card grid
- Each card row in text list shows: quantity + name + mana cost symbols (e.g., "4x Lightning Bolt {R}")
- +/- buttons on each card row for quantity control; reaching 0 removes the card
- Right panel has tab toggle: "Deck" | "Stats"
- Deck tab includes mini stats summary: small mana curve bar chart + total card count + color dots (~80px height)
- Stats tab is full-width with detailed charts: mana curve histogram, color distribution pie/donut, card type breakdown, deck summary panel
- Format selected at deck creation time (fixed for that deck)
- Format validation results shown as inline red badges/icons on illegal cards with tooltip explaining why
- Validation summary in stats tab deck summary panel
- Commander format decks get dedicated commander slot at top of deck panel
- Click legendary creature to set as commander
- Commander's color identity filters search results
- Inline land bar at bottom of deck panel: Plains/Island/Swamp/Mountain/Forest icons with +/- buttons each
- Always visible when editing a deck

### Claude's Discretion
- Chart library choice (recharts, chart.js, or lightweight custom SVG)
- Exact mana cost symbol rendering approach
- Transition animations between views
- Error handling for API failures
- Sideboard UI specifics (likely a tab or collapsible section within deck panel)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DECK-01 | User can search cards by name, type, color, CMC, and format legality | SearchBar + useCardSearch already built in Phase 2; needs integration into split layout left panel |
| DECK-03 | User can create, name, save, load, and delete decks | Create/delete exist; need updateDeck API function + format field on create; deck editor load via routing |
| DECK-04 | User can view deck as a text list with card names and quantities | New GroupedDeckList component with card type sections and quantity controls |
| DECK-05 | User can view deck as a visual card image grid/gallery | Existing CardGrid can be reused/adapted for deck contents view |
| DECK-06 | User can add and remove cards with quantity controls | Click-to-add from search + +/- buttons in deck list; updateDeck mutation |
| DECK-07 | User can see mana curve chart for their deck | Custom SVG bar chart component (8 bars for CMC 0-7+) |
| DECK-08 | User can see card type distribution | Custom SVG horizontal bar or list chart |
| DECK-09 | User can see color distribution across the deck | Custom SVG donut chart (5 colors + colorless) |
| DECK-10 | User can see format validation results | New backend endpoint + frontend validation display with badges/tooltips |
| DECK-11 | User can manage sideboard cards | Sideboard tab in deck panel + backend already supports sideboard in update |
| DECK-12 | User can set a commander for Commander format decks | Backend needs commander section in update; frontend commander slot UI |
| DECK-13 | User can quickly add basic lands with quantity controls | Inline land bar component with +/- for each basic land type |
</phase_requirements>

## Standard Stack

### Core (Already Installed)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.4 | UI framework | Already in project |
| @tanstack/react-query | 5.91.2 | Server state management | Already used for card search + deck CRUD |
| Tailwind CSS | 4.2.2 | Styling | Already in project |
| shadcn/ui | 4.0.8 | Component primitives (badge, button, dialog, tabs) | Already in project |
| lucide-react | 0.577.0 | Icons | Already in project |

### New Dependencies
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| mana-font (keyrune) | N/A (CSS/font) | MTG mana cost symbol rendering | Mana symbols in deck list rows and stats |

**Note on mana-font:** The `mana` font by Andrew Gioia (https://mana.andrewgioia.com/) is the standard approach for MTG web apps. It provides a pictographic font with CSS classes for each mana symbol ({W}, {U}, {B}, {R}, {G}, {C}, {1}, {2}, etc.). Install via npm as `mana-font` or self-host the CSS/font files.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom SVG charts | Recharts | Recharts adds ~200KB for charts with <10 data points; custom SVG is trivial here |
| Custom SVG charts | Chart.js | Chart.js is canvas-based, harder to style with Tailwind, overkill for 3 simple charts |
| State-based routing | react-router | Full router is heavy for 2 views (deck list vs deck editor); simple state toggle suffices for now |
| mana-font | Scryfall SVG symbology API | Self-hosted font is faster and works offline; Scryfall API adds latency per symbol |

**Installation:**
```bash
npm install mana-font
```

**Version verification:**
```bash
npm view mana-font version
# If mana-font not on npm, self-host from https://github.com/andrewgioia/mana
```

## Architecture Patterns

### Recommended Project Structure
```
src/
├── components/
│   ├── deck-editor/
│   │   ├── DeckEditor.tsx          # Main 2-column layout container
│   │   ├── CardSearchPanel.tsx     # Left panel: search + results grid
│   │   ├── DeckPanel.tsx           # Right panel: deck contents + stats tabs
│   │   ├── GroupedDeckList.tsx     # Text list grouped by card type
│   │   ├── DeckCardRow.tsx         # Single card row with qty controls + mana cost
│   │   ├── DeckGridView.tsx        # Visual card grid view of deck
│   │   ├── StatsPanel.tsx          # Full stats tab with charts
│   │   ├── MiniStats.tsx           # Compact stats summary in deck tab
│   │   ├── CommanderSlot.tsx       # Commander card display + selection
│   │   ├── BasicLandBar.tsx        # Inline land quantity controls
│   │   ├── SideboardPanel.tsx      # Sideboard management
│   │   └── CardHoverPreview.tsx    # Floating enlarged card on hover
│   ├── charts/
│   │   ├── ManaCurveChart.tsx      # SVG bar chart for CMC distribution
│   │   ├── ColorDistribution.tsx   # SVG donut chart for color breakdown
│   │   └── TypeBreakdown.tsx       # SVG chart for card type counts
│   └── ui/                         # Existing shadcn components
├── hooks/
│   ├── useDeck.ts                  # Single deck fetch + update hooks
│   ├── useDeckEditor.ts           # Local deck state management (optimistic updates)
│   └── useCardHover.ts            # Hover preview positioning logic
├── api/
│   └── decks.ts                    # Add updateDeck + validateDeck functions
├── lib/
│   ├── mana.ts                     # Mana cost string parser ({W}{U}{2} -> component array)
│   ├── deck-stats.ts              # Pure functions: mana curve, color dist, type breakdown
│   └── deck-grouping.ts           # Group cards by type for list view
└── types/
    └── deck.ts                     # Extend with format field, validation types
```

### Pattern 1: Optimistic Local Deck State
**What:** Maintain deck contents in local React state during editing, sync to server via debounced mutations
**When to use:** Always in the deck editor -- avoids round-trip lag on every card add/remove
**Example:**
```typescript
// useDeckEditor.ts
function useDeckEditor(deckName: string) {
  const { data: serverDeck } = useDeck(deckName)
  const [localDeck, setLocalDeck] = useState<DeckDetail | null>(null)
  const updateMutation = useUpdateDeck()

  // Initialize from server
  useEffect(() => {
    if (serverDeck && !localDeck) setLocalDeck(serverDeck)
  }, [serverDeck])

  const addCard = (card: CardSearchResult) => {
    setLocalDeck(prev => {
      // Add to main, increment quantity if exists
      // ...
      return updated
    })
  }

  // Debounced save to server
  const save = useDebouncedCallback(() => {
    if (localDeck) {
      updateMutation.mutate({
        name: deckName,
        main: toCardMap(localDeck.main),
        sideboard: toCardMap(localDeck.sideboard),
        commander: toCardMap(localDeck.commander),
      })
    }
  }, 1000)

  // Trigger save on changes
  useEffect(() => { if (localDeck) save() }, [localDeck])

  return { deck: localDeck, addCard, removeCard, setCommander, ... }
}
```

### Pattern 2: Mana Cost Parsing
**What:** Parse Forge's mana cost strings into renderable symbol components
**When to use:** Any time displaying mana costs (deck list rows, stats panel)
**Example:**
```typescript
// lib/mana.ts
// Forge manaCost format: "{2}{W}{U}" or "2 W U" -- need to verify exact format
// CardSearchDto.manaCost comes from rules.getManaCost().toString()

function parseManaCost(manaCost: string): string[] {
  // Extract symbols like {W}, {U}, {2}, {X}, etc.
  const symbols: string[] = []
  const regex = /\{([^}]+)\}/g
  let match
  while ((match = regex.exec(manaCost)) !== null) {
    symbols.push(match[1])
  }
  return symbols
}

// Render with mana-font CSS classes
function ManaCost({ cost }: { cost: string }) {
  const symbols = parseManaCost(cost)
  return (
    <span className="inline-flex items-center gap-0.5">
      {symbols.map((s, i) => (
        <i key={i} className={`ms ms-${s.toLowerCase()} ms-cost`} />
      ))}
    </span>
  )
}
```

### Pattern 3: Simple View Routing
**What:** State-based navigation between deck list and deck editor views
**When to use:** Instead of full react-router for 2-view app
**Example:**
```typescript
// App.tsx
type View = { type: 'list' } | { type: 'editor', deckName: string }

function AppContent() {
  const [view, setView] = useState<View>({ type: 'list' })

  if (view.type === 'editor') {
    return <DeckEditor
      deckName={view.deckName}
      onBack={() => setView({ type: 'list' })}
    />
  }
  return <DeckListView onEdit={(name) => setView({ type: 'editor', deckName: name })} />
}
```

### Pattern 4: Custom SVG Charts
**What:** Lightweight SVG charts rendered directly in React
**When to use:** Mana curve, color distribution, type breakdown -- all <10 data points
**Example:**
```typescript
// charts/ManaCurveChart.tsx
interface ManaCurveProps {
  curve: number[] // [count0, count1, count2, ..., count7plus]
}

function ManaCurveChart({ curve }: ManaCurveProps) {
  const max = Math.max(...curve, 1)
  const barWidth = 32
  const height = 120
  const gap = 4

  return (
    <svg width={curve.length * (barWidth + gap)} height={height + 20}>
      {curve.map((count, i) => {
        const barHeight = (count / max) * height
        return (
          <g key={i}>
            <rect
              x={i * (barWidth + gap)}
              y={height - barHeight}
              width={barWidth}
              height={barHeight}
              className="fill-primary"
              rx={2}
            />
            <text
              x={i * (barWidth + gap) + barWidth / 2}
              y={height + 14}
              textAnchor="middle"
              className="fill-muted-foreground text-[11px]"
            >
              {i === 7 ? '7+' : i}
            </text>
            {count > 0 && (
              <text
                x={i * (barWidth + gap) + barWidth / 2}
                y={height - barHeight - 4}
                textAnchor="middle"
                className="fill-foreground text-[11px]"
              >
                {count}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
```

### Anti-Patterns to Avoid
- **Server round-trip per card add:** Never PUT the entire deck to the server on every single card click. Batch changes locally, debounce the save.
- **Derived state in React state:** Mana curve, color distribution, type counts are derived from deck contents. Compute them in render or useMemo, not in separate state variables.
- **Prop drilling deck state:** The deck editor has many nested components. Use a single `useDeckEditor` hook at the DeckEditor level and pass down specific callbacks.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Mana symbols | SVG sprites for each mana symbol | mana-font CSS font | 400+ symbols including hybrid, phyrexian, split; font handles all edge cases |
| Debounced saves | Custom setTimeout wrapper | `useDebouncedCallback` from `use-debounce` or inline with useRef | Edge cases around unmounting, stale closures |
| Card type grouping | Ad-hoc string matching on typeLine | Structured parser using typeLine field | "Legendary Enchantment Creature" needs correct primary type extraction |
| Format validation logic | Client-side banned list checking | Backend endpoint using Forge's GameFormat | Forge already has complete format data; duplicating it is error-prone |

**Key insight:** The backend already has comprehensive format validation via GameFormat. Expose it as an endpoint rather than reimplementing banned/restricted/set legality checks in JavaScript.

## Common Pitfalls

### Pitfall 1: DeckHandler.update() Missing Commander Section
**What goes wrong:** Backend update endpoint only handles `main` and `sideboard` sections, not `commander`. Setting a commander via PUT will silently do nothing.
**Why it happens:** Phase 2 built basic CRUD; commander was deferred to Phase 3.
**How to avoid:** Extend DeckHandler.update() to accept a `commander` field in the request body, similar to main/sideboard handling. Use `DeckSection.Commander` to store it.
**Warning signs:** Commander selection appears to work in UI but doesn't persist after refresh.

### Pitfall 2: Deck Create Needs Format Field
**What goes wrong:** The create endpoint accepts only `name`. Without a format field, there's no way to associate a format with a deck for validation.
**Why it happens:** Phase 2's basic create was minimal.
**How to avoid:** Either add a `format` field to deck creation/metadata, or store format as a deck metadata field. Forge's Deck class may support metadata tags -- investigate `Deck.getComment()` or similar.
**Warning signs:** Format dropdown on creation does nothing; validation always says "no format selected."

### Pitfall 3: Mana Cost String Format Uncertainty
**What goes wrong:** Mana cost parser breaks because Forge's `ManaCost.toString()` uses a different format than expected.
**Why it happens:** Assuming `{W}{U}` format without verifying Forge's actual output.
**How to avoid:** Test with actual API response. Check `ManaCost.toString()` in Forge source. The format might be `W U` (space-separated) or `{W}{U}` (braced).
**Warning signs:** Mana symbols render as raw text or show wrong symbols.

### Pitfall 4: Card Type Extraction from typeLine
**What goes wrong:** Grouping "Legendary Enchantment Creature -- Elf Druid" incorrectly into Enchantments instead of Creatures.
**Why it happens:** Naive string matching on typeLine without understanding MTG type hierarchy.
**How to avoid:** Parse primary card type with priority order: Creature > Planeswalker > Instant > Sorcery > Enchantment > Artifact > Land. Check if typeLine contains each in priority order.
**Warning signs:** Cards appear in wrong type groups in the deck list.

### Pitfall 5: Search Results Not Refreshing When Commander Color Identity Changes
**What goes wrong:** User sets commander, search results still show cards outside commander's color identity.
**Why it happens:** Search query doesn't include color identity filter; TanStack Query cache serves stale results.
**How to avoid:** When commander is set, update the search params to include color identity filter. Use the `color` query param on the card search endpoint. Invalidate search cache when commander changes.
**Warning signs:** User can add cards outside commander's color identity.

### Pitfall 6: Race Condition on Debounced Save
**What goes wrong:** User adds card, immediately navigates back to deck list. Debounced save hasn't fired yet; changes are lost.
**Why it happens:** Navigation cancels pending debounced callbacks.
**How to avoid:** Flush pending save on navigation away. Use `beforeunload` event or flush in the back button handler.
**Warning signs:** Last few card additions before navigating away are lost.

## Code Examples

### Backend: Extend DeckHandler.update() for Commander
```java
// In DeckHandler.update(), add after sideboard handling:
final Map<String, Integer> commanderCards = (Map<String, Integer>) body.get("commander");
if (commanderCards != null) {
    deck.getOrCreate(DeckSection.Commander).clear();
    for (final Map.Entry<String, Integer> entry : commanderCards.entrySet()) {
        final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(entry.getKey());
        if (card != null) {
            deck.getOrCreate(DeckSection.Commander).add(card, entry.getValue());
        }
    }
}
```

### Backend: Format Validation Endpoint
```java
// New endpoint: GET /api/decks/{name}/validate?format=Commander
public static void validate(final Context ctx) {
    final String name = ctx.pathParam("name");
    final String formatName = ctx.queryParam("format");
    // Load deck, get GameFormat, test each card against getFilterRules()
    // Return { legal: boolean, illegalCards: [{name, reason}] }
}
```

### Frontend: updateDeck API Function
```typescript
// api/decks.ts -- add this
export interface UpdateDeckPayload {
  main: Record<string, number>
  sideboard: Record<string, number>
  commander: Record<string, number>
}

export async function updateDeck(name: string, payload: UpdateDeckPayload): Promise<DeckDetail> {
  return fetchApi<DeckDetail>(`/api/decks/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
```

### Frontend: Deck Stats Pure Functions
```typescript
// lib/deck-stats.ts
import type { DeckCardEntry } from '../types/deck'

export function computeManaCurve(cards: DeckCardEntry[], cardData: Map<string, CardSearchResult>): number[] {
  const curve = new Array(8).fill(0) // 0, 1, 2, 3, 4, 5, 6, 7+
  for (const entry of cards) {
    const card = cardData.get(entry.name)
    if (!card || card.typeLine.includes('Land')) continue
    const bucket = Math.min(card.cmc, 7)
    curve[bucket] += entry.quantity
  }
  return curve
}

export function computeColorDistribution(cards: DeckCardEntry[], cardData: Map<string, CardSearchResult>): Record<string, number> {
  const colors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0, C: 0 }
  for (const entry of cards) {
    const card = cardData.get(entry.name)
    if (!card) continue
    if (card.colors.length === 0) {
      colors.C += entry.quantity
    } else {
      for (const c of card.colors) {
        colors[c] += entry.quantity
      }
    }
  }
  return colors
}
```

### Frontend: Card Hover Preview
```typescript
// components/deck-editor/CardHoverPreview.tsx
function CardHoverPreview({ card, mousePos }: { card: CardSearchResult | null, mousePos: { x: number, y: number } }) {
  if (!card) return null

  // Position near cursor, flip side if near viewport edge
  const left = mousePos.x + 20
  const top = mousePos.y - 100
  const flipX = left + 300 > window.innerWidth

  return (
    <div
      className="fixed z-50 pointer-events-none"
      style={{
        left: flipX ? mousePos.x - 280 : left,
        top: Math.max(10, Math.min(top, window.innerHeight - 420)),
      }}
    >
      <img
        src={getScryfallImageUrl(card.setCode, card.collectorNumber, 'large')}
        alt={card.name}
        className="w-[260px] rounded-lg shadow-2xl"
      />
    </div>
  )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Recharts/Chart.js for all charts | Custom SVG for simple charts, libraries for complex | 2024+ | Fewer dependencies, smaller bundle for simple use cases |
| react-router for all routing | State-based for simple apps, router for complex | Always true | Less overhead for 2-3 view apps |
| Inline mana symbol images | mana-font CSS font | 2018+ | Single font file covers all symbols, scales perfectly |

**Deprecated/outdated:**
- react-router v5 patterns (useHistory, Switch): use v7 if adopting router
- Recharts v1 patterns: v3 current but unnecessary for this use case

## Open Questions

1. **Forge ManaCost.toString() format**
   - What we know: CardSearchDto uses `rules.getManaCost().toString()` to populate manaCost field
   - What's unclear: Whether output is `{W}{U}` braced format or `W U` space-separated or `WU` concatenated
   - Recommendation: Quick test by hitting `/api/cards?q=Lightning+Bolt` and inspecting manaCost field. Build parser to handle both formats defensively.

2. **Deck metadata storage for format**
   - What we know: Forge Deck class stores card sections, name, and can be serialized to .dck files
   - What's unclear: Whether .dck format supports arbitrary metadata like "format=Commander"
   - Recommendation: Check Deck.getComment()/setComment() or Deck.getTags(). If not supported, store format in the deck name convention or a companion metadata file. Alternatively, add a `format` field to the DeckHandler create/update and store it in a sidecar JSON file alongside the .dck.

3. **DeckCardEntry lacks card metadata for stats**
   - What we know: DeckDetail returns DeckCardEntry with name, quantity, setCode, collectorNumber -- no CMC, colors, or typeLine
   - What's unclear: How to compute stats without re-fetching card data
   - Recommendation: Either (a) enrich DeckDetailDto to include CMC/colors/typeLine per card entry, or (b) fetch card details client-side using the card search endpoint. Option (a) is cleaner -- extend DeckDetailDto.DeckCardEntry with these fields from PaperCard.getRules().

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None installed -- no test framework in frontend package.json |
| Config file | None |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DECK-01 | Card search integration in editor layout | manual | Visual verification of search panel in split layout | N/A |
| DECK-03 | Create, save, load, delete decks | integration | Backend test via HTTP | Needs backend test |
| DECK-04 | Grouped text list view | unit | Test card grouping logic | Wave 0 |
| DECK-05 | Visual card grid view | manual | Visual verification | N/A |
| DECK-06 | Add/remove cards with qty controls | unit | Test deck state manipulation functions | Wave 0 |
| DECK-07 | Mana curve chart | unit | Test computeManaCurve pure function | Wave 0 |
| DECK-08 | Card type distribution | unit | Test type extraction and counting | Wave 0 |
| DECK-09 | Color distribution | unit | Test computeColorDistribution | Wave 0 |
| DECK-10 | Format validation results | integration | Backend validation endpoint test | Needs backend test |
| DECK-11 | Sideboard management | unit | Test sideboard state manipulation | Wave 0 |
| DECK-12 | Commander selection | unit | Test commander state management | Wave 0 |
| DECK-13 | Basic land quantity controls | unit | Test land addition logic | Wave 0 |

### Sampling Rate
- **Per task commit:** Visual verification in browser (no automated frontend tests)
- **Per wave merge:** Backend endpoint tests via curl/httpie
- **Phase gate:** Manual walkthrough of all 12 requirements

### Wave 0 Gaps
- [ ] No frontend test framework installed (vitest recommended for Vite projects)
- [ ] No test files exist for any frontend code
- [ ] Backend DeckHandler tests may exist from Phase 2 -- check `forge-gui-web/src/test/`
- [ ] Pure logic functions (deck-stats.ts, deck-grouping.ts, mana.ts) are testable without DOM -- prioritize these if adding tests

*(Note: Given the project velocity and manual verification pattern from Phases 1-2, frontend test infrastructure is a Wave 0 gap but may be intentionally deferred by the project. Backend tests for new endpoints are higher priority.)*

## Sources

### Primary (HIGH confidence)
- Codebase inspection of all files listed in canonical_refs section of CONTEXT.md
- DeckHandler.java -- confirmed update() only handles main/sideboard, not commander
- DeckDetailDto.java -- confirmed commander section is read from DeckSection.Commander but not written in update
- GameFormat.java -- confirmed getFilterRules(), getDeckConformanceProblem() for validation
- WebServer.java -- confirmed route registration, PUT /api/decks/{name} already wired
- CardSearchDto.java -- confirmed manaCost field comes from ManaCost.toString()
- package.json -- confirmed no test framework, current dependency versions

### Secondary (MEDIUM confidence)
- [mana-font by Andrew Gioia](https://mana.andrewgioia.com/) -- de facto standard for MTG web mana symbols
- [Recharts GitHub](https://github.com/recharts/recharts) -- v3.8.0 current, but overkill for this use case
- Custom SVG recommendation based on data scale (<10 data points per chart)

### Tertiary (LOW confidence)
- ManaCost.toString() output format -- needs runtime verification against actual API response

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all core libraries already installed and verified in package.json
- Architecture: HIGH -- based on thorough inspection of existing codebase patterns
- Pitfalls: HIGH -- identified from actual code gaps (missing commander update, missing format field, missing card metadata in DeckCardEntry)
- Charts: HIGH -- custom SVG is clearly correct for <10 data points
- Mana symbols: MEDIUM -- mana-font is standard but npm package name/availability needs verification

**Research date:** 2026-03-19
**Valid until:** 2026-04-19 (stable domain, no fast-moving dependencies)
