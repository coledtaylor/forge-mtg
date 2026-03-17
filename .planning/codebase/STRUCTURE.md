# Codebase Structure

**Analysis Date:** 2026-03-16

## Directory Layout

```
forge-mtg/ (root, Maven multi-module project)
├── forge-core/                     # Card database, deck formats, static data
├── forge-game/                     # MTG rules engine, game state, AI
├── forge-ai/                       # AI decision-making
├── forge-gui/                      # Shared GUI framework and interfaces
├── forge-gui-desktop/              # Java Swing desktop client
├── forge-gui-mobile/               # LibGDX mobile client
├── forge-gui-android/              # Android-specific bindings
├── forge-gui-ios/                  # iOS-specific bindings
├── forge-gui-mobile-dev/           # Mobile dev/testing utilities
├── forge-web-starter/              # (Legacy/unused) Spring Boot skeleton
├── adventure-editor/               # Adventure mode editor tool
├── forge-installer/                # Installer build logic
├── forge-lda/                      # Latent Dirichlet Allocation (unused)
├── .planning/                      # GSD project planning documents
│   └── codebase/                   # Codebase analysis docs
├── docs/                           # Project documentation
├── pom.xml                         # Root Maven POM (declares modules)
└── checkstyle.xml                  # Code style rules
```

## Directory Purposes

**forge-core:**
- Purpose: Card definitions, editions, deck pools, formats, tokens
- Contains: Java classes for `PaperCard`, `CardDb`, `CardRules`, `CardEdition`, `TokenDb`, deck I/O
- Key files: `StaticData.java` (main entry point for card/format loading), `CardDb.java` (card lookup)
- Build: Maven JAR; no UI dependencies

**forge-game:**
- Purpose: Complete MTG rules implementation
- Contains: Game class, players, zones, cards in play, spellability system, combat, triggers, event system
- Key files: `Game.java` (main game controller), `Player.java` (player state), `Card.java` (in-game card), `PhaseType.java`, `Zone.java`
- Subdirs:
  - `ability/` — Ability resolution
  - `card/` — Card state and properties
  - `combat/` — Combat system
  - `event/` — Game events (GameEvent subclasses)
  - `phase/` — Phase management
  - `player/` — Player state and actions
  - `spellability/` — SpellAbility resolution
  - `zone/` — Zone management (hand, library, graveyard, field)
  - `trackable/` — View objects for GUI serialization
- Build: Maven JAR; depends on `forge-core`, `forge-ai`

**forge-ai:**
- Purpose: AI opponent decision-making
- Contains: AI profiles, game state evaluation, move selection algorithms
- Key concept: Called during `Player.getController().chooseFromOptions()` for AI players
- Build: Maven JAR; depends on `forge-game`

**forge-gui:**
- Purpose: Shared GUI infrastructure and interfaces
- Contains: `IGuiGame`, `IGuiBase` interfaces, base implementations, match orchestration, event handlers
- Key files:
  - `gui/interfaces/IGuiGame.java` — Main GUI event interface (60+ methods)
  - `gui/interfaces/IGuiBase.java` — Platform operations interface
  - `gamemodes/match/HostedMatch.java` — Match lifecycle orchestrator
  - `gamemodes/match/AbstractGuiGame.java` — Base implementation of `IGuiGame`
  - `control/FControlGameEventHandler.java` — Event processing pipeline
- Subdirs:
  - `gamemodes/` — Game mode coordinators (match, quest, gauntlet, etc.)
  - `gui/` — Shared GUI components and base classes
  - `interfaces/` — Abstract interfaces for GUI implementations
  - `itemmanager/` — Card pool UI (used by deck builder)
  - `localinstance/` — Skin, settings, achievements
- Build: Maven JAR; depends on `forge-game`

**forge-gui-desktop:**
- Purpose: Swing-based desktop UI
- Contains: Swing components, screens, event handlers
- Key files:
  - `GuiDesktop.java` — Implements `IGuiBase`; Swing app initialization
  - `screens/match/` — Match UI panels
  - `screens/home/` — Lobby UI
  - `screens/deckeditor/` — Deck builder UI
- Subdirs:
  - `screens/` — Main screens (home, match, deck editor)
  - `menus/` — Menu bars and popups
  - `control/` — Swing-specific event routing
- Build: Maven JAR; executable assembly with Java 17 requirement

**forge-gui-mobile:**
- Purpose: LibGDX-based mobile UI
- Contains: LibGDX rendering, touch input handling
- Key files: `GuiMobile.java` (implements `IGuiBase`)
- Build: Maven JAR; cross-platform (compiled for Android/iOS via native bindings)

