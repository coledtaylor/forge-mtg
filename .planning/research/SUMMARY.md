# Project Research Summary

**Project:** Forge Web Client v2.0
**Domain:** MTG digital client — gameplay UX polish, format support, deck simulation, card image quality
**Researched:** 2026-03-20
**Confidence:** HIGH

## Executive Summary

The v2.0 work builds on a validated React 19 + Javalin 7 foundation and adds four major capability areas: gameplay UX polish (keyboard shortcuts, game log, targeting feedback, priority clarity), card data enrichment (oracle text, image quality), an independent headless simulation subsystem, and Jumpstart format support. The research is grounded in direct codebase analysis of the live Forge engine, so most findings reflect verified API availability rather than speculation. The core insight across all four areas is that the Forge engine already has the infrastructure needed — `GameLog`, `ManaRefundService`, `AbstractGuiGame.autoYields`, `IGameController.undoLastAction()`, `StaticData.getSpecialBoosters()` — and v2.0 is primarily about wiring these existing capabilities to the web client, not building new engine logic.

The recommended build order is: backend DTO enrichment first (it unlocks multiple frontend features simultaneously), then frontend gameplay UX components, then Jumpstart as a self-contained vertical slice, and finally the simulation subsystem last because it carries the highest technical risk. The single new frontend dependency is `react-hotkeys-hook@5` for keyboard shortcut management. All other additions are vanilla TypeScript, existing engine APIs, and new REST endpoints. This is a lean, well-scoped upgrade cycle.

The primary risks are concentrated in the simulation subsystem: the `GamePlayerUtil.guiPlayer` static singleton must never be touched in headless code paths, `ThreadUtil`'s unbounded cached thread pool must not be used for simulation games, and `FModel` global preferences must not be mutated between runs. These are all avoidable with deliberate design choices made early in that phase. The Jumpstart format pitfall (no `GameType.Jumpstart` exists in the engine) is already known from v1.0 and the solution is established: treat it as a UI-only concept that starts a `GameType.Constructed` game with a merged 40-card deck. Undo must be scoped precisely to "Undo Last Spell" — the engine does not support general game-state rewind.

## Key Findings

### Recommended Stack

The v1.0 stack (React 19, TypeScript, Vite 8, Tailwind CSS 4, Zustand, TanStack Query, Javalin 7, Jackson 2.21) is unchanged. V2.0 adds exactly one new frontend dependency.

See [STACK.md](.planning/research/STACK.md) for full dependency table and version compatibility matrix.

**Core technologies:**
- `react-hotkeys-hook@5`: keyboard shortcut handling — React-lifecycle-aware, scoped per component, ~2KB; the only new package
- `GameLog` (engine, existing): Observable that already captures 18 typed log entry types; subscribe via Observer pattern in `WebGuiGame`
- `StaticData.getSpecialBoosters()` (engine, existing): returns `SealedTemplate` booster pack definitions for Jumpstart editions; already loaded at startup
- `HeadlessGuiGame` (new): no-op `IGuiGame` extending `AbstractGuiGame`; enables AI-vs-AI games with zero WebSocket output
- `SimulationRunner` (new): dedicated bounded `ExecutorService` for headless games; must be isolated from `ThreadUtil.gameThreadPool`
- Scryfall `/cards/named?exact={name}&set={setCode}&format=image`: faster, printing-controlled image URLs replacing name-only lookups on the game board

**What NOT to add:** charting libraries, SSE, WebSocket for simulation progress, new `GameType` enum values, `react-virtuoso` for game log.

### Expected Features

See [FEATURES.md](.planning/research/FEATURES.md) for full prioritization matrix, competitor analysis, and dependency graph.

**Must have — table stakes that make v1.0 feel unfinished:**
- Game log / action history — every MTG client has this; `GameLog` is already populated on the engine side
- Keyboard shortcuts — Enter/Space for OK, Escape for cancel, Z for undo; zero-dependency quick win
- Priority and phase clarity — pulsing ActionBar border, active-phase highlight on PhaseStrip, turn indicator
- Targeting feedback — green glow on valid targets, dim invalid, SVG arrows from source
- Oracle text display — extend `CardDto` with `oracleText` from `CardRules.getOracleText()`
- Card image quality — add `setCode` to `CardDto`; use Scryfall `set` param for consistent, controlled printings

