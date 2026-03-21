---
phase: 12-deck-simulation
plan: 02
subsystem: api
tags: [rest, sse, javalin, tanstack-query, eventsource, elo, simulation]

requires:
  - phase: 12-01
    provides: "SimulationRunner, SimulationJob, SimulationSummary backend engine"
provides:
  - "REST endpoints for simulation start/cancel/status/history/delete"
  - "SSE streaming of live simulation progress"
  - "Result persistence as JSON files"
  - "Frontend TypeScript types matching backend JSON"
  - "API client functions for all simulation endpoints"
  - "useSimulation hook with SSE lifecycle management"
  - "Elo display utility (computeElo, eloTier)"
affects: [12-03, 12-04, 12-05]

tech-stack:
  added: []
  patterns: [SSE via Javalin SseClient for live streaming, EventSource hook with cleanup]

key-files:
  created:
    - forge-gui-web/src/main/java/forge/web/api/SimulationHandler.java
    - forge-gui-web/frontend/src/lib/simulation-types.ts
    - forge-gui-web/frontend/src/api/simulation.ts
    - forge-gui-web/frontend/src/hooks/useSimulation.ts
    - forge-gui-web/frontend/src/lib/elo.ts
  modified:
    - forge-gui-web/src/main/java/forge/web/WebServer.java

key-decisions:
  - "SSE progress listener registered on SimulationJob with cleanup on client disconnect"
  - "Result persistence triggered by progress listener detecting job completion"
  - "Simulation routes placed before deck {name} routes to avoid path conflicts"

patterns-established:
  - "SSE streaming: Javalin SseClient with keepAlive, progress/complete event names"
  - "Result persistence: sim-{deckName}-{timestamp}.json in deck directory"
  - "Frontend SSE: EventSource with named events, cleanup on unmount via useRef"

requirements-completed: [SIM-01, SIM-02, SIM-07, SIM-12]

duration: 5min
completed: 2026-03-21
---

# Phase 12 Plan 02: API + SSE Layer Summary

**REST endpoints for simulation CRUD, SSE streaming for live progress, frontend types/hooks/API client with EventSource lifecycle management**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T04:58:58Z
- **Completed:** 2026-03-21T05:04:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Full REST API: start (with gameCount validation), status, cancel, history, delete endpoints
- SSE endpoint streams live simulation progress with keepAlive and proper event naming
- Result persistence saves completed simulations as JSON files for history
- Frontend data layer: types, API client, SSE hook, and Elo utility all TypeScript-clean

## Task Commits

Each task was committed atomically:

1. **Task 1: SimulationHandler REST + SSE endpoints and result persistence** - `a1c1d09fb0` (feat)
2. **Task 2: Frontend types, API client, Elo utility, and SSE hook** - `e361307315` (feat)

## Files Created/Modified
- `forge-gui-web/src/main/java/forge/web/api/SimulationHandler.java` - REST handler with start, status, cancel, history, deleteResult methods + result persistence
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Added 5 REST routes + 1 SSE route for simulations
- `forge-gui-web/frontend/src/lib/simulation-types.ts` - TypeScript types matching backend JSON (SimulationProgress, MatchupStats, CardPerformance, etc.)
- `forge-gui-web/frontend/src/api/simulation.ts` - API client wrapping all 5 endpoints via fetchApi
- `forge-gui-web/frontend/src/hooks/useSimulation.ts` - SSE hook with EventSource lifecycle, TanStack Query for history
- `forge-gui-web/frontend/src/lib/elo.ts` - Elo computation and tier display utility

## Decisions Made
- SSE progress listener registered on SimulationJob with cleanup on client disconnect -- avoids memory leaks from abandoned connections
- Result persistence triggered by progress listener detecting job completion -- saves automatically without separate mechanism
- Simulation routes placed before deck `{name}` routes to avoid Javalin path parameter conflicts

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed unused Arrays import**
- **Found during:** Task 1 (SimulationHandler)
- **Issue:** Unused import flagged by checkstyle
- **Fix:** Removed `import java.util.Arrays`
- **Files modified:** SimulationHandler.java
- **Verification:** `mvn compile` passes with checkstyle
- **Committed in:** a1c1d09fb0 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Trivial cleanup. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- API layer complete and ready for UI components (Plans 03-05)
- SimulationProgress type includes all fields needed for dashboard, matchup table, and card performance views
- useSimulation hook ready to be consumed by simulation panel component

---
*Phase: 12-deck-simulation*
*Completed: 2026-03-21*

## Self-Check: PASSED

All 6 files verified on disk. Both task commits (a1c1d09fb0, e361307315) verified in git log.
