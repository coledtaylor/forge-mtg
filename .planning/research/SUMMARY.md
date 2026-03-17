# Project Research Summary

**Project:** Forge MTG Web Client
**Domain:** Game client wrapping synchronous Java engine via WebSocket + REST
**Researched:** 2026-03-16
**Confidence:** HIGH

## Executive Summary

Building a web client for Forge MTG is primarily a bridge engineering problem, not a UI problem. The Forge game engine already has a complete rules implementation, AI, and 20,000+ cards — the challenge is exposing it to a browser safely. The engine is synchronous and blocking, using `CountDownLatch` for player input and an `InputQueue` stack that can nest multiple simultaneous prompts. The recommended approach is a thin Java web layer (Javalin 7 + Jackson) that implements `IGuiGame` by serializing engine callbacks to JSON WebSocket messages, with a `CompletableFuture`-based bridge that correctly unblocks the engine thread when the client responds. The React frontend (Vite 8, TypeScript 5.7, Zustand, TanStack Query) consumes this over WebSocket for real-time game state and REST for static data (cards, decks, formats).

The core product value is the integrated deck-builder-to-gameplay loop: users build a deck from Forge's full card pool, then immediately test it against a real AI opponent — something neither Moxfield (no gameplay) nor Arena (limited card pool) offers. The MVP scope is well-defined: deck CRUD + card search on the REST side, and a playable game board driven by the WebSocket protocol on the game side. Card images come from Scryfall CDN URLs and are never fetched per-card at runtime. Everything is desktop-only, localhost-only, single-user — multiplayer, collection tracking, and social features are explicitly out of scope.

The highest-risk area is the sync-to-async bridge in Phase 1. Five of the eight critical pitfalls are Phase 1 concerns: deadlocks from the `CountDownLatch` bridge, EDT threading assumptions in `FThreads`, the input stack being a LIFO deque (not simple request-response), circular references in `TrackableObject` serialization, and typed return values from generic `IGuiGame` methods that require a `ViewRegistry`. Getting these wrong means rewriting the foundation later; getting them right unlocks all subsequent phases. This is the recommendation: treat Phase 1 as proof-of-concept infrastructure with no UI, verify it with integration tests, and only build UI once the bridge is proven stable.

## Key Findings

### Recommended Stack

The stack is straightforward and high-confidence. On the frontend: React 19.2 + TypeScript 5.7 (not 6.0 RC) + Vite 8 + Tailwind 4.2 + Zustand 5 + TanStack Query 5 + react-use-websocket 4 + Zod 3. On the backend: Javalin 7 (which bundles Jetty 12) + Jackson 2.21. There is one critical integration constraint: `forge-gui` declares Jetty 9.4 as a transitive dependency. Source analysis confirms forge-gui's own code does not import Jetty classes, so Jetty 9.4 can be safely excluded from `forge-gui-web`'s pom.xml. Javalin 7 brings Jetty 12. This exclusion is mandatory.

See [STACK.md](.planning/research/STACK.md) for full dependency table, Maven XML, npm install commands, and version compatibility matrix.

**Core technologies:**
- **React 19.2 + TypeScript 5.7**: UI framework and type safety — TypeScript 5.7 is stable; 6.0 RC is too fresh
- **Vite 8 (Rolldown)**: Build tooling — 10-30x faster builds, HMR under 50ms
- **Tailwind CSS 4.2**: Styling — CSS-native config, ideal for many conditional visual states (tapped, highlighted, selected)
- **Zustand 5 + immer**: Client-side game state — selector-based subscriptions prevent full-board re-renders on single zone changes
- **TanStack Query 5**: Server state (REST) — handles card search, deck CRUD caching, pagination
- **react-use-websocket 4 + Zod 3**: WebSocket transport + runtime message validation — Zod validates incoming JSON and provides type narrowing
- **Javalin 7**: Java HTTP + WebSocket server — lightweight, embeddable, first-class WebSocket, built on Jetty 12
- **Jackson 2.21**: JSON serialization — LTS release, required for polymorphic `GameEvent` subclass handling via `@JsonTypeInfo`

**What to avoid:** Spring Boot (overkill), Socket.IO (wrong protocol layer), Axios (redundant given TanStack Query), Redux (excessive boilerplate), Create React App (deprecated), TypeScript 6.0 RC (not stable).

### Expected Features

The product splits cleanly into two subsystems: a deck builder (REST-only, no WebSocket) and a gameplay board (WebSocket-driven). The integrated flow between them — build a deck, immediately play it against AI — is the core differentiator.

