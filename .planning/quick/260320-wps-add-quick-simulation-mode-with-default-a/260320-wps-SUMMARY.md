# Quick Task Summary: 260320-wps

## Add Quick Simulation Mode with Default AI Profile

**Status:** Complete
**Date:** 2026-03-20
**Tasks:** 2/2

## Changes

### Task 1: Frontend type and UI toggle
- Added optional `aiProfile` field (`'Reckless' | 'Default'`) to `SimulationConfig` type
- Added Speed section with Quick/Thorough segmented toggle above game count selector
- Thorough (Reckless) selected by default to preserve existing behavior
- Help text explains Quick runs faster but with less precise AI play
- Commit: `516b750` — Add AI speed toggle to simulation config UI

### Task 2: Backend handler and runner wiring
- `SimulationHandler.start()` extracts and validates `aiProfile` from request body, defaults to "Reckless"
- `SimulationRunner.startSimulation()` accepts `aiProfile` parameter, threads it through `runSimulation` and `runSingleGame`
- Both `createAiPlayer()` calls use the passed `aiProfile` instead of hardcoded "Reckless"
- Commit: `f90a0c7` — Wire aiProfile through backend handler and simulation runner

## Files Modified

- `forge-gui-web/frontend/src/lib/simulation-types.ts` (modified)
- `forge-gui-web/frontend/src/components/simulation/SimulationConfig.tsx` (modified)
- `forge-gui-web/src/main/java/forge/web/api/SimulationHandler.java` (modified)
- `forge-gui-web/src/main/java/forge/web/simulation/SimulationRunner.java` (modified)

## Validation

- Task 1: PASSED (`npx tsc --noEmit` — no errors)
- Task 2: PASSED (`mvn compile -q` — no errors)
