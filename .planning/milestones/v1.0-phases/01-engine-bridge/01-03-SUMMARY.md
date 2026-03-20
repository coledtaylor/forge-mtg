---
phase: 01-engine-bridge
plan: 03
subsystem: api
tags: [websocket, javalin, integration-test, game-loop, input-queue, hosted-match]

# Dependency graph
requires:
  - phase: 01-engine-bridge plan 01
    provides: forge-gui-web Maven module with Javalin 7, Jackson, WebGuiBase pseudo-EDT
  - phase: 01-engine-bridge plan 02
    provides: WebInputBridge, ViewRegistry, DTOs, WebGuiGame IGuiGame implementation
provides:
  - WebSocket game endpoint at /ws/game/{gameId} with full message routing
  - GameSession lifecycle management (create, route messages, cleanup on disconnect)
  - BUTTON_OK/BUTTON_CANCEL message types for Forge's InputQueue system
  - Proven end-to-end game loop (connect, start, mulligan, play, AI responds)
  - Default deck creation helpers for testing
affects: [02-rest-api, 04-game-board, 05-game-setup]

# Tech tracking
tech-stack:
  added: []
  patterns: [dual-input-system, button-input-routing, game-session-management, ws-integration-testing]

key-files:
  created:
    - forge-gui-web/src/test/java/forge/web/GameLoopIntegrationTest.java
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/src/main/java/forge/web/protocol/MessageType.java

key-decisions:
  - "Forge uses two input mechanisms: InputQueue (buttons) for mulligan/priority, sendAndWait (CompletableFuture) for choices/targeting"
  - "Added BUTTON_OK/BUTTON_CANCEL message types to bridge the InputQueue system over WebSocket"
  - "Default decks use 60 basic lands (Mountain/Forest) as placeholder until Phase 5 deck selection"
  - "WebServer.createApp() extracted as static method for integration test reuse"
  - "Game started on ThreadUtil game thread pool to avoid blocking WebSocket thread"

patterns-established:
  - "GameSession pattern: ConcurrentHashMap<gameId, GameSession> for session management"
  - "Dual input handling: BUTTON_OK/CANCEL for InputProxy, CHOICE_RESPONSE for sendAndWait"
  - "Integration test pattern: Javalin app.start(0) for random port, TestWsClient helper with autoRespond"
  - "Button routing: BUTTON_OK -> IGameController.selectButtonOk() on the WebGuiGame's game controller"

requirements-completed: [API-03, API-04, API-05, API-06]

# Metrics
duration: 34min
completed: 2026-03-18
---

# Phase 1 Plan 03: WebSocket Game Endpoint + Integration Test Summary

**WebSocket game endpoint with dual input system routing and 5 integration tests proving full game loop: connect, start, mulligan, play, AI responds**

## Performance

- **Duration:** 34 min
- **Started:** 2026-03-19T02:31:59Z
- **Completed:** 2026-03-19T03:06:13Z
- **Tasks:** 3 (2 auto + 1 human-verify checkpoint)
- **Files modified:** 3

## Accomplishments
- WebSocket endpoint at /ws/game/{gameId} routes all message types to correct handlers
- Discovered and bridged Forge's dual input system: InputQueue buttons + sendAndWait CompletableFutures
- Full game loop proven end-to-end: WebSocket connects, game starts, mulligan handled via buttons, game progresses through phases/turns
- 5 integration tests pass (24 total tests in forge-gui-web), all with timeouts to prevent hangs
- Session lifecycle fully managed: create on START_GAME, cleanup on disconnect/error

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire WebSocket endpoint and message routing in WebServer** - `7660ee27bc` (feat)
2. **Task 2: Full game loop integration test** - `122a5c3218` (feat)
3. **Task 3: Verify engine bridge works end-to-end** - user-approved checkpoint

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - WebSocket endpoint, GameSession, message routing, default decks, BUTTON_OK/CANCEL handling
- `forge-gui-web/src/main/java/forge/web/protocol/MessageType.java` - Added BUTTON_OK, BUTTON_CANCEL inbound message types
- `forge-gui-web/src/test/java/forge/web/GameLoopIntegrationTest.java` - 5 integration tests with TestWsClient helper