See [FEATURES.md](.planning/research/FEATURES.md) for full prioritization matrix, competitor analysis, and dependency graph.

**Must have (table stakes — v1 MVP):**
- Card search with name/type/color/CMC/format filters — every deck builder has this
- Card image display via Scryfall — visual identification is non-negotiable
- Deck CRUD (create, save, load, delete) — persistent decks are fundamental
- Deck list view with quantity controls — basic editing
- Mana curve chart + format validation — minimum viable deck statistics
- Sideboard + Commander zone support — format-dependent deck sections
- Battlefield with all zones (hand, field, graveyard, exile, library, stack) — core gameplay display
- Card rendering with tap state and counters — basic visual game state
- Phase/turn indicator + life totals — orientation in game flow
- Priority/prompt system + choice/selection dialogs — ~15 `IGuiGame` choice methods must map to UI
- Combat phase UI (attackers, blockers, damage assignment) — combat is core to Magic
- Game setup screen (pick deck, start game vs AI) — the entry point

**Should have (competitive differentiators — v1.x):**
- Integrated build-then-play loop (capstone of both subsystems)
- Visual card grid/gallery view in deck builder
- Game log/history panel
- Stack visualization with full card images
- Keyboard shortcuts (Z=pass, Space=confirm)
- Auto-yield/auto-pass per phase
- Multiple AI difficulty selection (already exists in forge-ai, just expose it)
- Goldfish/solitaire mode for combo testing

**Defer (v2+):**
- Draft/sealed/limited modes — complex separate UI flow, needs its own research
- Advanced deck statistics (removal density, ramp count)
- Undo support — depends on engine capability investigation
- Quest/Adventure mode — a different product entirely

**Anti-features (never build):** Multiplayer/networked play, collection tracking, price data, card scanning, animated effects, social features, mobile-responsive UI.

### Architecture Approach

The architecture is a four-layer system: Forge engine (unchanged) → `WebGuiGame`/`WebGuiBase` bridge (new Java) → Javalin HTTP+WebSocket server (new Java) → React frontend (new TypeScript). Game state flows unidirectionally: engine → `WebGuiGame` → WebSocket → Zustand store → React components. Player actions flow in reverse. Components never hold game state; they subscribe to Zustand store slices via selectors. The WebSocket layer is a transport concern, kept separate from stores. Zone-based component decomposition (each MTG zone is its own component) is essential for render performance: when a card moves from hand to battlefield, only Hand and Battlefield re-render, not the entire board.

The threading model is non-negotiable: three separate threads per game session — game thread (blocks on `CompletableFuture.get()`), WebSocket handler thread (non-blocking frame I/O), and a pseudo-EDT single-threaded executor (processes inputs, calls `stop()` on inputs). Never run game logic on the WebSocket thread.

See [ARCHITECTURE.md](.planning/research/ARCHITECTURE.md) for the full system diagram, file structure, data flow diagrams, and code examples for all four architectural patterns.

**Major components:**
1. **WebGuiGame (Java)** — implements all 90+ `IGuiGame` methods; translates engine callbacks to JSON WebSocket messages; the most critical new code
2. **WebGuiBase (Java)** — implements `IGuiBase`; provides pseudo-EDT executor; hosts `WebServer` startup
3. **WebPlayerInput / ViewRegistry (Java)** — bridges blocking input to async WebSocket response; resolves client-returned IDs back to live Java objects
4. **REST Endpoints (Java)** — stateless: cards, decks, formats, match start
5. **Zustand stores (React)** — `gameStore` (real-time game state), `deckStore` (deck builder), `lobbyStore` (settings)
6. **Game Board components (React)** — zone-decomposed: `Battlefield`, `Hand`, `Stack`, `Graveyard`, `PhaseBar`, `PromptBar`
7. **Deck Builder components (React)** — `DeckEditor`, `CardSearch`, `DeckList`

**Key patterns to follow:**
- Request-response over WebSocket uses `requestId`/`inputId` correlation (not just message order)
- Input stack is LIFO — every prompt includes its `inputId`; server rejects stale responses
- Serialization uses ID-based references, never embedded object graphs — `ViewRegistry` resolves IDs to live objects
- Incremental zone updates, not full `GameView` on every change

### Critical Pitfalls

See [PITFALLS.md](.planning/research/PITFALLS.md) for all 8 critical pitfalls, technical debt patterns, performance traps, and the "looks done but isn't" checklist.

1. **Sync-to-async bridge deadlocks** — Build the `CompletableFuture`-based bridge first, before any UI. Add 5-minute timeout on all `CountDownLatch.await()` calls in web implementation. Prove with integration test: start game, reach mulligan, respond via WebSocket, verify engine advances. This is Phase 1 entry criteria.