**Should have — differentiators:**
- Advanced deck stats (removal/ramp/draw auto-detection from oracle text) — no free tool does this without manual tagging
- Goldfish / solitaire mode — lobby checkbox, pass-only AI; low effort, high value for combo testing
- Deck simulation (AI vs AI headless) — unique feature; batch win-rate testing against gauntlet; independent subsystem
- Auto-yield / auto-pass — per-phase stop toggles; `isUiSetToSkipPhase` is hardcoded to `false` in v1.0
- Undo (mana tapping only) — Z key + `ManaRefundService`; must be labeled "Undo Last Spell" precisely

**Defer to v2.x+:**
- Jumpstart pack builder full custom UI — valuable but large scope; after gameplay UX and simulation are solid
- Full game-state rewind — engine has no checkpoint/restore mechanism; explicitly anti-feature
- Animated card effects / VFX — enormous effort, slows experienced players
- Real-time multiplayer — completely different product architecture

### Architecture Approach

The architecture is additive: existing components (`WebGuiGame`, `GameStateDto`, `CardDto`, `WebServer`) are enriched, and five new backend classes plus six new frontend files are introduced. The key structural decision is keeping simulation entirely off the WebSocket — it uses REST with polling (TanStack Query `refetchInterval`) because it is a request-response pattern, not a bidirectional stream. The `GameLog` feeds the frontend via a new `GAME_LOG` WebSocket message using delta streaming (sequence-number-tracked), not full snapshots. The Jumpstart lobby flow branches by format at the `GameLobby` level; the engine never sees "Jumpstart" as a game type.

See [ARCHITECTURE.md](.planning/research/ARCHITECTURE.md) for the full system diagram, data flow diagrams, and code sketches for all integration points.

**Major components:**
1. `GameStateDto` enrichment — adds `priorityPlayerId`, `canUndo`, and `setCode` on `CardDto`; single backend change unlocks priority UI, undo visibility, and image quality simultaneously
2. New inbound WebSocket message handlers (`UNDO`, `PASS_UNTIL_EOT`, `SET_AUTO_YIELD`, `CONCEDE`) — all wire to existing `IGameController` APIs; no new engine logic
3. `GameLog` delta streaming — `WebGuiGame` subscribes as `Observer` to `GameLog`; sends `GAME_LOG` messages with only new entries per state change
4. `HeadlessGuiGame` + `SimulationRunner` — isolated from interactive game thread; dedicated bounded executor; REST-based job tracking via `ConcurrentHashMap<String, SimulationJob>`
5. `JumpstartHandler` — reads `StaticData.getSpecialBoosters()`, filters by edition, returns 6 random packs; game start maps to `GameType.Constructed`
6. Frontend: `GameLogPanel`, `PriorityIndicator`, `KeyboardShortcuts`, `JumpstartPackPicker`, `SimulationPage`, `SimulationResults`

**Build order (dependency-driven):**
1. `GameStateDto` enrichment (unlocks everything)
2. New inbound message handlers (engine APIs already exist)
3. `GameLog` streaming
4. Frontend UX components (depends on 1 + 2)
5. `GameLogPanel` (depends on 3)
6. Keyboard shortcuts (depends on 2 + 4)
7. Card image quality (depends on 1)
8. Jumpstart pack selection (independent)
9. Headless simulation (independent, highest risk)

### Critical Pitfalls

See [PITFALLS.md](.planning/research/PITFALLS.md) for all 6 critical pitfalls, technical debt patterns, performance traps, and the "looks done but isn't" checklist.

1. **Static `GamePlayerUtil.guiPlayer` singleton corrupts headless games** — never call `GamePlayerUtil.getGuiPlayer()` in simulation code; create `LobbyPlayerAi` instances directly; pass empty or dummy `guis` map to `HostedMatch`

2. **`ThreadUtil.gameThreadPool` is unbounded — simulation will OOM** — create a separate `ExecutorService` with a fixed-size pool bounded to CPU cores; never submit simulation work to `ThreadUtil.invokeInGameThread()`

3. **`FModel` global preferences must not be mutated between simulation runs** — build `GameRules` explicitly via `new GameRules(GameType.Constructed)` rather than `HostedMatch.getDefaultRules()`