## Decisions Made
- Forge uses two separate input mechanisms that both need WebSocket support:
  1. **InputQueue/InputProxy** (button-based): Used for mulligan, passing priority, blocking. Server sends BUTTON_UPDATE, client responds BUTTON_OK/CANCEL, routed to IGameController.selectButtonOk/Cancel
  2. **sendAndWait** (CompletableFuture): Used for card choices, targeting. Server sends PROMPT_CHOICE with inputId, client responds CHOICE_RESPONSE with matching inputId
- Default decks are 60 basic Mountains (human) and 60 basic Forests (AI) -- sufficient for integration testing, replaced in Phase 5
- WebServer.createApp() extracted as package-private static method so integration tests can create fresh server instances on random ports
- Game thread error handling: startMatch wrapped in try-catch to prevent silent game thread death

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Jackson serialization of empty payload**
- **Found during:** Task 2 (integration test)
- **Issue:** `new Object()` as START_GAME payload caused "No serializer found for class java.lang.Object"
- **Fix:** Changed to `Collections.emptyMap()` for empty payloads
- **Files modified:** GameLoopIntegrationTest.java
- **Verification:** All tests pass
- **Committed in:** 122a5c3218 (Task 2 commit)

**2. [Rule 3 - Blocking] Added BUTTON_OK/BUTTON_CANCEL input system**
- **Found during:** Task 2 (integration test debugging)
- **Issue:** Mulligan uses Forge's InputQueue/InputProxy button system, not getChoices()/sendAndWait. PROMPT_CHOICE never received for mulligan -- only BUTTON_UPDATE was sent.
- **Fix:** Added BUTTON_OK and BUTTON_CANCEL message types, routed to IGameController.selectButtonOk/Cancel. Updated test auto-respond to handle both input mechanisms.
- **Files modified:** MessageType.java, WebServer.java, GameLoopIntegrationTest.java
- **Verification:** All 5 integration tests pass
- **Committed in:** 122a5c3218 (Task 2 commit)

**3. [Rule 3 - Blocking] Removed unused import flagged by checkstyle**
- **Found during:** Task 1 (compilation)
- **Issue:** MessageType import unused in WebServer (switch on enum doesn't require it)
- **Fix:** Removed the import
- **Files modified:** WebServer.java
- **Verification:** mvn compile exits 0
- **Committed in:** 7660ee27bc (Task 1 commit)

---

**Total deviations:** 3 auto-fixed (1 bug, 2 blocking)
**Impact on plan:** The dual input system discovery was the most significant deviation. The plan assumed all prompts use getChoices()/sendAndWait, but Forge's mulligan and priority system use a separate InputQueue/InputProxy mechanism requiring BUTTON_OK/CANCEL support. This is correctly documented for Phase 4 game board development.

## Issues Encountered
- Forge's InputQueue/InputSyncronizedBase uses Observer pattern with InputProxy -- the ShowMessage flow calls updateButtons() and showPromptMessage() as fire-and-forget, then blocks on CountDownLatch. This is fundamentally different from the CompletableFuture-based sendAndWait pattern. Both need WebSocket support.
- Javalin 7 WsContext types are specific per handler: WsConnectContext, WsMessageContext, WsCloseContext, WsErrorContext. All extend WsContext which has pathParam() and send().
- `config.routes.ws()` is the correct Javalin 7 API for WebSocket routes (not `app.ws()`)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 1 (Engine Bridge) is COMPLETE: all 3 plans executed, all success criteria met
- WebSocket game endpoint ready for Phase 4 (Game Board) frontend to connect to
- REST API endpoints ready for Phase 2 extension (card search, deck CRUD)
- Dual input system documented for Phase 4 implementors: handle both BUTTON_UPDATE and PROMPT_CHOICE

## Self-Check: PASSED

All 3 created/modified files verified on disk. Both task commits (7660ee27bc, 122a5c3218) verified in git log.

---
*Phase: 01-engine-bridge*
*Completed: 2026-03-18*