2. **Input stack is LIFO, not simple request-response** — Casting a spell with targets + mana payment nests multiple inputs. Every prompt must carry an `inputId`. Client response must include matching `inputId`. Server rejects stale responses. Design this into the protocol in Phase 1; adding it later requires touching every message handler.

3. **TrackableObject serialization circular references** — `CardView` → `PlayerView` → `CardView[]` in 13+ zones creates circular reference chains. `CardView` alone has 180+ trackable properties. Never serialize the object graph. Use a flat `GameStateDTO` with ID references: `{ players: [...], cards: [...] }` where cards reference players by integer ID.

4. **EDT threading assumptions in Forge engine** — `FThreads.assertExecutedByEdt()` is called in blocking input code. The web `IGuiBase` must provide a pseudo-EDT single-threaded executor, not no-op these assertions. Wrong threading model requires rewriting the bridge layer — highest recovery cost of any pitfall.

5. **Typed return types from generic `IGuiGame` methods** — `getChoices<T>()`, `assignCombatDamage()` return Java generics including `CardView`, `PlayerView`, `SpellAbilityView`. JSON erases types. A `ViewRegistry` (map of `int id → TrackableObject`) must be built in Phase 1. Client responses use IDs only; server resolves IDs back to live Java objects before passing to engine.

## Implications for Roadmap

Based on combined research, the architecture's build order (ARCHITECTURE.md Phase 1-9) maps naturally to four roadmap phases. The critical path is: Java bridge → REST API → WebSocket protocol → Frontend scaffold → Game board → Deck builder → Polish.

### Phase 1: Core Bridge (Java-Only Infrastructure)

**Rationale:** Five of eight critical pitfalls must be resolved here. Nothing else works until the sync-to-async bridge is proven stable. This is foundational plumbing with no UI output. The output is a runnable Java server that can start a game, send a mulligan prompt over WebSocket, receive a response, and advance the engine — with correct threading, correct serialization, and correct input stack handling.

**Delivers:**
- `forge-gui-web` Maven module wired into the Forge build
- Javalin 7 server with static file serving + CORS
- `WebGuiBase` with pseudo-EDT executor (IGuiBase implementation)
- `WebGuiGame` skeleton with Tier 1 + Tier 2 `IGuiGame` methods implemented (not no-ops — throw `UnsupportedOperationException` for unimplemented methods)
- `ViewRegistry` for ID-based round-tripping of `TrackableObject` references
- Flat `GameStateDTO` serialization layer (not raw `CardView`/`GameView`)
- Input stack protocol with `inputId` correlation
- Integration test: start game → reach mulligan → WebSocket response → engine advances