4. **No `GameType.Jumpstart` in the engine — 40-card decks fail Constructed validation** — Jumpstart is a UI-only concept; merged 40-card deck must bypass standard deck-size validation; "Jumpstart" format label maps to `GameType.Constructed`

5. **Undo scope mismatch — engine only undoes the last stack item** — label the button "Undo Last Spell"; only show when `canUndoLastAction()` is true; do not expose `EXPERIMENTAL_RESTORE_SNAPSHOT`

6. **Priority prompts without phase context confuse players** — enrich every `BUTTON_UPDATE` with phase context; this is a prerequisite to auto-yield being useful; recovery cost is HIGH if deferred

## Implications for Roadmap

Based on dependency analysis across all four research files, the architecture drives a clear 5-phase build order. Phases 1 and 2 are sequential. Phases 3, 4, and 5 can proceed in parallel after Phase 1 lands, though Phase 3 depends on Phase 2 UX primitives.

### Phase 1: Backend Plumbing

**Rationale:** `GameStateDto` enrichment and new WebSocket message handlers are zero-dependency backend changes that unlock the entire frontend work stream. Nothing in Phase 2 can be built correctly without this foundation. Frontend components built against incomplete DTOs will require rework.

**Delivers:** Enriched `CardDto` (setCode, oracleText), enriched `GameStateDto` (priorityPlayerId, canUndo), 4 new inbound WebSocket message types (UNDO, PASS_UNTIL_EOT, SET_AUTO_YIELD, CONCEDE), `GAME_LOG` streaming wired to `GameLog` Observer

**Addresses:** Oracle text display, card image quality, undo visibility, priority clarity, game log (backend half of all P1 features)

**Avoids:** "Undo button always visible" pitfall by shipping `canUndo` state from day one; prevents frontend being built against stale DTO contracts

**Research flag:** Standard patterns — no deep research needed; all APIs verified directly in codebase

### Phase 2: Gameplay UX

**Rationale:** All frontend UX features can be built in parallel once Phase 1 lands. This phase delivers the highest user-visible impact per hour of effort. Priority/phase clarity and keyboard shortcuts have no internal dependencies, so they ship first within the phase.

**Delivers:** Pulsing ActionBar border, active-phase highlight on PhaseStrip, turn indicator, keyboard shortcuts (Enter/Space/Escape/Z), oracle text hover panel, direct Scryfall set URLs on game board, collapsible `GameLogPanel`, targeting feedback (green glow + SVG arrows), goldfish lobby checkbox, advanced deck stats (removal/ramp/draw density), `useKeyboardShortcuts` hook, `PriorityIndicator`, `GameLogPanel` components

**Uses:** `react-hotkeys-hook@5`; Phase 1 DTO changes; `deck-stats.ts` extended with oracle text pattern matching

**Avoids:** Priority confusion pitfall (phase context on every prompt); keyboard shortcut browser-default conflicts (avoid Ctrl+Z); game log as flat text dump (structured entries with turn/phase markers and filtering)

**Research flag:** Standard patterns — well-established React component patterns; no research phase needed

### Phase 3: Advanced Gameplay Engine Integration

**Rationale:** Auto-yield and undo require engine integration and carry moderate risk. They depend on Phase 2 UX primitives (phase stop indicators, keyboard shortcuts) being in place first. Sequencing this after Phase 2 lets the team validate the foundation before touching engine integration.

**Delivers:** Per-phase auto-yield toggles on `PhaseStrip`, `SET_AUTO_YIELD` handler overriding `isUiSetToSkipPhase` (stored in `Set<PhaseType>` on `WebGuiGame`), `PASS_UNTIL_EOT` handler, "Undo Last Spell" button wired to `ManaRefundService`, `CONCEDE` message handler

**Avoids:** Auto-yield implemented as WebSocket round-trip (must be pure local `Set<PhaseType>` check — no network latency in game loop); undo expectations mismatch (conditionally hidden, precisely labeled); auto-yield not canceling when opponent plays a spell during auto-yielded phase

**Research flag:** Targeted research recommended on `PlayerControllerHuman` undo constraints (lines 2360-2384) and `AbstractGuiGame.autoYields` edge-case behavior before sprint planning

### Phase 4: Jumpstart Format

