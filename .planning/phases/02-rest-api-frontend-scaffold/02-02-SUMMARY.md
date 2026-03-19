---
phase: 02-rest-api-frontend-scaffold
plan: 02
subsystem: ui
tags: [react, vite, shadcn-ui, tanstack-query, tailwindcss, scryfall, typescript]

# Dependency graph
requires:
  - phase: 02-rest-api-frontend-scaffold/01
    provides: "REST API endpoints for card search and deck CRUD"
provides:
  - "Vite + React + shadcn/ui frontend scaffold in forge-gui-web/frontend/"
  - "Card search UI with Scryfall image display and pagination"
  - "Deck list with create/delete CRUD"
  - "Dark theme with shadcn/ui components"
  - "TanStack Query hooks for API data fetching"
  - "Scryfall image URL construction helper"
affects: [03-deck-builder, 04-game-board]

# Tech tracking
tech-stack:
  added: [react, vite, tailwindcss, shadcn-ui, tanstack-react-query, typescript]
  patterns: [api-client-fetch-wrapper, tanstack-query-hooks, scryfall-image-redirect, component-composition]

key-files:
  created:
    - forge-gui-web/frontend/package.json
    - forge-gui-web/frontend/vite.config.ts
    - forge-gui-web/frontend/src/App.tsx
    - forge-gui-web/frontend/src/types/card.ts
    - forge-gui-web/frontend/src/types/deck.ts
    - forge-gui-web/frontend/src/lib/scryfall.ts
    - forge-gui-web/frontend/src/api/client.ts
    - forge-gui-web/frontend/src/api/cards.ts
    - forge-gui-web/frontend/src/api/decks.ts
    - forge-gui-web/frontend/src/hooks/useCardSearch.ts
    - forge-gui-web/frontend/src/hooks/useDecks.ts
    - forge-gui-web/frontend/src/components/CardImage.tsx
    - forge-gui-web/frontend/src/components/CardGrid.tsx
    - forge-gui-web/frontend/src/components/SearchBar.tsx
    - forge-gui-web/frontend/src/components/PaginationBar.tsx
    - forge-gui-web/frontend/src/components/DeckList.tsx
  modified:
    - forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java

key-decisions:
  - "Removed keepPreviousData from useCardSearch to prevent stale results persisting across searches"
  - "Used getUniqueCardsNoAlt() instead of getUniqueCards() to eliminate DFC/adventure/alchemy duplicate entries"

patterns-established:
  - "API client pattern: fetchApi<T> wrapper with error handling in api/client.ts"
  - "Hook pattern: TanStack Query hooks in hooks/ directory wrapping API calls"
  - "Component pattern: UI components in components/ using shadcn/ui primitives from components/ui/"
  - "Scryfall image pattern: getScryfallImageUrl(setCode, collectorNumber) with lazy loading and text fallback"

requirements-completed: [DECK-02]

# Metrics
duration: 35min
completed: 2026-03-19
---

# Phase 2 Plan 02: Frontend Scaffold Summary

**React + shadcn/ui frontend with Scryfall card images, search filters, pagination, and deck CRUD via TanStack Query**

## Performance

- **Duration:** ~35 min (across checkpoint pause)
- **Started:** 2026-03-19T15:35:00Z
- **Completed:** 2026-03-19T16:10:00Z
- **Tasks:** 3
- **Files modified:** 15

## Accomplishments
- Complete Vite + React + TypeScript project scaffold with shadcn/ui dark theme
- Card search with filters (name, color, type, format, CMC) and paginated results displaying Scryfall images
- Deck list with create/delete CRUD and confirmation dialogs
- TanStack Query hooks providing cached data fetching with 5-minute stale time
- Scryfall image component with lazy loading, skeleton placeholders, and styled text fallback

## Task Commits

Each task was committed atomically:

1. **Task 1: Initialize Vite + React + shadcn/ui project** - `894be72766` (feat)
2. **Task 2: Build TypeScript types, API client, hooks, and all UI components** - `481560ecb4` (feat)
3. **Task 3: Verify frontend renders with card images and search** - `41207aeb00` (fix - bug fixes from verification)

## Files Created/Modified
- `forge-gui-web/frontend/package.json` - React + Vite + TanStack Query + Tailwind project config
- `forge-gui-web/frontend/vite.config.ts` - Vite config with proxy for /api and /ws to localhost:8080
- `forge-gui-web/frontend/src/App.tsx` - Main app layout composing all components with QueryClientProvider
- `forge-gui-web/frontend/src/types/card.ts` - CardSearchResult, CardSearchResponse, CardSearchParams types
- `forge-gui-web/frontend/src/types/deck.ts` - DeckSummary, DeckDetail, DeckCardEntry types
- `forge-gui-web/frontend/src/lib/scryfall.ts` - Scryfall image URL construction helper
- `forge-gui-web/frontend/src/api/client.ts` - Generic fetchApi wrapper with error handling
- `forge-gui-web/frontend/src/api/cards.ts` - Card search API client
- `forge-gui-web/frontend/src/api/decks.ts` - Deck CRUD API client
- `forge-gui-web/frontend/src/hooks/useCardSearch.ts` - TanStack Query hook for card search
- `forge-gui-web/frontend/src/hooks/useDecks.ts` - TanStack Query hooks for deck list, create, delete
- `forge-gui-web/frontend/src/components/CardImage.tsx` - Scryfall image with fallback
- `forge-gui-web/frontend/src/components/CardGrid.tsx` - Responsive card grid layout
- `forge-gui-web/frontend/src/components/SearchBar.tsx` - Search input with filter controls
- `forge-gui-web/frontend/src/components/PaginationBar.tsx` - Page navigation
- `forge-gui-web/frontend/src/components/DeckList.tsx` - Deck list with create/delete
- `forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java` - Fixed duplicate card entries

## Decisions Made
- Removed `keepPreviousData` from useCardSearch -- it caused stale results from previous searches to persist, confusing the user when switching between different search terms
- Changed `getUniqueCards()` to `getUniqueCardsNoAlt()` in CardSearchHandler -- the former returned DFC back faces, adventure halves, and alchemy rebalanced variants as separate entries, bloating results with duplicates

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] DFC/adventure/alchemy duplicate card entries in search results**
- **Found during:** Task 3 (human verification)
- **Issue:** `getUniqueCards()` returned both faces of DFCs, adventure halves, and alchemy variants as separate cards
- **Fix:** Changed to `getUniqueCardsNoAlt()` which filters alternate printings
- **Files modified:** forge-gui-web/src/main/java/forge/web/api/CardSearchHandler.java
- **Verification:** User confirmed duplicates eliminated
- **Committed in:** 41207aeb00

**2. [Rule 1 - Bug] Stale search data persisting across searches**
- **Found during:** Task 3 (human verification)
- **Issue:** `keepPreviousData` in TanStack Query caused previous search results to remain visible while new search loaded, creating confusion
- **Fix:** Removed `keepPreviousData` option from useCardSearch hook
- **Files modified:** forge-gui-web/frontend/src/hooks/useCardSearch.ts
- **Verification:** User confirmed clean transitions between searches
- **Committed in:** 41207aeb00

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correct UX behavior. No scope creep.

## Issues Encountered
None beyond the two bugs fixed during verification.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Frontend scaffold complete, ready for Phase 3 (deck builder) to add deck editing UI
- Component patterns established for Phase 4 (game board) to follow
- Scryfall image infrastructure shared across both Phase 3 and Phase 4

## Self-Check: PASSED

All key files verified present. All 3 task commits verified in git log.

---
*Phase: 02-rest-api-frontend-scaffold*
*Completed: 2026-03-19*
