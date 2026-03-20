---
phase: 07-backend-dto-enrichment-tech-debt
plan: 02
subsystem: api, ui
tags: [javalin, format-validation, typescript, types]

requires:
  - phase: 04-game-loop
    provides: FormatValidationHandler, GameLobby, useGameWebSocket
provides:
  - "Format validation for Casual 60-card (60+ cards, 4-of limit) and Jumpstart (20 cards)"
  - "Graceful unknown format handling (200 instead of 400)"
  - "Canonical GameStartConfig interface in src/types/game.ts"
affects: [07-backend-dto-enrichment-tech-debt]

tech-stack:
  added: []
  patterns: [custom-format-validation-before-engine-lookup, shared-type-with-re-exports]

key-files:
  created:
    - forge-gui-web/frontend/src/types/game.ts
  modified:
    - forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java
    - forge-gui-web/frontend/src/hooks/useGameWebSocket.ts
    - forge-gui-web/frontend/src/components/lobby/GameLobby.tsx
    - forge-gui-web/frontend/src/components/game/GameBoard.tsx
    - forge-gui-web/frontend/src/App.tsx

key-decisions:
  - "Custom format handlers run before FModel.getFormats().get() to intercept non-engine formats"
  - "Re-exports from original locations preserve backwards compatibility for GameStartConfig"

patterns-established:
  - "Custom format validation: intercept format name before engine lookup, return early with manual checks"
  - "Shared TypeScript types: canonical definition in src/types/, re-export from original locations"

requirements-completed: [DEBT-01, DEBT-02]

duration: 3min
completed: 2026-03-20
---

# Phase 7 Plan 02: Format Validation & GameStartConfig Consolidation Summary

**Custom format validation for Casual 60-card (deck size + 4-of) and Jumpstart (20-card pack), plus GameStartConfig consolidated to single canonical types/game.ts**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T21:29:42Z
- **Completed:** 2026-03-20T21:33:11Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Format validation handles "Casual 60-card" with 60-card minimum and 4-of limit (basic lands exempt)
- Format validation handles "Jumpstart" with exactly 20-card requirement
- Unknown formats return 200 with legal=true instead of 400 error
- GameStartConfig interface lives in one place with re-exports for backwards compatibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix format validation for Casual 60-card, Jumpstart, and unknown formats** - `9454119a51` (fix)
2. **Task 2: Consolidate GameStartConfig into shared types/game.ts** - `45dc13e9b5` (refactor)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/api/FormatValidationHandler.java` - Custom format handling before engine lookup
- `forge-gui-web/frontend/src/types/game.ts` - Canonical GameStartConfig interface (new)
- `forge-gui-web/frontend/src/hooks/useGameWebSocket.ts` - Import from types/game, re-export
- `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` - Import from types/game, re-export
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` - Import from types/game
- `forge-gui-web/frontend/src/App.tsx` - Import from types/game

## Decisions Made
- Custom format handlers run before `FModel.getFormats().get()` to intercept non-engine formats
- Re-exports from original locations (useGameWebSocket, GameLobby) preserve backwards compatibility
- Basic lands exempt from 4-of limit in Casual 60-card validation

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Maven cached dependency resolution failure required `-U` flag to clear; stale class files required `-am` flag to rebuild dependencies. Both resolved without code changes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Format validation is complete for all four supported formats
- TypeScript types are clean with single source of truth
- Ready for remaining Phase 7 plans (CardDto enrichment, AI deck bundling)

---
*Phase: 07-backend-dto-enrichment-tech-debt*
*Completed: 2026-03-20*
