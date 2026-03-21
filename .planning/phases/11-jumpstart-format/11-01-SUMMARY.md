---
phase: 11-jumpstart-format
plan: 01
subsystem: api
tags: [jumpstart, rest-api, react-query, dto, sealed-template]

# Dependency graph
requires:
  - phase: 07-game-lobby
    provides: GameStartConfig type, WebServer route registration pattern
provides:
  - GET /api/jumpstart/packs endpoint returning pack summaries from JMP and J22 sets
  - JumpstartPack TypeScript interface matching backend DTO
  - fetchJumpstartPacks API function
  - useJumpstartPacks query hook with Infinity staleTime
  - GameStartConfig extended with optional pack1/pack2 fields
affects: [11-jumpstart-format]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "StaticData.getSpecialBoosters() iteration filtered by set code prefix"
    - "UnOpenedProduct for deterministic wholeSheet pack opening"
    - "Immutable DTO with constructor + getters (no Jackson default constructor)"

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/dto/JumpstartPackDto.java
    - forge-gui-web/src/main/java/forge/web/api/JumpstartHandler.java
    - forge-gui-web/frontend/src/types/jumpstart.ts
    - forge-gui-web/frontend/src/api/jumpstart.ts
    - forge-gui-web/frontend/src/hooks/useJumpstartPacks.ts
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/frontend/src/types/game.ts

key-decisions:
  - "JumpstartPackDto uses immutable constructor pattern (no default constructor) unlike DeckSummaryDto"
  - "Color identity computed per-card via ColorSet.hasWhite/Blue/Black/Red/Green matching DeckSummaryDto pattern"
  - "Pack list sorted by theme then setCode for consistent frontend display order"

patterns-established:
  - "JumpstartHandler: static utility class with Context-based handlers for Jumpstart domain"

requirements-completed: [JUMP-02, JUMP-04]

# Metrics
duration: 3min
completed: 2026-03-21
---

# Phase 11 Plan 01: Jumpstart Pack API Summary

**REST endpoint listing 242 JMP/J22 pack variants with theme, colors, and card count, plus TypeScript types and react-query hook**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T03:54:28Z
- **Completed:** 2026-03-21T03:57:44Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Backend endpoint iterates StaticData specialBoosters, filters JMP/J22, opens each pack via UnOpenedProduct, computes color identity, returns sorted DTO list
- Frontend type contracts (JumpstartPack interface), API function, and cached query hook ready for lobby and deck builder consumption
- GameStartConfig extended with optional pack1/pack2 fields for Jumpstart game setup

## Task Commits

Each task was committed atomically:

1. **Task 1: Backend Jumpstart pack API endpoint** - `2867e3c2eb` (feat)
2. **Task 2: Frontend types, API layer, and GameStartConfig extension** - `ff4ea2c618` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/dto/JumpstartPackDto.java` - Immutable DTO with id, theme, setCode, cardCount, colors
- `forge-gui-web/src/main/java/forge/web/api/JumpstartHandler.java` - GET /api/jumpstart/packs handler
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Route registration for jumpstart endpoint
- `forge-gui-web/frontend/src/types/jumpstart.ts` - JumpstartPack TypeScript interface
- `forge-gui-web/frontend/src/types/game.ts` - Added pack1/pack2 optional fields to GameStartConfig
- `forge-gui-web/frontend/src/api/jumpstart.ts` - fetchJumpstartPacks API function
- `forge-gui-web/frontend/src/hooks/useJumpstartPacks.ts` - useJumpstartPacks hook with Infinity staleTime

## Decisions Made
- JumpstartPackDto uses immutable constructor pattern (final fields + getters, no default constructor) since it is write-once from handler. Jackson serializes via getters.
- Color identity computed using the same ColorSet has-color pattern established in DeckSummaryDto for consistency
- Pack list sorted by theme then setCode for predictable display ordering in the frontend

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Pack API ready for GameLobby dual-pack picker (Plan 02)
- GameStartConfig pack1/pack2 fields ready for WebServer merge logic (Plan 03)
- useJumpstartPacks hook ready for PackPicker component consumption

## Self-Check: PASSED

All 5 created files verified on disk. Both task commits (2867e3c2eb, ff4ea2c618) verified in git log.

---
*Phase: 11-jumpstart-format*
*Completed: 2026-03-21*