**Addresses pitfalls:** Deadlocks (#1), Input stack LIFO (#2), Circular serialization (#3), EDT threading (#4), Typed return types (#5)

**Research flag:** NEEDS PHASE RESEARCH — the `InputQueue`/`InputSyncronizedBase`/`FThreads` interaction is underdocumented. This is the highest-risk implementation task in the project. Recommend a focused research spike on Forge's existing network play serialization (`TrackableSerializer`) as a reference implementation before coding.

---

### Phase 2: REST API + Card Display Foundation

**Rationale:** The deck builder and game lobby both depend on REST endpoints. Card image loading strategy must be resolved before either the deck builder or gameplay board is built (Scryfall rate limiting pitfall #6 applies to both). This phase is parallelizable with Phase 1 on the frontend side once the REST API shape is defined.

**Delivers:**
- `GET /api/cards` with debounced search and pagination
- `GET /api/decks`, `POST`, `PUT`, `DELETE /api/decks/{id}` (Forge deck format compatible)
- `GET /api/formats`
- `POST /api/match/start` (creates `HostedMatch`, returns WebSocket URL)
- Scryfall bulk data strategy: use CDN image URLs (`/cards/{set}/{number}?format=image`), not per-card API calls
- Shared `Card.tsx` component with lazy loading via Intersection Observer
- React app scaffold with Vite 8 + Tailwind 4.2 + Zustand stores + TanStack Query
- MSW mocks for all REST endpoints (frontend can develop without running Java backend)

**Addresses pitfalls:** Scryfall rate limiting (#6); StaticData initialization (integration gotcha — initialize at server startup, block until ready)

**Research flag:** Standard patterns. REST CRUD and Scryfall CDN URL patterns are well-documented.

---

### Phase 3: Game Board UI

**Rationale:** The WebSocket protocol is defined in Phase 1; the frontend scaffold exists from Phase 2; now build the gameplay UI zone by zone. This is the highest-complexity frontend work. Build and verify each zone independently before integrating. The "looks done but isn't" checklist from PITFALLS.md is the acceptance criteria for this phase.

**Delivers:**
- `useGameSocket` hook with reconnection + sequence number tracking
- `gameStore` (Zustand) populated from WebSocket messages
- Game session lifecycle: lobby → WebSocket connect → GAME_STATE → game loop → GAME_OVER
- Zone components: `Battlefield`, `Hand`, `Graveyard`, `Exile`, `Stack`, `Library`
- `PhaseBar`, `PromptBar` (OK/Cancel per `updateButtons()`), `PlayerPanel` (life, mana pool)
- `ChoiceDialog` system mapping all ~15 `IGuiGame` choice signatures to UI
- Combat phase UI: attacker/blocker declaration with visual arrows, damage split dialog
- State resync endpoint `GET /api/game/{id}/state` + client-side resync on tab focus

**Addresses pitfalls:** Game state desync (#7); remaining `IGuiGame` coverage Tier 3-4 (#2)

**Verification (from PITFALLS.md checklist):** Basic mana payment, multi-blocker damage assignment, attacker ordering, "may" triggers, priority for stack responses, `finishGame()` for best-of-3, multi-select choices, zone browsing for search effects, browser refresh reconnection.

**Research flag:** NEEDS PHASE RESEARCH — choice dialog system mapping ~15 `IGuiGame` method signatures to React components has no prior art. The `assignCombatDamage` and `manipulateCardList` method signatures are particularly complex. Recommend enumerating all method signatures before planning sprint tasks.

---

### Phase 4: Deck Builder UI + Integration Polish

**Rationale:** The deck builder is REST-only and relatively independent. It can be developed in parallel with Phase 3 by a separate track, but integrating it (lobby pre-selects deck, play button launches game) requires both Phase 3 and Phase 4 to be complete. Polish (keyboard shortcuts, auto-yield, game log, animations) comes last.

**Delivers:**
- `DeckEditor` with card search panel, deck list, quantity controls, sideboard panel, Commander zone
- `CardSearch` with filters: name, type, color, CMC, format legality
- `DeckList` page (saved decks with create/load/delete)
- Mana curve bar chart + color distribution + card type breakdown
- Format validation display (legal/illegal indicators per card)
- Basic land quick-add panel
- Lobby screen: format picker, deck selector, AI difficulty
- Integrated build-then-play navigation (deck builder → lobby with pre-selected deck)
- v1.x additions: visual card grid view, deck import via text paste, game log panel, keyboard shortcuts, auto-yield/auto-pass settings, AI difficulty exposure

**Research flag:** Standard patterns. Deck builder UI is well-documented territory (Moxfield/Archidekt serve as UX benchmarks). No research phase needed.

---

### Phase Ordering Rationale

- **Phase 1 must be first** because five critical pitfalls are Phase 1 concerns and all game functionality depends on the bridge being correct. Starting UI before the bridge is proven wastes effort.
- **Phase 2 before Phase 3** because the frontend scaffold, MSW mocks, and Scryfall strategy established in Phase 2 are consumed by Phase 3. The card image component is shared infrastructure for both deck builder and game board.
- **Phase 3 before Phase 4 integration** because the integrated play loop (the core product differentiator) requires a working game board. The deck builder can be built in parallel but the lobby-to-game handoff tests Phase 3's match start flow.
- **Deck builder is lower risk than game board** — REST CRUD against Forge's existing deck storage is straightforward; the game board with its WebSocket state machine and 90+ `IGuiGame` methods is the hardest frontend work.

### Research Flags

**Needs deeper research during planning:**
- **Phase 1 — Forge engine input system:** `InputQueue`, `InputSyncronizedBase`, `FThreads`, `PlayerControllerHuman`, and the existing `TrackableSerializer` network play code need careful study before implementation. The threading model and input stack are underdocumented. Suggest a dedicated research spike reading the existing Forge network play module as the closest prior art.
- **Phase 3 — IGuiGame choice method catalog:** All ~15 `IGuiGame` blocking input methods (`getChoices`, `one`, `oneOrNone`, `many`, `order`, `confirm`, `chooseSingleEntityForEffect`, `assignCombatDamage`, `assignGenericAmount`, `manipulateCardList`, `sideboard`, `insertInList`) need their signatures mapped to React dialog components before sprint planning. Some return `Map<CardView, Integer>` — non-trivial to round-trip through JSON.

**Standard patterns (skip research-phase):**
- **Phase 2 — REST API:** Standard CRUD patterns. Javalin routing is well-documented.
- **Phase 2 — Scryfall CDN:** CDN image URL pattern is documented; bulk data download approach is established.
- **Phase 4 — Deck builder UI:** Moxfield and Archidekt serve as clear UX benchmarks. React table components, debounced search, and chart libraries are well-documented.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All versions confirmed from official release notes and changelogs. Jetty conflict verified by source code search. |
| Features | HIGH | Benchmarked against Moxfield, Archidekt, Arena, and Forge desktop. Feature scope is clear; the `IGuiGame` interface is the source of truth for gameplay capabilities. |
| Architecture | HIGH | Derived from direct analysis of Forge source (`IGuiGame`, `HostedMatch`, `PlayerControllerHuman`, `InputQueue`, `FThreads`). Patterns are proven in analogous projects (game clients with blocking engines). |
| Pitfalls | HIGH | Derived from direct codebase analysis, not speculation. Line counts, class names, and method signatures are cited. The threading and serialization pitfalls are grounded in actual Forge internals. |

**Overall confidence:** HIGH

### Gaps to Address

- **`FModel` initialization for headless web context:** `FModel.getPreferences()` and related desktop-context dependencies need a web-specific initialization path. The exact set of desktop preferences that require replacement is not fully catalogued. Address at the start of Phase 1 by running `WebServer.main()` and observing what breaks.
- **Forge's existing network play module as prior art:** Forge has `TrackableSerializer`/`TrackableDeserializer` for its own network play feature. This is an ordinal-based binary protocol, not JSON, but it solves the same circular reference and ID-based round-trip problem. Review it before designing the `GameStateDTO` layer to avoid reinventing what Forge's own team already solved.
- **`IGuiGame` Tier 4 method completeness:** The 90+ method count is known, but the exact set of Tier 4 (edge-case) methods that require real implementation vs. silent no-ops for v1 is not fully determined. The pitfalls research recommends tracking which methods are called during test games — this tracking mechanism needs to be built in Phase 1.
- **Deck storage file format compatibility:** Forge uses its own deck file format (`.dck` files via `DeckSerializer`). The REST API must read/write in this format. The exact JSON ↔ deck format mapping needs verification against `DeckSerializer.java` at the start of Phase 2.

## Sources

### Primary (HIGH confidence)

- Forge source code: `IGuiGame.java`, `IGuiBase.java`, `AbstractGuiGame.java`, `HostedMatch.java`, `PlayerControllerHuman.java` (3,487 lines), `InputSyncronizedBase.java`, `InputQueue.java`, `TrackableProperty.java` (180+ properties), `FThreads.java`, `CardView.java` (1,942 lines), `PlayerView.java`, `GameView.java`
- [React 19.2 announcement](https://react.dev/blog/2025/10/01/react-19-2)
- [Vite releases](https://vite.dev/releases)
- [TypeScript 6.0 RC announcement](https://devblogs.microsoft.com/typescript/announcing-typescript-6-0-rc/)
- [Tailwind CSS v4.0 blog](https://tailwindcss.com/blog/tailwindcss-v4)
- [Javalin 7.0 release](https://javalin.io/news/javalin-7.0.0-stable.html)
- [Jackson Release 2.21](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21)
- [Zustand GitHub](https://github.com/pmndrs/zustand)
- [TanStack Query releases](https://github.com/tanstack/query/releases)
- [Vitest GitHub](https://github.com/vitest-dev/vitest)

### Secondary (MEDIUM confidence)

- [Moxfield](https://moxfield.com/) — UX benchmark for deck builder features
- [Archidekt](https://archidekt.com/) — UX benchmark for deck builder features
- [MTG Arena Zone - Interface Guide](https://mtgazone.com/using-arena-interface-and-add-ons/) — gameplay UI patterns
- [react-use-websocket npm](https://www.npmjs.com/package/react-use-websocket) — 77K weekly downloads, active maintenance
- [Draftsim - Best MTG Deck Builder](https://draftsim.com/best-mtg-deck-builder/) — competitor feature comparison
- Scryfall API documentation — rate limits, bulk data, image URL patterns

### Tertiary (LOW confidence)

- [WebSocket game architecture patterns](https://dev.to/sauravmh/building-a-multiplayer-game-using-websockets-1n63) — general patterns, not Forge-specific
- [Lobby-based multiplayer browser games with React](https://riven.ch/en/news/build-lobby-based-online-multiplayer-browser-games-with-react-and-nodejs) — lobby UX patterns

---
*Research completed: 2026-03-16*
*Ready for roadmap: yes*
