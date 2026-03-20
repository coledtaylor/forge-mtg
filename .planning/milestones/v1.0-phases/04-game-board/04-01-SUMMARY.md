---
phase: 04-game-board
plan: 01
subsystem: ui
tags: [zustand, immer, websocket, typescript, scryfall, react]

requires:
  - phase: 01-engine-bridge
    provides: WebSocket protocol with typed DTOs, game WebSocket endpoint
  - phase: 02-rest-api-frontend-scaffold
    provides: Frontend scaffold, CardImage component, Scryfall integration

provides:
  - TypeScript DTO types matching all Java backend game DTOs
  - Zustand game store with immer middleware for reactive state management
  - WebSocket manager class with reconnect and full message dispatch
  - useGameWebSocket React hook with StrictMode-safe lifecycle
  - GameCardImage component for name-based Scryfall card rendering
  - Game view routing in App.tsx

affects: [04-game-board]

tech-stack:
  added: [zustand 5.0.12, immer 11.1.4]
  patterns: [zustand-immer store, websocket-manager-class, name-based-scryfall-image]

key-files:
  created:
    - forge-gui-web/frontend/src/lib/gameTypes.ts
    - forge-gui-web/frontend/src/stores/gameStore.ts
    - forge-gui-web/frontend/src/lib/gameWebSocket.ts
    - forge-gui-web/frontend/src/hooks/useGameWebSocket.ts
    - forge-gui-web/frontend/src/components/game/GameCardImage.tsx
    - forge-gui-web/frontend/src/components/game/GameBoard.tsx
  modified:
    - forge-gui-web/frontend/src/App.tsx
    - forge-gui-web/frontend/package.json

key-decisions:
  - "Record<number, T> for cards/players instead of Map to avoid immer Map serialization pitfall"
  - "Scryfall name-based exact match URL for game card images (CardDto lacks setCode/collectorNumber)"
  - "useRef guard in useGameWebSocket to prevent StrictMode double-mount WebSocket connections"
  - "Protocol-aware WebSocket URL (wss: for HTTPS, ws: for HTTP)"

patterns-established:
  - "Zustand + immer store pattern: create<State & Actions>()(immer((set) => ({...})))"
  - "WebSocket manager class (not hook) dispatching to store via useGameStore.getState()"
  - "Zone membership derived from cards' zoneType field, not from zone notification payloads"

requirements-completed: [GAME-01, GAME-02, GAME-03, GAME-04, GAME-05, GAME-06, GAME-07, GAME-08, GAME-09, GAME-11]

duration: 3min
completed: 2026-03-19
---

# Phase 4 Plan 01: Game Data Foundation Summary

**Zustand game store with immer, WebSocket manager with reconnect, typed DTOs matching Java backend, and Scryfall name-based card image component**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-19T23:23:28Z
- **Completed:** 2026-03-19T23:26:08Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Complete TypeScript type definitions for all game DTOs (GameStateDto, CardDto, PlayerDto, CombatDto, SpellAbilityDto, ZoneUpdateDto, message types, prompts)
- Zustand store with 12 actions handling full game state snapshots, incremental updates, prompts, buttons, and connection status
- WebSocket manager class with exponential backoff reconnect (1s/2s/4s), full message type dispatch, and typed send helpers
- GameCardImage component using Scryfall name-based exact match API with loading skeleton and error fallback
- App.tsx extended with game view type for game board routing

## Task Commits

Each task was committed atomically:

1. **Task 1: Install deps, create game types, Zustand store, and WebSocket manager** - `18832a00a1` (feat)
2. **Task 2: Create GameCardImage wrapper and add game route to App.tsx** - `e8c14a301f` (feat)

## Files Created/Modified
- `src/lib/gameTypes.ts` - All TypeScript types matching Java backend DTOs
- `src/stores/gameStore.ts` - Zustand store with immer middleware, 12 game state actions
- `src/lib/gameWebSocket.ts` - WebSocket manager class with connect/reconnect/send
- `src/hooks/useGameWebSocket.ts` - React hook wrapping WebSocket lifecycle with StrictMode guard
- `src/components/game/GameCardImage.tsx` - Card image component using Scryfall name-based URL
- `src/components/game/GameBoard.tsx` - Placeholder game board component
- `src/App.tsx` - Extended View type with game variant, added GameBoard routing
- `package.json` - Added zustand and immer dependencies

## Decisions Made
- Used `Record<number, T>` instead of `Map` for cards and players to avoid immer Map serialization pitfall (Pitfall 4 from RESEARCH.md)
- Used Scryfall exact name match API (`/cards/named?exact=`) for game card images since CardDto lacks setCode/collectorNumber
- Added `useRef` guard in `useGameWebSocket` to prevent StrictMode double-mount creating duplicate WebSocket connections
- Made WebSocket URL protocol-aware (wss: for HTTPS, ws: for HTTP) for production readiness

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Pre-existing TypeScript errors in DeckList.tsx and CardSearchPanel.tsx (unrelated to this plan's changes) - out of scope, not fixed

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Game data layer complete, ready for UI components in Plan 02 (GameBoard, PlayerInfoBar, PhaseStrip, zones)
- All DTO types available for component props
- WebSocket manager ready for game connection
- Game route in App.tsx ready to render real GameBoard component

## Self-Check: PASSED

All 6 created files verified on disk. Both task commits (18832a00a1, e8c14a301f) verified in git log. TypeScript compiles cleanly for all new files.

---
*Phase: 04-game-board*
*Completed: 2026-03-19*
