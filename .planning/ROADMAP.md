# Roadmap: Forge Web Client

## Overview

This roadmap delivers a browser-based MTG client that wraps Forge's existing Java engine. The critical path starts with the sync-to-async bridge (the highest-risk work), then builds REST endpoints and the frontend scaffold, followed by the deck builder and game board in parallel-capable phases, and culminates with game setup and the integrated build-then-play loop. Every phase delivers a coherent, verifiable capability.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Engine Bridge** - Java backend with WebSocket bridge, IGuiGame/IGuiBase implementation, and game state serialization (completed 2026-03-19)
- [x] **Phase 2: REST API + Frontend Scaffold** - Card search and deck CRUD endpoints, React app with Scryfall card display (completed 2026-03-19)
- [ ] **Phase 3: Deck Builder** - Full deck building experience with search, editing, statistics, and format validation
- [ ] **Phase 4: Game Board** - Gameplay UI with zone rendering, player interaction, combat, and prompt system
- [ ] **Phase 5: Game Setup + Integration** - Lobby screen, format/deck selection, and the integrated build-then-play loop

## Phase Details

### Phase 1: Engine Bridge
**Goal**: Forge engine is accessible over WebSocket with correct threading, serialization, and input handling
**Depends on**: Nothing (first phase)
**Requirements**: API-03, API-04, API-05, API-06
**Success Criteria** (what must be TRUE):
  1. A WebSocket client can connect to the running server and receive a game state message after starting a match
  2. The server correctly handles the mulligan prompt-response cycle over WebSocket (engine blocks, client responds, engine advances)
  3. The server serializes game state as flat DTOs with ID references (no circular references, no raw TrackableObject graphs)
  4. Multiple sequential prompts (nested input stack) are correctly correlated via inputId and processed in LIFO order
  5. The Forge card database and static data initialize successfully on server startup without desktop GUI dependencies
**Plans**: TBD

Plans:
- [ ] 01-01: TBD
- [ ] 01-02: TBD
- [ ] 01-03: TBD

### Phase 2: REST API + Frontend Scaffold
**Goal**: Card search and deck management work end-to-end from browser to backend, and the React app renders card images
**Depends on**: Phase 1
**Requirements**: API-01, API-02, DECK-02
**Success Criteria** (what must be TRUE):
  1. User can search cards by name, type, color, CMC, and format legality via the browser and see paginated results
  2. User can create, save, load, and delete decks via the browser (persisted in Forge's .dck format)
  3. User can see card images loaded from Scryfall CDN with lazy loading (no per-card API calls, no rate limit violations)
  4. The React app builds and serves via Vite with hot module replacement working
**Plans**: 2 plans

Plans:
- [ ] 02-01-PLAN.md — Backend REST API (card search + deck CRUD endpoints)
- [ ] 02-02-PLAN.md — Frontend scaffold (React + shadcn/ui + Scryfall card images)

### Phase 3: Deck Builder
**Goal**: Users can build, analyze, and validate decks for any supported format entirely in the browser
**Depends on**: Phase 2
**Requirements**: DECK-01, DECK-03, DECK-04, DECK-05, DECK-06, DECK-07, DECK-08, DECK-09, DECK-10, DECK-11, DECK-12, DECK-13
**Success Criteria** (what must be TRUE):
  1. User can search for cards and add them to a deck with quantity controls, seeing both list and visual grid views
  2. User can see mana curve, color distribution, and card type breakdown charts for their deck
  3. User can see format legality validation results indicating which cards are legal or illegal and why
  4. User can manage sideboard cards and set a commander for Commander format decks
  5. User can quickly add basic lands with a dedicated quantity control panel
**Plans**: 3 plans

Plans:
- [ ] 03-01-PLAN.md — Backend extensions + frontend data layer (types, API, hooks, utilities)
- [ ] 03-02-PLAN.md — Core deck editor UI (layout, search, deck list, grid view, hover preview, lands)
- [ ] 03-03-PLAN.md — Stats charts, validation, commander, sideboard, mini stats

### Phase 4: Game Board
**Goal**: Users can play a full game of Magic against the AI through the browser with all zones, prompts, and combat working
**Depends on**: Phase 1
**Requirements**: GAME-01, GAME-02, GAME-03, GAME-04, GAME-05, GAME-06, GAME-07, GAME-08, GAME-09, GAME-10, GAME-11
**Success Criteria** (what must be TRUE):
  1. User can see the battlefield with all zones rendered (hand, battlefield, graveyard, exile, library, stack) updating in real time via WebSocket
  2. User can cast spells from hand, activate abilities, and respond to prompts with selection dialogs
  3. User can declare attackers and blockers in combat with visual assignment UI
  4. User can see life totals, mana pool, phase/turn indicator, and the stack with spells in resolution order
  5. User can hover or click any card to see an enlarged detail view
**Plans**: 4 plans

Plans:
- [ ] 04-01-PLAN.md — Game data foundation (types, Zustand store, WebSocket manager, card image wrapper, routing)
- [ ] 04-02-PLAN.md — Board layout shell (CSS Grid, PlayerInfoBar, PhaseStrip, StackPanel, ZonePile/ZoneOverlay)
- [ ] 04-03-PLAN.md — Card zone components (HandZone/HandCard fan, BattlefieldZone/GameCard with tap/counters/attachments)
- [ ] 04-04-PLAN.md — Interaction layer (ActionBar, ChoiceDialog, CombatOverlay, casting, GameOverScreen)

### Phase 5: Game Setup + Integration
**Goal**: Users can go from building a deck to playing it against the AI in one seamless flow
**Depends on**: Phase 3, Phase 4
**Requirements**: SETUP-01, SETUP-02, SETUP-03, SETUP-04
**Success Criteria** (what must be TRUE):
  1. User can select a format (Commander, Standard, casual 60-card, Jumpstart) and choose from their saved decks
  2. User can start a game against the AI from the lobby screen
  3. User can navigate from the deck builder directly into a game with the current deck pre-selected
**Plans**: TBD

Plans:
- [ ] 05-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5
Note: Phases 3 and 4 can execute in parallel (different dependency chains).

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Engine Bridge | 3/3 | Complete   | 2026-03-19 |
| 2. REST API + Frontend Scaffold | 2/2 | Complete   | 2026-03-19 |
| 3. Deck Builder | 0/3 | Not started | - |
| 4. Game Board | 1/4 | In Progress|  |
| 5. Game Setup + Integration | 0/1 | Not started | - |