**forge-web-starter:**
- Purpose: (Legacy, not active) Spring Boot + JPA starter project
- Status: Superseded by planned `forge-gui-web` module (uses WebSocket + React instead)
- Should not be modified; ignore in new development

**adventure-editor:**
- Purpose: Standalone tool for editing adventure mode content
- Status: Low priority; out of scope for web client epic
- Build: Maven JAR; executable assembly

## Key File Locations

**Entry Points:**
- Desktop: `forge-gui-desktop/src/main/java/forge/GuiDesktop.java` — `main()` method
- Mobile: `forge-gui-mobile/src/forge/GuiMobile.java` — App startup
- Web (planned): `forge-gui-web/src/main/java/forge/web/WebServer.java` (new) — HTTP + WebSocket server startup

**Configuration:**
- Maven: `pom.xml` (root), `pom.xml` (in each module) — Dependency and build config
- Checkstyle: `checkstyle.xml` — Code style rules
- Logging: `.mvn/settings.xml` (if present) — SLF4J configuration
- Game data: `res/` directories (managed by game, not source code) — Card definitions, formats, decks

**Core Logic:**
- Game engine: `forge-game/src/main/java/forge/game/Game.java` — Main game loop and rules
- Card database: `forge-core/src/main/java/forge/StaticData.java` — Card/format loading
- Match orchestration: `forge-gui/src/main/java/forge/gamemodes/match/HostedMatch.java` — Match lifecycle
- Event system: `forge-game/src/main/java/forge/game/event/` — All game events
- AI: `forge-ai/src/main/java/forge/` — Decision-making

**Testing:**
- Desktop tests: `forge-gui-desktop/src/test/java/`
- Card tests: `forge-core/src/test/java/`, `forge-game/src/test/java/`
- Test fixtures: `forge-gui-desktop/src/test/resources/`

## Naming Conventions

**Files:**
- **Class files:** PascalCase + `.java` (e.g., `GameView.java`, `PlayerControllerHuman.java`)
- **Test files:** Class name + `Test.java` or `Tests.java` (e.g., `CardDbPerformanceTests.java`)
- **Configuration:** lowercase-kebab-case + extension (e.g., `checkstyle.xml`, `pom.xml`)

**Directories:**
- **Packages:** lowercase with dots (e.g., `forge.game.card`, `forge.gui.screens.match`)
- **Feature directories:** descriptive plural (e.g., `screens/`, `abilities/`, `events/`)
- **Modules:** forge-{layer} or forge-gui-{platform} (e.g., `forge-gui-web`, `forge-gui-desktop`)

**Classes:**
- **Interfaces:** I + PascalCase (e.g., `IGuiGame`, `IGuiBase`, `IGameController`)
- **Abstract classes:** Abstract + PascalCase (e.g., `AbstractGuiGame`)
- **Enums:** PascalCase (e.g., `PhaseType`, `ZoneType`)
- **Views:** Class name + View (e.g., `GameView`, `CardView`, `PlayerView`)
- **Controllers:** Class name + Controller (e.g., `PlayerControllerHuman`)
- **Utilities:** Prefix of F + PascalCase (e.g., `FThreads`, `FModel`, `FileUtil`)

**Methods:**
- camelCase (e.g., `updatePhase()`, `showCombat()`, `getGameView()`)
- Boolean getters: `is`/`has` prefix (e.g., `isLegal()`, `hasStarted()`)
- Setters: `set` prefix (e.g., `setGameView()`)

## Where to Add New Code

**New Feature (e.g., new game format or ability type):**
- Primary code: `forge-game/src/main/java/forge/game/` — Add ability handlers in `ability/effects/`, or format rules in `player/`
- Supporting card data: `forge-core/src/main/java/forge/` — Card definitions, format predicates
- Tests: `forge-game/src/test/java/` — Rules validation, ability tests
- Desktop UI (if needed): `forge-gui-desktop/src/main/java/forge/screens/` — New screen or panel
- Web UI (if needed): `forge-gui-web/frontend/src/` (new) — React component for new UI

**New Component/Module (e.g., new GUI platform like web):**
- Implementation: Create new Maven module `forge-gui-{platform}/`
- Implement interfaces: Create classes implementing `IGuiGame` and `IGuiBase`
- Add pom.xml: Depend on `forge-gui` (which depends on `forge-game`)
- Entry point: Create main/startup class
- Register module: Add to root `pom.xml` modules list
- Example: Planned `forge-gui-web` module structure below