**Rationale:** Fully independent of Phases 2 and 3. Engine APIs are verified. The 40-card validation bypass approach is established from pitfalls research. This is a self-contained vertical slice that can be worked in parallel with Phase 3.

**Delivers:** `GET /api/jumpstart/packs` returning 6 random themed packs per edition, `JumpstartPackPicker` component, `GameLobby` format-conditional branching, merged 40-card deck creation via `POST /api/decks`, correct `GameType.Constructed` mapping bypassing size validation

**Avoids:** Adding `GameType.Jumpstart` to the engine; Jumpstart decks polluting Constructed deck list; pack selection showing raw card lists (show theme name, color identity, key cards preview); land count mismatch in merged decks

**Research flag:** Verify `StaticData.getSpecialBoosters()` returns the correct edition codes for Jumpstart 2022 and Foundations Jumpstart before implementation — confirm pack template names match expected display labels

### Phase 5: Headless Deck Simulation

**Rationale:** Most isolated, most new code, highest technical risk. Building this last means the interactive game is stable and fully polished before introducing the simulation thread pool. All three critical simulation pitfalls (static singleton, unbounded thread pool, FModel mutations) must be addressed at design time. This is the project's unique killer feature and deserves focused, unhurried implementation.

**Delivers:** `HeadlessGuiGame` (no-op `IGuiGame`), `SimulationRunner` with bounded `ExecutorService`, `SimulationHandler` (`POST /api/simulate`, `GET /api/simulate/{id}`), `SimulationJob` with `ConcurrentHashMap` job tracking, `SimulationPage` and `SimulationResults` frontend components, TanStack Query polling at 2-second intervals, per-matchup win/loss/draw stats with average turn count

**Avoids:** Static `guiPlayer` singleton contaminating headless games; `ThreadUtil.gameThreadPool` OOM; `FModel` preference corruption between runs; simulation WebSocket spam (headless games produce zero messages); game object memory leaks (dereference `Game` immediately after result extraction)

**Research flag:** Needs a targeted research phase before implementation — verify `HostedMatch.startMatch()` signature behavior with empty `guis` map (check line 249 `humanCount == 0` branch does not hit null `GuiBase.getInterface()`); confirm simulation thread isolation approach is compatible with `ThreadUtil` design

### Phase Ordering Rationale

- Phase 1 before Phase 2: frontend components cannot be built correctly against stale DTOs; backend changes are zero-dependency and fast to ship
- Phase 3 after Phase 2: auto-yield depends on PhaseStrip stop indicators; undo button depends on keyboard infrastructure
- Phase 4 after Phase 1: needs enriched `CardDto` for pack card display; otherwise independent of Phases 2-3
- Phase 5 last: highest risk, most new code; isolating it reduces blast radius if simulation architecture needs revision; interactive game must be stable before introducing simulation thread pool
- Phases 4 and 5 can run in parallel after Phase 1 if team capacity allows; Phase 3 and 4 can run in parallel

### Research Flags

Phases needing deeper research during planning:
- **Phase 3 (Advanced Gameplay):** Targeted codebase research on `PlayerControllerHuman` undo constraints and `AbstractGuiGame.autoYields` edge-case behavior (opponent plays spell during auto-yielded end step; re-enable semantics)
- **Phase 5 (Simulation):** Verify `HostedMatch.startMatch()` behavior with empty `guis` map; confirm simulation thread isolation is compatible with `ThreadUtil` design; determine safe game timeout for degenerate AI loops

Phases with standard patterns (no research phase needed):
- **Phase 1 (Backend Plumbing):** All APIs directly verified in codebase; DTO extension is well-understood Java pattern
- **Phase 2 (Gameplay UX):** Standard React component patterns; `react-hotkeys-hook` is well-documented
- **Phase 4 (Jumpstart):** Engine API verified; format mapping approach established from pitfalls research

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Single new dependency (`react-hotkeys-hook@5`) with verified React 19 compatibility; all other technologies unchanged from validated v1.0 stack |
| Features | HIGH | Benchmarked against Arena, MTGO, Forge Desktop, and Moxfield; features grounded in competitor analysis and direct Forge source inspection |
| Architecture | HIGH | Based on direct codebase analysis of all relevant Forge modules; component boundaries and data flows verified against live code, not assumptions |
| Pitfalls | HIGH | All 6 critical pitfalls derived from direct source code inspection with specific file names and line numbers; not theoretical risks |

