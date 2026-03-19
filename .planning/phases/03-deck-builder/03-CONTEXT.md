# Phase 3: Deck Builder - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Full deck building experience: search cards, add/remove with quantity controls, view as grouped text list or visual grid, see statistics (mana curve, color distribution, type breakdown), validate against format rules, manage sideboard, set commander for Commander format, quickly add basic lands. Everything runs in-browser against the existing REST API.

</domain>

<decisions>
## Implementation Decisions

### Editor Layout
- Two-column split layout: left panel = card search with results grid, right panel = deck contents + stats
- Fixed 50/50 split ratio (min viewport 1280px, so ~640px each side)
- Hover on any card (search results or deck list) shows enlarged card image floating near cursor (Scryfall-style hover preview)
- Click a search result card to add 1 copy to deck (click again for more copies)

### Deck View Modes
- Default view: grouped text list organized by card type (Creatures, Instants, Sorceries, Enchantments, Artifacts, Planeswalkers, Lands)
- Toggle button (list/grid icon) in deck panel header to switch between grouped text list and visual card grid
- Each card row in text list shows: quantity + name + mana cost symbols (e.g., "4x Lightning Bolt {R}")
- +/- buttons on each card row for quantity control; reaching 0 removes the card

### Statistics & Charts
- Right panel has tab toggle: "Deck" | "Stats"
- Deck tab includes mini stats summary: small mana curve bar chart + total card count + color dots (~80px height)
- Stats tab is full-width with detailed charts:
  - Mana curve histogram (vertical bars for CMC 0, 1, 2, 3, 4, 5, 6, 7+)
  - Color distribution pie/donut chart (W/U/B/R/G/colorless proportions)
  - Card type breakdown chart (creatures, instants, sorceries, enchantments, artifacts, planeswalkers, lands)
  - Deck summary panel: total cards, average CMC, land count, color identity, format legality status

### Format & Validation
- Format selected at deck creation time (fixed for that deck)
- Format validation results shown as inline red badges/icons on illegal cards in deck list, with tooltip explaining why
- Validation summary also appears in the stats tab deck summary panel

### Commander
- Commander format decks get a dedicated commander slot displayed prominently at top of deck panel
- Click a legendary creature in the deck to set it as commander
- Commander's color identity filters search results (only show cards within commander's color identity)

### Basic Lands
- Inline land bar at bottom of deck panel: Plains/Island/Swamp/Mountain/Forest icons with +/- buttons each
- Always visible when editing a deck

### Claude's Discretion
- Chart library choice (recharts, chart.js, or lightweight custom SVG)
- Exact mana cost symbol rendering approach
- Transition animations between views
- Error handling for API failures
- Sideboard UI specifics (likely a tab or collapsible section within deck panel)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing API & Types
- `forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java` -- Card search endpoint with 5 filters + pagination
- `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` -- Deck CRUD with main/sideboard update via PUT
- `forge-gui-web/frontend/src/types/deck.ts` -- DeckDetail/DeckSummary/DeckCardEntry types
- `forge-gui-web/frontend/src/types/card.ts` -- CardSearchResult/CardSearchParams types
- `forge-gui-web/frontend/src/api/decks.ts` -- Deck API client (list, create, get, delete -- note: no update function yet)
- `forge-gui-web/frontend/src/hooks/useDecks.ts` -- useDecks, useCreateDeck, useDeleteDeck hooks

### Existing Components
- `forge-gui-web/frontend/src/components/CardImage.tsx` -- Scryfall image with lazy loading + fallback
- `forge-gui-web/frontend/src/components/CardGrid.tsx` -- Responsive card image grid
- `forge-gui-web/frontend/src/components/SearchBar.tsx` -- Search with 5 filter dropdowns
- `forge-gui-web/frontend/src/components/PaginationBar.tsx` -- Page navigation
- `forge-gui-web/frontend/src/components/DeckList.tsx` -- Basic deck list with create/delete (will be replaced/expanded)

### Format Validation (Backend)
- `forge-game/src/main/java/forge/game/GameFormat.java` -- Forge's format validation system (getFilterRules)
- `forge-gui-web/frontend/src/components/ui/` -- shadcn/ui components: badge, button, card, dialog, input, select, skeleton

No external specs -- requirements fully captured in decisions above and REQUIREMENTS.md (DECK-01 through DECK-13).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CardImage` component: Scryfall hover preview can extend this with enlarged positioning logic
- `CardGrid`: Reusable for visual grid view of deck contents
- `SearchBar` + `useCardSearch`: Entire left panel search is already built -- just needs integration into split layout
- `DeckDetail` type: Already has main/sideboard/commander arrays -- maps directly to UI sections
- shadcn/ui `badge`: Can be used for format validation error badges on illegal cards
- shadcn/ui `dialog`: Available for deck creation dialog (name + format selection)

### Established Patterns
- TanStack Query for data fetching with `useQuery`/`useMutation` and queryClient invalidation
- Tailwind CSS for styling (dark theme with CSS variables)
- shadcn/ui component patterns (composition, variant props)
- Vite proxy for `/api` routes to backend on port 8080

### Integration Points
- `App.tsx` currently shows search + deck list vertically -- needs routing or conditional rendering for deck editor view
- Backend `DeckHandler.update()` accepts `{main: {cardName: qty}, sideboard: {cardName: qty}}` -- frontend needs updateDeck API function + hook
- Backend format validation via `GameFormat.getFilterRules()` -- may need new endpoint for per-card legality checking
- No frontend routing yet -- will need React Router or simple state-based navigation (deck list -> deck editor)

</code_context>

<specifics>
## Specific Ideas

- Hover preview like Scryfall's -- enlarged card image floating near cursor, not in a fixed panel
- Grouped text list like Moxfield's default deck view -- organized by card type with clean typography
- Mana curve as the primary mini stat on the deck tab -- it's the most-glanced stat while building

</specifics>

<deferred>
## Deferred Ideas

None -- discussion stayed within phase scope

</deferred>

---

*Phase: 03-deck-builder*
*Context gathered: 2026-03-19*
