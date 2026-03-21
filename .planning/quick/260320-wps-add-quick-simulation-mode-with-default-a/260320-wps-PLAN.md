# Quick Plan: Add Quick Simulation Mode with Default AI Profile

## Context

The simulation system currently hardcodes the "Reckless" AI profile for both players in `SimulationRunner.runSingleGame()` (lines 135-138). Reckless does deep analysis and takes 2-3 minutes per game. The "Default" profile is faster and sufficient for rough evaluation. The change needs to flow through 6 files: the TypeScript type definition, the UI config component, the API layer, the hook, the backend handler, and the runner.

The data flow is:
1. `SimulationConfig` type (simulation-types.ts) -- add `aiProfile` field
2. `SimulationConfig.tsx` -- add a toggle between "Thorough" (Reckless) and "Quick" (Default)
3. `simulation.ts` API -- already passes full config, no change needed
4. `useSimulation.ts` hook -- already passes full config, no change needed
5. `SimulationHandler.start()` -- extract `aiProfile` from request body, pass to runner
6. `SimulationRunner` -- accept `aiProfile` parameter, use it in `createAiPlayer()` calls

## Tasks

### Task 1: Add aiProfile to frontend type and UI toggle [x]

<files>
forge-gui-web/frontend/src/lib/simulation-types.ts
forge-gui-web/frontend/src/components/simulation/SimulationConfig.tsx
</files>

<action>
Add an optional `aiProfile` field to the `SimulationConfig` interface in simulation-types.ts with type `'Reckless' | 'Default'` (no default value in the type -- the backend defaults).

In SimulationConfig.tsx, add a "Speed" section above the "Number of Games" section with two segmented buttons: "Quick" (maps to `'Default'`) and "Thorough" (maps to `'Reckless'`). Default selection should be "Thorough" to preserve current behavior. Include the selected aiProfile in the config object passed to `onStart`. Add a subtle help text below the toggle explaining: Quick runs faster but with less precise AI play.
</action>

<verify>
Run `cd forge-gui-web/frontend && npx tsc --noEmit` to confirm no type errors.
</verify>

<done>
- SimulationConfig type has optional aiProfile field with union type 'Reckless' | 'Default'
- UI shows a Speed toggle with Quick and Thorough options
- Thorough is selected by default
- The aiProfile value is included in the config passed to onStart
</done>

### Task 2: Wire aiProfile through backend handler and runner [x]

<files>
forge-gui-web/src/main/java/forge/web/api/SimulationHandler.java
forge-gui-web/src/main/java/forge/web/simulation/SimulationRunner.java
</files>

<action>
In SimulationHandler.start(), extract the `aiProfile` string from the request body. Validate it is either "Reckless" or "Default" (default to "Reckless" if absent or invalid). Pass it to SimulationRunner.startSimulation() as a new parameter.

In SimulationRunner, update the `startSimulation` method signature to accept an `aiProfile` String parameter. Thread it through to `runSimulation` and then to `runSingleGame`. In `runSingleGame`, replace the hardcoded `"Reckless"` strings on lines 135 and 138 with the aiProfile parameter in both `GamePlayerUtil.createAiPlayer()` calls.
</action>

<verify>
Build the backend module: `cd forge-gui-web && mvn compile -q` to confirm no compilation errors.
</verify>

<done>
- SimulationHandler extracts aiProfile from request, validates it, defaults to "Reckless"
- SimulationRunner.startSimulation accepts aiProfile parameter
- Both createAiPlayer calls in runSingleGame use the passed aiProfile instead of hardcoded "Reckless"
- Existing behavior is preserved when aiProfile is not sent (defaults to Reckless)
</done>
