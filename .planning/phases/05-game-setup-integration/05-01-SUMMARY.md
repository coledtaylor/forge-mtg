---
phase: 05-game-setup-integration
plan: 01
subsystem: ui, api
tags: [react, websocket, lobby, deck-selection, shadcn, base-ui]

# Dependency graph
requires:
  - phase: 04-game-board
    provides: GameBoard component, useGameWebSocket hook, GameWebSocket class
  - phase: 02-rest-api-frontend-scaffold
    provides: useDecks hook, DeckSummary type, shadcn components
provides:
  - Game lobby UI with format filtering, deck selection, AI configuration
  - Config payload in START_GAME WebSocket message (deckName, aiDeckName, format, aiDifficulty)
  - Backend deck resolution from name to Deck object with format filtering
  - AI difficulty-to-profile mapping (Easy/Medium/Hard -> Cautious/Default/Reckless)
  - Post-game return to lobby with preserved selections
affects: [05-02-play-this-deck]

# Tech tracking
tech-stack:
  added: []
  patterns: [lobby-view-routing, config-payload-websocket, format-based-deck-filtering]

key-files:
  created:
    - forge-gui-web/frontend/src/components/lobby/GameLobby.tsx
    - forge-gui-web/frontend/src/components/lobby/DeckPicker.tsx
    - forge-gui-web/frontend/src/components/lobby/AiSettings.tsx
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/frontend/src/lib/gameWebSocket.ts
    - forge-gui-web/frontend/src/hooks/useGameWebSocket.ts
    - forge-gui-web/frontend/src/components/game/GameBoard.tsx
    - forge-gui-web/frontend/src/App.tsx

key-decisions:
  - "GameConfig passed through View state -> GameBoard -> useGameWebSocket -> GameWebSocket.connect onopen, not created in lobby"
  - "DeckHandler.findDeckFile not exposed; duplicated loadDeckByName pattern in WebServer per RESEARCH advice"
  - "Format matching for Casual 60-card includes empty/Constructed deck comments for broad compatibility"

patterns-established:
  - "Lobby view routing: App.tsx View union includes lobby with preSelected props for return state"
  - "Config-through-WS: gameConfig stored in GameWebSocket instance, sent as START_GAME payload on onopen"

requirements-completed: [SETUP-01, SETUP-02, SETUP-03]

# Metrics
duration: 4min
completed: 2026-03-20
---

# Phase 5 Plan 1: Game Setup Lobby Summary

**Game lobby with format-filtered deck picker, AI difficulty/deck settings, and backend deck resolution via START_GAME config payload**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-20T14:17:54Z
- **Completed:** 2026-03-20T14:22:01Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- Backend handleStartGame accepts config payload with deckName/aiDeckName/format/aiDifficulty, resolves decks from .dck files, maps AI difficulty to profiles
- Lobby UI with format dropdown (4 options), filterable deck list with radio selection, collapsible AI settings panel, Start Game button with loading state
- Full round-trip: lobby -> game -> lobby with preserved deck/format selections via returnState

## Task Commits

Each task was committed atomically:

1. **Task 1: Backend START_GAME payload handling** - `cb6931efaa` (feat)
2. **Task 2: Lobby UI components + App.tsx routing** - `e0ad4476ef` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Config payload parsing, loadDeckByName, pickRandomDeck, AI profile mapping, format-to-GameType mapping
- `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` - Main lobby view with format selector, deck picker, AI settings, start button
- `forge-gui-web/frontend/src/components/lobby/DeckPicker.tsx` - Format-filtered deck list with radio-style selection, color dots, skeleton loading
- `forge-gui-web/frontend/src/components/lobby/AiSettings.tsx` - Collapsible AI config panel with difficulty and deck dropdowns
- `forge-gui-web/frontend/src/lib/gameWebSocket.ts` - sendStartGame accepts config, connect sends START_GAME on open
- `forge-gui-web/frontend/src/hooks/useGameWebSocket.ts` - Accepts optional GameStartConfig, passes to connect
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` - Accepts optional gameConfig prop
- `forge-gui-web/frontend/src/App.tsx` - Lobby view in View union, "Play a Game" button, returnState for post-game lobby

## Decisions Made
- GameConfig flows through View state rather than being created/sent in lobby directly, keeping WebSocket lifecycle in GameBoard/useGameWebSocket
- Duplicated deck file lookup in WebServer rather than exposing DeckHandler.findDeckFile, per RESEARCH anti-pattern guidance
- Casual 60-card format matches decks with empty, "Constructed", or "casual 60-card" comment for broad compatibility

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Lobby fully functional for manual deck selection and AI configuration
- Ready for Plan 05-02 to add "Play This Deck" shortcut from deck editor

---
*Phase: 05-game-setup-integration*
*Completed: 2026-03-20*

## Self-Check: PASSED
