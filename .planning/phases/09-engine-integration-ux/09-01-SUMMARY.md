---
phase: 09-engine-integration-ux
plan: 01
subsystem: api
tags: [auto-pass, undo, websocket, game-engine, priority]

# Dependency graph
requires:
  - phase: 08-gameplay-ux
    provides: WebGuiGame with BUTTON_UPDATE, WebServer message routing, game session management
provides:
  - UNDO and SET_AUTO_PASS inbound message types and WebServer handlers
  - Auto-pass logic in WebGuiGame.updateButtons() that skips priority when no legal plays
  - canUndo flag in BUTTON_UPDATE payload via PlayerControllerHuman.canUndoLastAction()
  - hasLegalPlays() check across hand, battlefield, and external zones
  - PHASE_UPDATE with autoPass=true for frontend phase flash animation
affects: [09-02-PLAN, frontend-undo-button, frontend-auto-pass-toggle, frontend-phase-flash]

# Tech tracking
tech-stack:
  added: []
  patterns: [backend-driven-auto-pass, lazy-game-resolution-via-hostedmatch, inAutoPass-reentrant-guard]

key-files:
  created: []
  modified:
    - forge-gui-web/src/main/java/forge/web/protocol/MessageType.java
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/src/main/java/forge/web/WebGuiGame.java

key-decisions:
  - "Auto-pass uses HostedMatch.getGame() lazy resolution instead of eager Game reference"
  - "Human player identified via PlayerControllerHuman instanceof check (simplest reliable approach)"
  - "Auto-pass only fires when stack is empty (conservative: let player respond to stack items)"
  - "autoPassEnabled defaults to true per user decision in CONTEXT.md"

patterns-established:
  - "Lazy Game resolution: store HostedMatch reference, call getGame() when needed"
  - "Reentrant guard pattern: volatile inAutoPass flag prevents infinite auto-pass loops"
  - "Fail-open legal plays check: returns true when Game unavailable to avoid silent skipping"

requirements-completed: [GUX-06, GUX-09]

# Metrics
duration: 4min
completed: 2026-03-21
---

# Phase 9 Plan 1: Auto-Pass and Undo Backend Summary

**Backend auto-pass that skips priority when no legal plays exist, canUndo flag in BUTTON_UPDATE, and UNDO/SET_AUTO_PASS WebSocket message handlers**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T01:57:56Z
- **Completed:** 2026-03-21T02:01:38Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- UNDO and SET_AUTO_PASS message types routed from WebSocket to engine/WebGuiGame
- Auto-pass intercepts updateButtons() when player has no legal plays and stack is empty
- canUndo included in every BUTTON_UPDATE payload for frontend undo button visibility
- hasLegalPlays() checks hand, battlefield, and external zones via getAllPossibleAbilities(player, true)
- Reentrant inAutoPass guard prevents infinite loop when auto-pass triggers selectButtonOk()

## Task Commits

Each task was committed atomically:

1. **Task 1: Add UNDO and SET_AUTO_PASS message types + WebServer handlers** - `e27a47fd9d` (feat)
2. **Task 2: Add auto-pass logic and canUndo to WebGuiGame** - `e88f5195c3` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/protocol/MessageType.java` - Added UNDO and SET_AUTO_PASS enum values
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Added UNDO and SET_AUTO_PASS case handlers, setHostedMatch call
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` - Auto-pass logic, hasLegalPlays(), canUndo in BUTTON_UPDATE, HostedMatch lazy Game resolution

## Decisions Made
- Used HostedMatch lazy resolution instead of eagerly setting Game reference (Game doesn't exist until match starts, HostedMatch is available immediately)
- Human player identified via `p.getController() instanceof PlayerControllerHuman` (simpler and more reliable than matching by humanPlayerId or LobbyPlayer)
- Auto-pass only fires when stack is empty (conservative: always let player respond to stack items like spells/abilities)
- autoPassEnabled defaults to true per user context decision

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed unused JsonProcessingException import**
- **Found during:** Task 1 (compilation verification)
- **Issue:** Checkstyle flagged unused import in WebGuiGame.java, blocking compilation
- **Fix:** Removed the unused `com.fasterxml.jackson.core.JsonProcessingException` import
- **Files modified:** forge-gui-web/src/main/java/forge/web/WebGuiGame.java
- **Verification:** mvn compile succeeds
- **Committed in:** e27a47fd9d (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Pre-existing unused import unrelated to plan changes. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Backend auto-pass and undo fully wired, ready for Plan 02 frontend integration
- Frontend needs to handle: autoPass flag in PHASE_UPDATE, canUndo in BUTTON_UPDATE, send UNDO and SET_AUTO_PASS messages

---
*Phase: 09-engine-integration-ux*
*Completed: 2026-03-21*