**Overall confidence: HIGH**

### Gaps to Address

- **`HostedMatch.startMatch()` with empty guis map:** The architecture recommends passing an empty map for headless games, but the exact behavior of the `humanCount == 0` branch (line 249) needs verification before the simulation runner is designed. If it hits a null `GuiBase.getInterface()` call, the `HeadlessGuiGame` approach requires adjustment.

- **Scryfall set code mismatches:** Forge set codes do not always align with Scryfall codes. The scope of mismatches is unknown until tested against the card database. A small lookup table of known exceptions may be needed in `scryfall.ts`.

- **`CardView.getCurrentState().getSetCode()` coverage for tokens and copies:** Tokens, emblems, and copy effects may not have a standard `setCode`. The frontend image URL construction needs a fallback chain (set URL -> name URL -> placeholder) that handles these edge cases without breaking image display.

- **Simulation game timeout:** A safe timeout for a single AI-vs-AI game is estimated at ~2 seconds, but degenerate loops (infinite triggers, infinite mana combos) could run indefinitely. A configurable timeout ceiling needs to be established and tested before shipping.

- **Jumpstart pack edition codes:** `StaticData.getSpecialBoosters()` must return templates for the expected Jumpstart editions (Jumpstart 2022, Foundations Jumpstart). The exact edition code strings used as filter keys need verification against the loaded booster data at startup.

## Sources

### Primary (HIGH confidence)

- Forge engine source: `GameLog.java`, `GameLogEntryType.java` — 18 typed log entry types, Observable pattern confirmed
- Forge engine source: `IGameController.java` — `undoLastAction()`, `concede()`, `passPriorityUntilEndOfTurn()` verified
- Forge engine source: `AbstractGuiGame.java` — `autoYields` Set, `shouldAutoYield(key)`, `autoPassUntilEndOfTurn` confirmed
- Forge engine source: `GameType.java` — no Jumpstart enum entry confirmed
- Forge engine source: `HostedMatch.java` — startGame flow, humanCount check at line 249
- Forge engine source: `CardView.CardStateView.getSetCode()` — set code available without engine changes
- Forge engine source: `StaticData.getSpecialBoosters()` — Jumpstart pack templates confirmed
- Forge engine source: `ManaRefundService.java`, `ManaPool.refundMana()`, `PhaseHandler.undoTap` — undo mechanism verified
- Forge engine source: `ThreadUtil.java` — unbounded cached thread pool at line 23 confirmed
- Forge engine source: `GamePlayerUtil.java` — static singleton guiPlayer at line 22 confirmed
- Forge web source: `WebGuiGame.java` line 868 — `isUiSetToSkipPhase` hardcoded to `false` confirmed
- [react-hotkeys-hook npm](https://www.npmjs.com/package/react-hotkeys-hook) — v5.x, 707+ dependents, React 19 compatible
- [Scryfall API: Cards by Name](https://scryfall.com/docs/api/cards/named) — `set` query parameter behavior confirmed
- [Scryfall API: Card Imagery](https://scryfall.com/docs/api/images) — image versions and URL format

### Secondary (MEDIUM confidence)

- [MTG Arena Keyboard Shortcuts - Draftsim](https://draftsim.com/mtg-arena-keyboard-shortcuts/) — Arena shortcut set used as benchmark
- [Arena Hot Keys and Interface Guide - MTG Arena Zone](https://mtgazone.com/arena-hot-keys-and-interface-guide-simplify-your-game-with-these-easy-tricks/) — priority UX patterns
- [MTGO Getting Started Gameplay](https://www.mtgo.com/getting-started/getting-started-gameplay) — MTGO priority UX patterns
- [5 Best Deck Testers for Magic - Draftsim](https://draftsim.com/mtg-deck-tester/) — competitor feature analysis for simulation differentiator
- [Jumpstart Format - MTG Wiki](https://mtg.fandom.com/wiki/Jumpstart_(format)) — format rules and pack construction rules
- [Foundations Jumpstart on Arena](https://magic.wizards.com/en/news/mtg-arena/foundations-jumpstart) — Arena Jumpstart UX reference

---
*Research completed: 2026-03-20*
*Ready for roadmap: yes*
