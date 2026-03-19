---
phase: 01-engine-bridge
plan: 02
subsystem: api
tags: [websocket, completable-future, jackson, dto, iguigame, input-bridge]

# Dependency graph
requires:
  - phase: 01-engine-bridge plan 01
    provides: forge-gui-web Maven module with Javalin 7, Jackson, WebGuiBase pseudo-EDT
provides:
  - WebInputBridge CompletableFuture-based input correlation for async WebSocket I/O
  - ViewRegistry TrackableObject ID-to-instance mapping
  - Flat DTOs (CardDto, PlayerDto, GameStateDto, SpellAbilityDto, CombatDto, ZoneUpdateDto)
  - WebSocket message protocol (MessageType enum, OutboundMessage, InboundMessage envelopes)
  - WebGuiGame full IGuiGame implementation for web clients
affects: [01-engine-bridge, 02-rest-api, 04-game-board]

# Tech tracking
tech-stack:
  added: []
  patterns: [send-and-wait-completable-future, flat-dto-with-id-refs, outbound-message-envelope]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/WebInputBridge.java
    - forge-gui-web/src/main/java/forge/web/ViewRegistry.java
    - forge-gui-web/src/main/java/forge/web/dto/CardDto.java
    - forge-gui-web/src/main/java/forge/web/dto/PlayerDto.java
    - forge-gui-web/src/main/java/forge/web/dto/GameStateDto.java
    - forge-gui-web/src/main/java/forge/web/dto/SpellAbilityDto.java
    - forge-gui-web/src/main/java/forge/web/dto/CombatDto.java
    - forge-gui-web/src/main/java/forge/web/dto/ZoneUpdateDto.java
    - forge-gui-web/src/main/java/forge/web/protocol/MessageType.java
    - forge-gui-web/src/main/java/forge/web/protocol/OutboundMessage.java
    - forge-gui-web/src/main/java/forge/web/protocol/InboundMessage.java
    - forge-gui-web/src/main/java/forge/web/WebGuiGame.java
    - forge-gui-web/src/test/java/forge/web/WebInputBridgeTest.java
    - forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java
  modified: []

key-decisions:
  - "DTOs use public fields for simplicity -- checkstyle only enforces unused/redundant imports"
  - "WebGuiGame uses TypeReference overload for sendAndWait to handle generic response types (List<Integer>, Map<Integer,Integer>)"
  - "CombatDto.AttackerInfo uses defendingPlayerId for both player and planeswalker defenders"
  - "PlayerDto uses CounterEnumType.POISON for poison counter extraction"

patterns-established:
  - "send() for fire-and-forget, sendAndWait() for blocking input -- mirrors NetGuiGame pattern"
  - "All DTOs have static from() factory method converting View objects to flat JSON-safe POJOs"
  - "OutboundMessage envelope: type + optional inputId + sequenceNumber + payload"
  - "payloadMap() helper for building ad-hoc JSON payloads in fire-and-forget methods"

requirements-completed: [API-03, API-04]

# Metrics
duration: 11min
completed: 2026-03-18
---

# Phase 1 Plan 02: Core Bridge Implementation Summary

**WebInputBridge with CompletableFuture input correlation, flat DTOs avoiding circular refs, and full WebGuiGame implementing all ~90 IGuiGame methods via WebSocket send/sendAndWait**

## Performance

- **Duration:** 11 min
- **Started:** 2026-03-19T02:16:23Z
- **Completed:** 2026-03-19T02:27:14Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments
- WebInputBridge correctly manages CompletableFuture lifecycle (register, complete, cancelAll, timeout)
- All 6 DTOs serialize to flat JSON without circular references -- CardDto uses ownerId/controllerId instead of embedded PlayerView
- WebGuiGame implements every IGuiGame method with real WebSocket communication -- zero stubs
- Message protocol covers all IGuiGame communication patterns (14 outbound + 4 inbound types)
- 9 unit tests pass: 5 for WebInputBridge, 4 for DTO serialization round-trips

## Task Commits

Each task was committed atomically:

1. **Task 1: WebInputBridge, ViewRegistry, DTOs, and message protocol** - `20c9bf0cf9` (feat)
2. **Task 2: WebGuiGame - Full IGuiGame implementation** - `4d59884c5b` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/WebInputBridge.java` - CompletableFuture-based input correlation with cancelAll and GameSessionExpiredException
- `forge-gui-web/src/main/java/forge/web/ViewRegistry.java` - ConcurrentHashMap-backed TrackableObject ID-to-instance mapping
- `forge-gui-web/src/main/java/forge/web/dto/CardDto.java` - Flat card DTO with ID references (ownerId, controllerId) instead of embedded objects
- `forge-gui-web/src/main/java/forge/web/dto/PlayerDto.java` - Player state DTO with mana pool and zone-to-card-ID mapping
- `forge-gui-web/src/main/java/forge/web/dto/GameStateDto.java` - Top-level game state aggregating players, cards, stack, combat
- `forge-gui-web/src/main/java/forge/web/dto/SpellAbilityDto.java` - Stack item DTO with source card and activating player IDs
- `forge-gui-web/src/main/java/forge/web/dto/CombatDto.java` - Combat state with attacker/blocker/defender ID assignments
- `forge-gui-web/src/main/java/forge/web/dto/ZoneUpdateDto.java` - Zone change notification DTO
- `forge-gui-web/src/main/java/forge/web/protocol/MessageType.java` - Enum of 18 WebSocket message types
- `forge-gui-web/src/main/java/forge/web/protocol/OutboundMessage.java` - Server-to-client message envelope with sequenceNumber
- `forge-gui-web/src/main/java/forge/web/protocol/InboundMessage.java` - Client-to-server message envelope
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` - Full IGuiGame implementation extending AbstractGuiGame
- `forge-gui-web/src/test/java/forge/web/WebInputBridgeTest.java` - 5 tests for input bridge lifecycle
- `forge-gui-web/src/test/java/forge/web/DtoSerializationTest.java` - 4 tests for DTO serialization round-trips and size bounds

## Decisions Made
- DTOs use public fields since project checkstyle only enforces unused/redundant imports (not visibility)
- WebGuiGame uses Jackson TypeReference for generic response deserialization (List<Integer>, Map<Integer,Integer>)
- CombatDto.AttackerInfo.defendingPlayerId works for both player and planeswalker defenders (both have IDs)
- PlayerDto poison counters extracted via CounterEnumType.POISON enum value

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed unused import checkstyle errors**
- **Found during:** Task 1 and Task 2
- **Issue:** Checkstyle (RedundantImport + UnusedImports rules) rejected unused imports
- **Fix:** Removed 5 unused imports across PlayerDto, DtoSerializationTest, WebInputBridgeTest, WebGuiGame
- **Files modified:** PlayerDto.java, DtoSerializationTest.java, WebInputBridgeTest.java, WebGuiGame.java
- **Verification:** mvn compile exits 0

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Trivial import cleanup. No scope creep.

## Issues Encountered
- CounterType is an interface (not enum) in Forge -- used CounterEnumType.POISON directly for poison counter access
- Javalin 7 WsContext is in package io.javalin.websocket (confirmed via JAR inspection)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- WebGuiGame is fully implemented and ready for WebSocket session management (Plan 03)
- WebInputBridge is ready to be connected to inbound WebSocket message handler
- DTO serialization proven -- game state can be sent to browser as flat JSON

## Self-Check: PASSED

All 14 created files verified on disk. Both task commits (20c9bf0cf9, 4d59884c5b) verified in git log.

---
*Phase: 01-engine-bridge*
*Completed: 2026-03-18*
