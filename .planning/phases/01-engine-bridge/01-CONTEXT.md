# Phase 1: Engine Bridge - Context

**Gathered:** 2026-03-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Java backend that bridges the Forge MTG engine to a WebSocket-connected browser client. Implements IGuiGame and IGuiBase interfaces for web, serializes game state to JSON, and handles the sync-to-async bridge between Forge's blocking input model and WebSocket communication. No frontend UI in this phase — purely Java infrastructure.

</domain>

<decisions>
## Implementation Decisions

### Server Framework
- Javalin 7 as the embedded HTTP + WebSocket server
- Exclude Forge's unused Jetty 9.4 transitive dependency; use Javalin's bundled Jetty 12
- Server runs on port 8080
- Headless initialization of Forge's StaticData/FModel — strip GUI dependencies, initialize only card database and game data. Fail fast if desktop code is needed

### Serialization Format
- Jackson 2.21 (LTS) for JSON serialization
- Flat DTO classes with ID references to avoid circular reference issues in TrackableObject graph (e.g., `CardDto` has `playerId` instead of embedded `PlayerView`)
- Incremental zone updates over WebSocket — only send changed zones/cards using Forge's existing `PlayerZoneUpdate` infrastructure

### Input Bridge Model
- CompletableFuture-based bridge: engine thread blocks on `CompletableFuture.get()`, WebSocket handler completes the future when player responds
- Single-threaded executor as pseudo-EDT to satisfy Forge's `FThreads.assertExecutedByEdt()` assertions
- Timeout with cleanup: set a timeout on CompletableFuture (e.g., 5 min). If expired, end the game session and clean up resources

### IGuiGame Implementation Scope
- Implement ALL ~90 IGuiGame methods — no stubs. Complete coverage from the start
- Also implement all IGuiBase methods needed for web context
- Full game loop integration test: start game, mulligan, play a land, cast a creature, attack, end turn, verify AI responds

### Claude's Discretion
- Exact DTO class structure and field naming
- WebSocket message protocol format (message types, envelope structure)
- Input correlation mechanism (inputId for nested input stack)
- Error serialization format
- Test framework choice (TestNG matches existing Forge tests)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Engine Interfaces
- `forge-gui/src/main/java/forge/gui/interfaces/IGuiGame.java` — 90+ method interface that WebGuiGame must implement. Defines all game event notifications and player input methods
- `forge-gui/src/main/java/forge/gui/interfaces/IGuiBase.java` — Platform operations interface. WebGuiBase must implement for headless web context
- `forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java` — Base implementation of IGuiGame with default behaviors

### Input System
- `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` — Bridges human player input with game engine. Uses blocking calls that need CompletableFuture bridge
- `forge-gui/src/main/java/forge/player/PlayerZoneUpdate.java` — Zone update model used for incremental updates
- `forge-gui/src/main/java/forge/player/PlayerZoneUpdates.java` — Collection of zone updates

### Game State Views
- `forge-game/src/main/java/forge/game/GameView.java` — Top-level game state view (serialization target)
- `forge-game/src/main/java/forge/game/card/CardView.java` — Card state view with circular refs to PlayerView
- `forge-game/src/main/java/forge/game/player/PlayerView.java` — Player state view

### Initialization
- `forge-core/src/main/java/forge/StaticData.java` — Card database and format loading entry point
- `forge-gui/src/main/java/forge/gamemodes/match/HostedMatch.java` — Match lifecycle orchestrator

### Threading
- `forge-gui/src/main/java/forge/util/FThreads.java` — EDT threading utilities with assertions that web must satisfy

### Research
- `.planning/research/PITFALLS.md` — 8 critical pitfalls, 5 of which are Phase 1 concerns
- `.planning/research/ARCHITECTURE.md` — System architecture with component boundaries and data flow
- `.planning/research/STACK.md` — Recommended technology stack with versions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AbstractGuiGame`: Base IGuiGame implementation — WebGuiGame should extend this where possible
- `PlayerZoneUpdate`/`PlayerZoneUpdates`: Already designed for incremental zone updates — use directly for WebSocket messages
- `HostedMatch`: Match lifecycle orchestrator — use as-is for starting/managing game sessions
- `GameView`/`CardView`/`PlayerView`: Existing view objects — create flat DTOs that mirror these

### Established Patterns
- I-prefix for interfaces (IGuiGame, IGuiBase) — follow for any new web interfaces
- `FModel` singleton for app-wide state — will need headless initialization path
- `FThreads` for thread safety — must provide web-compatible EDT replacement
- Module naming: `forge-gui-{platform}` — new module should be `forge-gui-web`

### Integration Points
- `forge-gui` module is the dependency boundary — `forge-gui-web` depends on `forge-gui`
- Root `pom.xml` needs new module declaration for `forge-gui-web`
- `forge-gui`'s existing Jetty 9.4 dependency needs exclusion in `forge-gui-web`'s pom.xml

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches. The key constraint is that the bridge must work end-to-end with a full game loop integration test before any frontend work begins.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-engine-bridge*
*Context gathered: 2026-03-16*
