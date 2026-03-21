# Roadmap: Forge Web Client

## Milestones

- ✅ **v1.0 MVP** — Phases 1-6 (shipped 2026-03-20)
- **v2.0 Polish, Formats & Simulation** — Phases 7-12 (in progress)

## Phases

<details>
<summary>v1.0 MVP (Phases 1-6) — SHIPPED 2026-03-20</summary>

- [x] Phase 1: Engine Bridge (3/3 plans) — completed 2026-03-19
- [x] Phase 2: REST API + Frontend Scaffold (2/2 plans) — completed 2026-03-19
- [x] Phase 3: Deck Builder (3/3 plans) — completed 2026-03-19
- [x] Phase 4: Game Board (4/4 plans) — completed 2026-03-19
- [x] Phase 5: Game Setup + Integration (2/2 plans) — completed 2026-03-20
- [x] Phase 6: Deck Import & Export (2/2 plans) — completed 2026-03-20

Full details: `milestones/v1.0-ROADMAP.md`

</details>

### v2.0 Polish, Formats & Simulation

- [ ] **Phase 7: Backend DTO Enrichment & Tech Debt** - Enrich CardDto and GameStateDto with fields that unlock card quality, priority, and undo; fix v1.0 tech debt
- [x] **Phase 8: Gameplay UX** - Priority clarity, targeting feedback, game log, keyboard shortcuts, AI difficulty, goldfish mode, oracle text display (completed 2026-03-20)
- [ ] **Phase 9: Engine Integration UX** - Auto-yield for specific phases and undo last spell, requiring deeper engine wiring
- [x] **Phase 10: Advanced Deck Stats** - Oracle-text-based deck analysis with removal, ramp, interaction, and win condition metrics (completed 2026-03-21)
- [x] **Phase 11: Jumpstart Format** - Pack creation, pack browsing, dual-pack game setup as a self-contained vertical slice (completed 2026-03-21)
- [ ] **Phase 12: Deck Simulation** - Headless AI vs AI games with configurable gauntlet, real-time progress, and comprehensive statistics

## Phase Details

### Phase 7: Backend DTO Enrichment & Tech Debt
**Goal**: Backend data contracts are enriched so that downstream phases can build correct frontend features without rework, and v1.0 tech debt is resolved
**Depends on**: Phase 6 (v1.0 complete)
**Requirements**: CARD-01, CARD-02, CARD-03, DEBT-01, DEBT-02, DEBT-03
**Success Criteria** (what must be TRUE):
  1. Card images on the game board and deck builder load via direct Scryfall set/collector-number URLs (not name-based lookup)
  2. Card images consistently show recent, English-only, recognizable printings rather than obscure foreign variants
  3. Format validation returns 200 for "Casual 60-card" and "Jumpstart" format strings (no more 400 errors)
  4. GameStartConfig is defined in exactly one place and imported everywhere it is used
  5. AI deck selection provides a real deck for every supported format (no 60-Forests fallback)
**Plans:** 2/3 plans executed
Plans:
- [ ] 07-01-PLAN.md — Card DTO enrichment with preferred-printing resolution and frontend Scryfall URL update
- [ ] 07-02-PLAN.md — Format validation fix for Casual/Jumpstart and GameStartConfig consolidation
- [ ] 07-03-PLAN.md — Bundled AI decks for all formats and 60-Forests fallback removal

### Phase 8: Gameplay UX
**Goal**: Users can play games with clear priority information, visual targeting feedback, a readable action log, keyboard shortcuts, and flexible game setup options
**Depends on**: Phase 7
**Requirements**: GUX-01, GUX-02, GUX-03, GUX-04, GUX-05, GUX-07, GUX-08, CARD-04
**Success Criteria** (what must be TRUE):
  1. User can see at a glance whose turn it is, which phase is active, and whether they currently hold priority (pulsing indicator + phase highlight)
  2. User can distinguish between confirming an action (OK) and passing priority (Pass) through visually distinct, clearly labeled buttons
  3. User can see highlighted valid targets when choosing targets for a spell, with confirm/cancel to exit targeting mode
  4. User can read a scrollable game log showing every game action in chronological order with turn and phase markers
  5. User can use keyboard shortcuts (Enter/Space for OK, Escape for cancel) to play without touching the mouse for common actions
**Plans:** 4/4 plans complete
Plans:
- [ ] 08-01-PLAN.md — Backend infrastructure: GameLog streaming, choiceIds, goldfish AI + frontend data layer
- [ ] 08-02-PLAN.md — ActionBar redesign with priority indicator, Confirm/Pass split, keyboard shortcuts, goldfish lobby
- [ ] 08-03-PLAN.md — Game log panel with tabbed Stack/Log + oracle text in hover previews
- [ ] 08-04-PLAN.md — Targeting UX with card ID matching, visual highlights, multi-target badges

### Phase 9: Engine Integration UX
**Goal**: Users can automate repetitive priority passes and undo mana-tapping mistakes, smoothing out the pace of gameplay
**Depends on**: Phase 8
**Requirements**: GUX-06, GUX-09
**Success Criteria** (what must be TRUE):
  1. User can toggle auto-yield for specific phases (e.g., always pass upkeep) and the game skips those priority windows without prompting
  2. User can click "Undo Last Spell" (or press Z) to reverse the last cast when the engine supports it, and the button is hidden when undo is unavailable
