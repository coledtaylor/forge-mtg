# Phase 12: Deck Simulation - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Build a headless AI vs AI simulation system triggered from the deck builder. Runs configurable numbers of games against a gauntlet of opponent decks, collects comprehensive statistics, computes Elo rating and playstyle classification, displays results in a live-updating tabbed dashboard, and persists results for later review.

</domain>

<decisions>
## Implementation Decisions

### Gauntlet Configuration
- Default: test against all available decks in the same format (user's decks + bundled AI decks)
- "Configure Gauntlet" button to select specific opponent decks (checkboxes)
- Games distributed evenly across opponents (100 games / 5 opponents = 20 per opponent)
- Both sides play at maximum AI strength (Hard/Reckless profile) — no AI handicap
- Configurable game count: 10, 50, 100, 500

### Results Dashboard
- Tabbed sections: Overview (Elo + playstyle radar + win rate + headline stats), Matchups (per-opponent breakdown), Performance (per-card win rate, dead cards), Mana (screw/flood, land drops)
- Each tab focused and scannable
- Results persisted as server-side JSON files alongside deck files (e.g., `sim-{deckname}-{timestamp}.json`)
- Backend API to list/load past simulation results for each deck
- User can view history of simulation runs for a deck

### Elo Rating
- Per-run Elo calculation (not cumulative across runs)
- Starting Elo = 1500
- Each game against each opponent adjusts Elo using standard formula
- Final Elo displayed prominently in Overview tab
- Compare Elo across runs to measure improvement

### Playstyle Classification
- Radar chart with 4 axes: Aggro, Midrange, Control, Combo
- Derived from combination of static deck analysis (from Phase 10) + simulation results (kill speed, interaction usage, card draw patterns)
- Shows how much the deck leans into each archetype

### Real-Time Progress
- Live updating dashboard while simulation runs
- Progress bar: "42/100 games complete"
- Running win rate updating as games finish
- Running Elo adjusting in real-time
- Matchup table filling in progressively
- Per-card stats accumulating
- WebSocket or SSE for progress updates from backend to frontend

### Cancel & Partial Results
- Cancel button stops remaining games
- Stats from completed games are kept and displayed
- Shows "42/100 games (cancelled)" with whatever data was gathered
- Partial results are still saved (persisted like complete results)

### Statistics Collected (per game)
- Winner, loser, game duration (turns)
- Play/draw assignment
- Mulligans taken by test deck
- Turn of first threat played
- Final life totals
- Cards drawn, cards stuck in hand (uncastable)
- For each card: drawn count, contributed-to-win signal

### Claude's Discretion
- HeadlessGuiGame no-op implementation details
- Bounded thread executor sizing (how many concurrent games)
- Game timeout ceiling (max turns before forced draw)
- Exact Elo K-factor
- Playstyle radar scoring algorithm details
- SSE vs WebSocket for progress streaming
- JSON result file structure and naming convention
- Per-card win correlation algorithm

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Engine Integration
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` — Reference for IGuiGame implementation (HeadlessGuiGame will be a no-op version)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` — handleStartGame() pattern for HostedMatch.startMatch()
- `forge-ai/src/main/java/forge/ai/AIOption.java` — AI profiles (use Reckless for Hard)

### Frontend
- `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` — Where "Simulate" button goes
- `forge-gui-web/frontend/src/components/deck-editor/StatsPanel.tsx` — Existing stats pattern
- `forge-gui-web/frontend/src/lib/deck-analysis.ts` — Static analysis functions (playstyle classification input)

### Research
- `.planning/research/STACK.md` — HostedMatch supports AI-only games, no new deps needed
- `.planning/research/PITFALLS.md` — GamePlayerUtil.guiPlayer singleton, ThreadUtil unbounded pool, simulation thread isolation
- `.planning/research/ARCHITECTURE.md` — HeadlessGuiGame pattern, simulation architecture

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `HostedMatch` + `RegisteredPlayer` — Pattern for starting games programmatically (already used in WebServer.handleStartGame)
- `LobbyPlayerAi` — AI player creation pattern
- `deck-analysis.ts` — Static analysis functions for playstyle baseline
- SVG chart components — ManaCurveChart, ColorDistribution, TypeBreakdown patterns for the radar chart
- `useJumpstartPacks` hook pattern — TanStack Query for API data fetching

### Established Patterns
- REST endpoints for CRUD (DeckHandler pattern for simulation results)
- WebSocket for real-time updates (gameWebSocket.ts pattern — but simulation may use SSE for simpler one-directional streaming)
- Zustand stores for complex state (gameStore.ts pattern for simulation state)
- Server-side JSON for persistence (existing deck .dck pattern, but JSON for structured data)

### Integration Points
- `DeckEditor.tsx` — "Simulate" button trigger
- New REST endpoints: POST /api/simulations/start, GET /api/simulations/{id}/status, GET /api/simulations/history/{deckName}
- New SSE or WebSocket endpoint for live progress streaming
- Backend: SimulationRunner service with bounded thread pool, separate from game thread pool

</code_context>

<specifics>
## Specific Ideas

- Live updating dashboard should feel like watching a machine learning training run — stats converging over time
- Elo gives a single number to compare decks ("is my 1650 deck better than my 1480 deck?")
- Playstyle radar helps understand WHY a deck wins or loses (aggressive? controlling? combo?)
- Cancelling early is valuable — "I can see after 20 games this deck is 15% win rate, no point running 80 more"
- Saved results enable tracking deck evolution over time ("v1 of my deck was 1400 Elo, v3 is 1620")

</specifics>

<deferred>
## Deferred Ideas

- Real-time AI spectating (watching games play out visually) — v3 feature
- Deck generation based on simulation results ("make me a deck that beats this gauntlet") — separate feature
- Cross-deck Elo leaderboard — future enhancement

</deferred>

---

*Phase: 12-deck-simulation*
*Context gathered: 2026-03-21*
