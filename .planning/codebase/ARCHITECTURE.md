# Architecture

**Analysis Date:** 2026-03-16

## Pattern Overview

**Overall:** Layered + Plugin-Based GUI Architecture

The Forge codebase follows a **layered architecture** where the core MTG rules engine (`forge-core`, `forge-game`, `forge-ai`) is completely decoupled from GUI implementations through well-defined interface boundaries. Multiple GUI frontends (Swing desktop, libGDX mobile, and emerging web) all implement the same `IGuiGame` and `IGuiBase` interfaces, allowing the engine to run without modification.

**Key Characteristics:**
- **Engine-agnostic:** Game logic in `forge-game` contains no GUI dependencies
- **Interface-driven:** All GUI communication flows through `IGuiGame` (game events) and `IGuiBase` (platform operations)
- **Modular:** Each major feature area has its own Maven module with clear dependencies
- **Event-based:** Game events (`GameEvent` and subclasses) notify the GUI of state changes
- **Trackable objects:** Serializable view objects (`GameView`, `CardView`, `PlayerView`) provide the GUI with JSON-serializable snapshots of game state

## Layers

**Core Engine (`forge-game`):**
- Purpose: Implements all MTG rules, card abilities, AI logic, and game flow
- Location: `forge-game/src/main/java/forge/game/`
- Contains: Game rules, players, cards, zones, spellability, combat, triggers, replacements, events
- Depends on: `forge-core` (card definitions), utilities
- Used by: All GUI implementations via `IGuiGame` interface

**Game Foundation (`forge-core`):**
- Purpose: Card database, deck definitions, editions, static rules data
- Location: `forge-core/src/main/java/forge/`
- Contains: Card definitions, card pools, deck formats, editions, tokens, mana rules
- Depends on: Utilities only
- Used by: `forge-game` (loads cards), GUI (searches cards)

**AI Engine (`forge-ai`):**
- Purpose: Computer opponent decision-making
- Location: `forge-ai/src/main/java/forge/`
- Contains: AI profiles (Cautious, Default, Experimental, Reckless), game state evaluation, move selection
- Depends on: `forge-game` (uses game state)
- Used by: `forge-game` (called during AI player turns)

**GUI Framework (`forge-gui`):**
- Purpose: Abstract GUI interfaces and base implementations
- Location: `forge-gui/src/main/java/forge/`
- Contains: `IGuiGame`, `IGuiBase` interfaces, `AbstractGuiGame` base class, game mode orchestrators (`HostedMatch`), event handlers, skin/asset management
- Depends on: `forge-game`, `forge-core`
- Used by: Desktop (Swing), Mobile (libGDX), Web implementations

**Desktop Implementation (`forge-gui-desktop`):**
- Purpose: Java Swing-based desktop client
- Location: `forge-gui-desktop/src/main/java/forge/`
- Contains: Swing components, match screens, deck editor, UI controllers
- Depends on: `forge-gui` (implements `IGuiGame`/`IGuiBase`)
- Used by: End users on desktop

**Mobile Implementation (`forge-gui-mobile`):**
- Purpose: LibGDX-based mobile client
- Location: `forge-gui-mobile/src/forge/`
- Contains: LibGDX rendering, touch controls
- Depends on: `forge-gui`
- Used by: End users on mobile devices

**Web Implementation (`forge-gui-web`, planned):**
- Purpose: React/TypeScript browser-based client
- Location: `forge-gui-web/` (new module)
- Contains: HTTP + WebSocket server, JSON message serialization, React frontend code
- Depends on: `forge-gui` (implements `IGuiGame`/`IGuiBase`)
- Used by: End users in web browsers

## Data Flow

**Game Start → Match Setup → Game Loop → Game End:**

1. **Lobby Phase**
   - User selects format, deck, AI difficulty in lobby UI
   - Frontend sends game settings (POST/REST) to backend server
   - Backend parses settings, loads deck from filesystem

2. **Match Initialization**
   - `HostedMatch.startGame()` called with `RegisteredPlayer` list and `IGuiGame` instances
   - Game engine creates players, shuffles decks, initializes zones
   - `GameView` snapshot sent to frontend via WebSocket

3. **Game Loop (repeating until game ends)**
   - **Player Turn:**
     - `GameLog` and phase change events sent to `IGuiGame.updatePhase()`
     - Engine awaits player input via `PlayerControllerHuman.move()` (blocking call)
     - Frontend receives update, renders board state, waits for player action
     - Player action (e.g., cast spell, attack) sent via WebSocket to server
     - Server's blocking queue unblocks `PlayerControllerHuman`, returns action
   - **Card/Zone Updates:**
     - As cards move zones, `PlayerZoneUpdate` objects sent to `IGuiGame.updateZones()`
     - Frontend re-renders affected zones
   - **Events:**
     - Combat, triggers, stack resolution sent as `GameEvent` subclasses
     - Frontend animates or logs events as appropriate

4. **Game End**
   - Winner determined, `GameEvent` sent to GUI
   - `IGuiGame.finishGame()` called
   - Match controller decides whether to start new game or end match