**Plans:** 1/2 plans executed
Plans:
- [ ] 09-01-PLAN.md — Backend auto-pass logic, canUndo in BUTTON_UPDATE, UNDO/SET_AUTO_PASS message handlers
- [ ] 09-02-PLAN.md — Frontend undo button, Z hotkey, auto-pass toggle, phase strip flash animation

### Phase 10: Advanced Deck Stats
**Goal**: Users can see deep analytical metrics about their deck's composition without manual card-by-card evaluation
**Depends on**: Phase 7
**Requirements**: STATS-01, STATS-02, STATS-03, STATS-04
**Success Criteria** (what must be TRUE):
  1. User can see removal count, ramp density, and card draw source count computed automatically from oracle text analysis
  2. User can see interaction range analysis showing whether the deck can answer creatures, enchantments, artifacts, and graveyards
  3. User can see consistency metrics (4-of ratio, tutor count, threat redundancy) and win condition analysis (distinct win cons, redundancy assessment)
**Plans:** 2/2 plans complete
Plans:
- [ ] 10-01-PLAN.md — Backend DTO enrichment (oracleText/power/toughness) and deck-analysis.ts computation engine
- [ ] 10-02-PLAN.md — CompositionBreakdown, InteractionGrid components and StatsPanel wiring

### Phase 11: Jumpstart Format
**Goal**: Users can build Jumpstart packs, browse existing packs, and start a Jumpstart game by merging two packs into a 40-card deck
**Depends on**: Phase 7
**Requirements**: JUMP-01, JUMP-02, JUMP-03, JUMP-04, JUMP-05
**Success Criteria** (what must be TRUE):
  1. User can create a 20-card Jumpstart pack in the deck builder with proper format constraints
  2. User can browse Forge's existing Jumpstart pack definitions and select from them
  3. User can select two packs in game setup, see the merged 40-card deck, and start a game where the AI also selects two packs
  4. Game setup validates that exactly two packs are selected and prevents starting with fewer or more
**Plans:** 3/3 plans complete
Plans:
- [ ] 11-01-PLAN.md — Backend Jumpstart pack API endpoint and frontend type contracts
- [ ] 11-02-PLAN.md — Deck builder Jumpstart format support (format option, sideboard hiding)
- [ ] 11-03-PLAN.md — Jumpstart lobby UI (dual pack picker) and backend pack merge

### Phase 12: Deck Simulation
**Goal**: Users can test their deck's strength by running headless AI vs AI simulations against a gauntlet and reviewing comprehensive performance statistics
**Depends on**: Phase 8
**Requirements**: SIM-01, SIM-02, SIM-03, SIM-04, SIM-05, SIM-06, SIM-07, SIM-08, SIM-09, SIM-10, SIM-11, SIM-12
**Success Criteria** (what must be TRUE):
  1. User can trigger a simulation from the deck builder, configure the number of games (10-500), and see progress updating in real time as games complete
  2. User can see overall win rate, per-matchup win rate, and win rate on play vs draw after a simulation completes
  3. User can see mulligan stats (keep rate, avg mulligans), speed stats (avg kill turn, fastest/slowest win), and mana stats (screw/flood rate, land drop timing)
  4. User can see per-card performance (win rate when drawn, dead card rate) and resource stats (cards drawn, empty hand turns, life totals)
  5. User can see an Elo rating for the deck and a play style classification (aggro/midrange/control/combo) derived from simulation results
**Plans:** 4/5 plans executed
Plans:
- [ ] 12-01-PLAN.md — Backend simulation engine: HeadlessGuiGame, SimulationRunner, GameStatExtractor, EloCalculator
- [ ] 12-02-PLAN.md — Backend REST/SSE API endpoints + frontend types, API client, SSE hook
- [ ] 12-03-PLAN.md — Frontend simulation config, progress UI, and DeckEditor integration
- [ ] 12-04-PLAN.md — Frontend Overview tab (Elo, playstyle radar, headline stats) + Matchups tab
- [ ] 12-05-PLAN.md — Frontend Performance tab, Mana tab, and SimulationHistory

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Engine Bridge | v1.0 | 3/3 | Complete | 2026-03-19 |
| 2. REST API + Frontend Scaffold | v1.0 | 2/2 | Complete | 2026-03-19 |
| 3. Deck Builder | v1.0 | 3/3 | Complete | 2026-03-19 |
| 4. Game Board | v1.0 | 4/4 | Complete | 2026-03-19 |
| 5. Game Setup + Integration | v1.0 | 2/2 | Complete | 2026-03-20 |
| 6. Deck Import & Export | v1.0 | 2/2 | Complete | 2026-03-20 |
| 7. Backend DTO Enrichment & Tech Debt | 2/3 | In Progress|  | - |
| 8. Gameplay UX | 4/4 | Complete   | 2026-03-20 | - |
| 9. Engine Integration UX | 1/2 | In Progress|  | - |
| 10. Advanced Deck Stats | 1/2 | Complete    | 2026-03-21 | - |
| 11. Jumpstart Format | 3/3 | Complete    | 2026-03-21 | - |
| 12. Deck Simulation | 4/5 | In Progress|  | - |
