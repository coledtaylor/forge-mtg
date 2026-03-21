---
phase: 12-deck-simulation
plan: 01
subsystem: simulation
tags: [headless-game, ai-vs-ai, elo, statistics, java]

# Dependency graph
requires:
  - phase: 07-backend-enrichment
    provides: "CardDto, DeckSummaryDto for deck loading patterns"
provides:
  - "HeadlessGuiGame for AI-only headless games"
  - "SimulationRunner for sequential game orchestration"
  - "GameStatExtractor for per-game statistics extraction"
  - "EloCalculator for Elo rating computation"
  - "SimulationResult/SimulationSummary data models"
  - "SimulationJob for progress tracking and cancellation"
affects: [12-deck-simulation plan 02 (REST/SSE endpoints), 12-deck-simulation plan 03 (frontend dashboard)]

# Tech tracking
tech-stack:
  added: []
  patterns: [headless-gui-game, dedicated-simulation-thread, endgame-hook-synchronization]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/simulation/HeadlessGuiGame.java
    - forge-gui-web/src/main/java/forge/web/simulation/SimulationResult.java
    - forge-gui-web/src/main/java/forge/web/simulation/SimulationSummary.java
    - forge-gui-web/src/main/java/forge/web/simulation/EloCalculator.java
    - forge-gui-web/src/main/java/forge/web/simulation/GameStatExtractor.java
    - forge-gui-web/src/main/java/forge/web/simulation/SimulationJob.java
    - forge-gui-web/src/main/java/forge/web/simulation/SimulationRunner.java
  modified:
    - forge-gui-web/src/main/java/forge/web/WebGuiBase.java

key-decisions:
  - "endGameHook CompletableFuture used to synchronize game completion (avoids needing reference to internal HeadlessGuiGame)"
  - "HeadlessGuiGame overrides handleGameEvent as no-op (skips FControlGameEventHandler overhead for headless games)"
  - "emptyHandTurns uses -1 sentinel since reliable detection from GameLog text is limited"
  - "200-turn max for stalemate detection, marked as loss"

patterns-established:
  - "HeadlessGuiGame pattern: extend AbstractGuiGame, no-op all methods, CompletableFuture for game end signaling"
  - "SimulationRunner pattern: dedicated single-thread executor, endGameHook for synchronization, explicit GameRules construction"

requirements-completed: [SIM-03, SIM-07, SIM-10]

# Metrics
duration: 7min
completed: 2026-03-21
---

# Phase 12 Plan 01: Backend Simulation Engine Summary

**Headless AI-vs-AI game execution with per-game statistics extraction, Elo rating, and progress-tracking job system**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-21T04:49:05Z
- **Completed:** 2026-03-21T04:56:25Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- HeadlessGuiGame enables AI-only games by satisfying IGuiGame contract with no-op methods
- WebGuiBase.getNewGuiGame() returns HeadlessGuiGame instead of null, fixing NPE on AI-only game starts
- SimulationRunner orchestrates N games sequentially on dedicated thread with progress callbacks
- GameStatExtractor pulls winner, turns, mulligans, land drop timing, and card draw counts from engine objects
- EloCalculator computes Elo rating with K=32 from win/loss sequence (start 1500)
- SimulationSummary.computeFrom() aggregates all per-game results into dashboard-ready statistics

## Task Commits

Each task was committed atomically:

1. **Task 1: HeadlessGuiGame, data models, EloCalculator, and WebGuiBase fix** - `6a524061f8` (feat)
2. **Task 2: SimulationJob and SimulationRunner with progress listeners** - `8754813786` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/simulation/HeadlessGuiGame.java` - No-op IGuiGame for headless AI games with CompletableFuture game end signaling
- `forge-gui-web/src/main/java/forge/web/simulation/SimulationResult.java` - Immutable per-game result with all stat fields
- `forge-gui-web/src/main/java/forge/web/simulation/SimulationSummary.java` - Aggregated stats with computeFrom() factory, inner MatchupStats/CardPerformance classes
- `forge-gui-web/src/main/java/forge/web/simulation/EloCalculator.java` - Standard Elo formula with K=32, start=1500
- `forge-gui-web/src/main/java/forge/web/simulation/GameStatExtractor.java` - Extracts stats from GameOutcome/GameLog including land drop tracking
- `forge-gui-web/src/main/java/forge/web/simulation/SimulationJob.java` - Job lifecycle with progress listeners and cancel support
- `forge-gui-web/src/main/java/forge/web/simulation/SimulationRunner.java` - Sequential game executor on dedicated Simulation-Orchestrator thread
- `forge-gui-web/src/main/java/forge/web/WebGuiBase.java` - getNewGuiGame() returns HeadlessGuiGame instead of null

## Decisions Made
- Used endGameHook CompletableFuture for game synchronization rather than accessing the internal HeadlessGuiGame created by startGame() -- cleaner API boundary
- HeadlessGuiGame overrides handleGameEvent as no-op to skip FControlGameEventHandler overhead for headless games
- emptyHandTurns tracked as -1 sentinel since reliable per-turn hand state detection from GameLog text is limited (frontend shows N/A)
- Stalemate detection at 200 turns: games exceeding this are marked as losses rather than draws

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed unused imports causing checkstyle failure**
- **Found during:** Task 1 (HeadlessGuiGame)
- **Issue:** HeadlessGuiGame had unused imports for GameEventSpellAbilityCast and GameEventSpellRemovedFromStack
- **Fix:** Removed unused imports
- **Files modified:** HeadlessGuiGame.java
- **Verification:** mvn compile passes with checkstyle enabled
- **Committed in:** 6a524061f8 (part of Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Minor cleanup, no scope change.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All simulation Java classes compile and are ready for REST/SSE endpoint wiring (Plan 02)
- No references to FModel preferences in simulation code
- No references to GamePlayerUtil.getGuiPlayer() in simulation code
- Dedicated thread pool avoids conflict with interactive game threads

---
*Phase: 12-deck-simulation*
*Completed: 2026-03-21*

## Self-Check: PASSED
- All 7 created files exist on disk
- Both task commits found in git history (6a524061f8, 8754813786)
