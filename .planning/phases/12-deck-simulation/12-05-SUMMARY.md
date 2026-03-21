---
phase: 12-deck-simulation
plan: 05
subsystem: ui
tags: [react, simulation, performance, mana-analysis, history]

requires:
  - phase: 12-deck-simulation (plan 02)
    provides: simulation-types.ts, useSimulation hook, elo.ts, simulation API
  - phase: 12-deck-simulation (plan 04)
    provides: OverviewTab, MatchupsTab, SimulationPanel tab structure
provides:
  - PerformanceTab with sortable per-card statistics table
  - ManaTab with screw/flood rates, land drop timing, resource stats
  - SimulationHistory component for past run management
  - Complete 5-tab simulation dashboard (Overview, Matchups, Performance, Mana, History)
affects: []

tech-stack:
  added: []
  patterns: [sortable-table-with-state, visual-bar-indicators, relative-timestamp-formatting]

key-files:
  created:
    - forge-gui-web/frontend/src/components/simulation/PerformanceTab.tsx
    - forge-gui-web/frontend/src/components/simulation/ManaTab.tsx
    - forge-gui-web/frontend/src/components/simulation/SimulationHistory.tsx
  modified:
    - forge-gui-web/frontend/src/components/simulation/SimulationPanel.tsx

key-decisions:
  - "History shown both on config screen and as 5th results tab for quick access"
  - "Per-card summary stats computed only from cards with sufficient data (3+ games)"
  - "Historical result viewing uses banner with back-to-latest button"

patterns-established:
  - "Sortable table: useState for sortKey/sortDir, clickable headers with arrow indicators"
  - "Visual health bars: red fill for screw, blue fill for flood, percentage width"

requirements-completed: [SIM-07, SIM-08, SIM-09]

duration: 2min
completed: 2026-03-21
---

# Phase 12 Plan 05: Performance, Mana, and History Tabs Summary

**Per-card performance table with sortable win/dead rates, mana consistency dashboard with screw/flood/land-drop/resource stats, and simulation history browser with load/delete**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-21T05:12:50Z
- **Completed:** 2026-03-21T05:14:54Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- PerformanceTab with sortable per-card table showing draw rate, win rate when drawn, and dead card rate with color coding
- ManaTab with mana screw/flood visual bars, land drop timing (3rd/4th land), and 2x2 resource stats grid
- SimulationHistory component with relative timestamps, Elo tiers, clickable entries, and delete buttons
- SimulationPanel fully wired with all 5 tabs and historical result viewing

## Task Commits

Each task was committed atomically:

1. **Task 1: PerformanceTab and ManaTab components** - `f785667` (feat)
2. **Task 2: SimulationHistory component and final SimulationPanel wiring** - `0e54627` (feat)

## Files Created/Modified
- `forge-gui-web/frontend/src/components/simulation/PerformanceTab.tsx` - Sortable per-card performance table with win rate and dead card detection
- `forge-gui-web/frontend/src/components/simulation/ManaTab.tsx` - Mana consistency, land drop timing, and resource statistics dashboard
- `forge-gui-web/frontend/src/components/simulation/SimulationHistory.tsx` - Past simulation results list with load and delete
- `forge-gui-web/frontend/src/components/simulation/SimulationPanel.tsx` - Wired all tabs and added history viewing with state management

## Decisions Made
- History shown both on config screen (for quick access to past results) and as 5th results tab
- Per-card summary stats (best/worst/most dead) computed only from cards with sufficient data (3+ games drawn) to avoid misleading stats
- Historical result viewing uses a yellow banner showing timestamp with "Back to latest" button

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 5 simulation dashboard tabs complete (Overview, Matchups, Performance, Mana, History)
- Phase 12 (Deck Simulation) is fully complete
- v2.0 milestone complete

---
*Phase: 12-deck-simulation*
*Completed: 2026-03-21*
