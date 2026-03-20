---
phase: 03-deck-builder
plan: 01
subsystem: api, ui
tags: [javalin, react, tanstack-query, mana-font, deck-builder, format-validation]

requires:
  - phase: 02-rest-api-frontend-scaffold
    provides: "REST API framework, deck CRUD endpoints, frontend scaffold with TanStack Query"
provides:
  - "Enriched DeckCardEntry with manaCost, typeLine, cmc, colors"
  - "Format field on deck create and summary"
  - "Commander section update support in PUT /api/decks/{name}"
  - "Format validation endpoint GET /api/decks/{name}/validate?format=X"
  - "Frontend updateDeck and validateDeck API functions"
  - "useDeckEditor hook with optimistic local state and debounced save"
  - "useCardHover hook for card preview positioning"
  - "Mana cost parser and mana-font CSS class mapper"
  - "Deck stats functions (manaCurve, colorDistribution, typeBreakdown, averageCMC)"
  - "Card type grouping logic with priority ordering"
affects: [03-deck-builder]

tech-stack:
  added: [mana-font]
  patterns: [enriched-dto, debounced-save, optimistic-local-state, pure-utility-functions]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java
    - forge-gui-web/frontend/src/hooks/useDeckEditor.ts
    - forge-gui-web/frontend/src/hooks/useCardHover.ts
    - forge-gui-web/frontend/src/lib/mana.ts
    - forge-gui-web/frontend/src/lib/deck-stats.ts
    - forge-gui-web/frontend/src/lib/deck-grouping.ts
  modified:
    - forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java
    - forge-gui-web/src/main/java/forge/web/dto/DeckSummaryDto.java
    - forge-gui-web/src/main/java/forge/web/api/DeckHandler.java
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/frontend/src/types/deck.ts
    - forge-gui-web/frontend/src/api/decks.ts
    - forge-gui-web/frontend/src/hooks/useDecks.ts
    - forge-gui-web/frontend/src/components/DeckList.tsx

key-decisions:
  - "Validate route registered before generic {name} route to prevent path collision"
  - "useDeckEditor uses 1-second debounced save with flushSave for explicit save"
  - "DeckCardEntry enrichment reuses CardRules pattern from CardSearchDto"

patterns-established:
  - "Enriched DTO pattern: DeckCardEntry carries card metadata from backend for frontend stat computation"
  - "Optimistic local state: useDeckEditor maintains local copy, syncs debounced to server"
  - "Pure utility pattern: deck-stats.ts and deck-grouping.ts are pure functions with no side effects"

requirements-completed: [DECK-03, DECK-06, DECK-10, DECK-12]

duration: 5min
completed: 2026-03-19
---

# Phase 3 Plan 1: Deck Builder Data Layer Summary

**Backend deck API extensions (commander, format, enriched cards, validation) and frontend data layer (hooks, mana parser, stats, grouping)**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-19T21:31:37Z
- **Completed:** 2026-03-19T21:36:24Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- Extended backend DeckCardEntry with manaCost, typeLine, cmc, colors fields populated from CardRules
- Added format validation endpoint that checks deck legality against Forge GameFormat rules
- Built complete frontend data layer: API functions, hooks (useDeckEditor, useCardHover), and pure utility libraries (mana parser, deck stats, card grouping)

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend backend** - `5244b52234` (feat)
2. **Task 2: Frontend data layer** - `e6aaacfc96` (feat)

## Files Created/Modified
- `FormatValidationHandler.java` - Format validation endpoint with filter rules and conformance checking
- `DeckDetailDto.java` - Enriched DeckCardEntry with card metadata fields
- `DeckSummaryDto.java` - Added format field from deck comment
- `DeckHandler.java` - Commander section update, format on create
- `WebServer.java` - Registered validate route
- `types/deck.ts` - Extended types with enriched fields, new payload/result types
- `api/decks.ts` - Added updateDeck, validateDeck; updated createDeck signature
- `hooks/useDecks.ts` - Added useDeck, useUpdateDeck, useValidateDeck hooks
- `hooks/useDeckEditor.ts` - Central deck editing hook with optimistic state and debounced save
- `hooks/useCardHover.ts` - Card preview hover positioning hook
- `lib/mana.ts` - Mana cost string parser and mana-font CSS class mapper
- `lib/deck-stats.ts` - Pure stat functions: manaCurve, colorDistribution, typeBreakdown, averageCMC, deckColors
- `lib/deck-grouping.ts` - Card type grouping with priority ordering

## Decisions Made
- Validate route registered before generic `{name}` route to prevent Javalin path collision
- useDeckEditor uses 1-second debounced save with explicit flushSave callback
- DeckCardEntry enrichment reuses same CardRules pattern from CardSearchDto for consistency
- Updated DeckList.tsx call site to pass CreateDeckPayload instead of plain string (Rule 1 - breaking change fix)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated DeckList.tsx call site for createDeck signature change**
- **Found during:** Task 2 (Frontend data layer)
- **Issue:** createDeck changed from `(name: string)` to `(payload: CreateDeckPayload)`, breaking existing call in DeckList.tsx
- **Fix:** Changed `createDeck.mutate(name)` to `createDeck.mutate({ name, format: '' })`
- **Files modified:** forge-gui-web/frontend/src/components/DeckList.tsx
- **Verification:** `npx tsc --noEmit` passes
- **Committed in:** e6aaacfc96 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Essential fix for type safety. No scope creep.

## Issues Encountered
- Maven parent POM needed `mvn install -N` before module compilation -- standard setup, resolved immediately.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All data contracts and business logic established for Plans 02 and 03
- Plans 02/03 can build UI components purely against local types and hooks
- No backend changes needed for deck builder UI components

---
*Phase: 03-deck-builder*
*Completed: 2026-03-19*