**State Management:**
- **Server-side:** Single `Game` instance per match; `PlayerControllerHuman` holds references to `IGuiGame` implementations
- **Client-side:** React state holds `GameView` (full board state), `CardView[]` (all visible cards), `PlayerView[]` (player info)
- **Updates:** Incremental updates via WebSocket (only changed `CardView` or `PlayerZoneUpdate` objects sent, not full GameView each time)

## Key Abstractions

**IGuiGame Interface:**
- Purpose: Abstracts all game event notifications sent from engine to GUI
- Examples: `forge-gui-desktop/src/main/java/forge/screens/match/`, `forge-gui/src/main/java/forge/gamemodes/match/AbstractGuiGame.java`
- Pattern: 60+ methods including `updatePhase()`, `updateZones()`, `showCombat()`, `handleGameEvent()`, etc.
- Implementation: Desktop/Mobile override for UI rendering; Web implementation serializes to JSON and sends via WebSocket

**IGuiBase Interface:**
- Purpose: Abstracts platform-specific operations (dialogs, file I/O, audio, imaging)
- Examples: `GuiDesktop` (Swing), `GuiMobile` (LibGDX)
- Pattern: File dialogs, choice dialogs, audio/image loading, threading primitives (`invokeInEdtLater`)
- Implementation: Web implementation mostly no-ops or REST calls; minimal platform operations needed

**TrackableObject / GameView / CardView / PlayerView:**
- Purpose: Serializable snapshots of game state sent from engine to GUI
- Examples: `forge-game/src/main/java/forge/game/GameView.java`, `CardView.java`, `PlayerView.java`
- Pattern: Read-only view objects; engine populates with game state, GUI deserializes and displays
- Uses: JSON serialization for web protocol

**HostedMatch:**
- Purpose: Orchestrates match lifecycle (init, loop, cleanup)
- Location: `forge-gui/src/main/java/forge/gamemodes/match/HostedMatch.java`
- Pattern: Holds references to `Match`, `Game`, `IGuiGame` instances; drives game loop via `match.play()`
- Uses: `GameRules`, `RegisteredPlayer`, `PlayerControllerHuman`

**PlayerControllerHuman:**
- Purpose: Bridges human player input with game engine
- Location: `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java`
- Pattern: Implements `PlayerController` interface; calls `IGuiGame` methods to request input from GUI, blocks waiting for response
- Uses: Blocking queue or `CompletableFuture` to synchronously wait for async WebSocket messages (for web)

## Entry Points

**Desktop Entry Point:**
- Location: `forge-gui-desktop/src/main/java/forge/GuiDesktop.java`
- Triggers: `main()` method; user launches desktop app
- Responsibilities: Initialize Swing framework, load assets, show lobby screen, wire up event handlers

**Mobile Entry Point:**
- Location: `forge-gui-mobile/src/forge/GuiMobile.java`
- Triggers: App startup on mobile device
- Responsibilities: Initialize LibGDX, load assets, show lobby screen

**Web Entry Point (Backend):**
- Location: `forge-gui-web/src/main/java/forge/web/` (planned)
- Triggers: `java -cp ... forge.web.WebServer` or Maven Javalin server startup
- Responsibilities: Start embedded HTTP + WebSocket server on localhost, initialize StaticData (card database), set up REST endpoints for card search and deck CRUD, wait for WebSocket connections

**Web Entry Point (Frontend):**
- Location: `forge-gui-web/frontend/src/main.tsx` (planned)
- Triggers: Browser loads `http://localhost:5173` (Vite dev server) or bundled app
- Responsibilities: Connect WebSocket to backend, render React app, initialize game state context

## Error Handling

**Strategy:** Multi-layer error catching with user-facing dialogs

**Patterns:**
- **Engine-level:** Game rules violations caught in ability resolution; invalid moves rejected before `PlayerControllerHuman` receives them
- **Controller-level:** `FControlGameEventHandler` wraps game event processing in try-catch; logs errors, shows error dialog
- **GUI-level (Desktop/Mobile):** `showBugReportDialog()` in `IGuiBase` displays error with stack trace option
- **Web-level:** WebSocket errors handled in React hook; server-side exceptions serialized to JSON and sent to frontend; frontend displays error toast/modal

## Cross-Cutting Concerns

**Logging:**
- Engine: `GameLog` class tracks game events (moves, triggers, damage) for replay/analysis
- Desktop/Mobile: System logs via SLF4J; user-visible log windows
- Web: Server logs to console/file; client logs to browser console; game events optionally sent to frontend for in-UI match log

**Validation:**
- Deck builder: `CardPool.isLegal()` checks format restrictions per `PaperCard`
- Match init: `RegisteredPlayer` validated for legal deck format before starting
- Runtime: `PlayerControllerHuman` only accepts moves that are in the current choice list returned by engine

**Authentication:**
- Desktop/Mobile: None (single-player local)
- Web: None (local-only, no user accounts)

**Threading:**
- Desktop: Swing EDT (Event Dispatch Thread) for all UI updates; `FThreads.invokeInEdtLater()` for thread-safe updates
- Mobile: LibGDX render thread; blocking calls on separate threads
- Web: Server: Main thread runs HTTP/WebSocket; `PlayerControllerHuman` blocking calls on WebSocket message arrival. Client: React main thread for rendering; WebSocket in separate thread