**Utilities / Shared Helpers:**
- Java utilities: `forge-core/src/main/java/forge/util/` — Generic Java helpers
- Game utilities: `forge-game/src/main/java/forge/game/` — Game-specific logic
- UI utilities: `forge-gui/src/main/java/forge/gui/` — UI-agnostic helpers
- Desktop-specific: `forge-gui-desktop/src/main/java/forge/` — Swing helpers

## Special Directories

**res/ (Game Resources):**
- Purpose: Card definitions, game data, images, audio (not in source control on most machines)
- Generated: Yes (built from card definition files)
- Committed: No (managed separately via `res/` folder structure)
- Accessed via: `ForgeConstants.CACHE_CARD_PICS_DIR`, `StaticData` loader

**target/ (Maven Build Output):**
- Purpose: Compiled classes, JARs, assembled applications
- Generated: Yes (by Maven)
- Committed: No (in .gitignore)
- Location: Each module has its own `target/` directory

**.planning/codebase/ (GSD Analysis Docs):**
- Purpose: Architecture, structure, conventions, testing patterns documentation
- Generated: Yes (by GSD mapping tool)
- Committed: Yes (tracked in version control)
- Consumed by: `/gsd:plan-phase` and `/gsd:execute-phase` commands

**.mvn/ (Maven Build Configuration):**
- Purpose: Maven wrapper scripts and settings
- Committed: Yes
- Key file: `.mvn/wrapper/maven-wrapper.properties` — Maven version pinning

## Planned: forge-gui-web Module Structure

When building the new web client module, follow this structure:

```
forge-gui-web/
├── pom.xml                         # Maven config; depend on forge-gui
├── src/main/java/forge/web/
│   ├── WebServer.java              # HTTP + WebSocket server startup
│   ├── game/
│   │   ├── WebGuiGame.java         # Implements IGuiGame
│   │   └── WebGameController.java  # WebSocket message dispatch
│   ├── api/
│   │   ├── CardSearchController.java — REST endpoints for card search
│   │   ├── DeckController.java      — CRUD for deck management
│   │   └── GameController.java      — Game setup endpoints
│   ├── websocket/
│   │   ├── GameMessageHandler.java  — WebSocket message parsing
│   │   ├── ClientSession.java       — Per-connection state
│   │   └── MessageQueue.java        — Thread-safe input queue for blocking calls
│   └── util/
│       └── GameStateSerializer.java — GameView → JSON serialization
├── src/main/resources/
│   └── application.properties       — Server config (port, etc.)
├── src/test/java/forge/web/
│   └── WebGuiGameTest.java         — Unit tests
└── frontend/
    ├── package.json                 # Node.js; React, Vite, Tailwind, Shadcn
    ├── tsconfig.json                # TypeScript config
    ├── vite.config.ts               # Vite bundler config
    ├── src/
    │   ├── main.tsx                 # React entry point
    │   ├── App.tsx                  # Root component
    │   ├── pages/
    │   │   ├── Lobby.tsx            — Game setup screen
    │   │   ├── Match.tsx            — Main game board
    │   │   └── DeckBuilder.tsx      — Deck editor
    │   ├── components/
    │   │   ├── Board.tsx            — Battlefield rendering
    │   │   ├── Hand.tsx             — Hand display
    │   │   ├── CardGrid.tsx         — Card list component
    │   │   └── ...
    │   ├── hooks/
    │   │   ├── useGameWebSocket.ts  — WebSocket connection
    │   │   ├── useGameState.ts      — Game state context hook
    │   │   └── useCardSearch.ts     — Card search API hook
    │   ├── api/
    │   │   ├── gameApi.ts           — Game REST endpoints
    │   │   └── cardApi.ts           — Card search endpoints
    │   ├── types/
    │   │   └── game.ts              — TypeScript types matching backend JSON
    │   └── styles/
    │       └── globals.css          — Tailwind setup
    └── dist/                        — Built output (not committed)
```

## Maven Module Dependencies

**Dependency Graph:**

```
forge (root)
├── forge-core (no deps on other modules)
├── forge-ai
│   └── → forge-game
├── forge-game
│   └── → forge-core
├── forge-gui
│   ├── → forge-game
│   └── → forge-core
├── forge-gui-desktop
│   └── → forge-gui
├── forge-gui-mobile
│   └── → forge-gui
├── forge-gui-web (planned)
│   └── → forge-gui
├── adventure-editor
│   └── → forge-game
└── [others with lower priority]
```

**Rule:** No backwards dependencies (e.g., `forge-core` never depends on `forge-game`; `forge-game` never depends on GUI modules).
