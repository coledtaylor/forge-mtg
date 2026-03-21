---
status: awaiting_human_verify
trigger: "Multiple bugs in deck simulation — wrong opponent filtering, percentage math, broken charts, wrong stat extraction"
created: 2026-03-20T00:00:00Z
updated: 2026-03-21T01:00:00Z
---

## Current Focus

hypothesis: All remaining bugs fixed — getRegisteredPlayers, radar viewBox, mana screw threshold, UX labels
test: Manual verification
expecting: All data correct, labels descriptive, tooltips present, radar chart visible
next_action: Awaiting user verification

## Symptoms

expected: Simulation tests deck against same-format opponents only. Win rates display as real percentages (50%, 100%). Charts render correctly with labels. Stats are accurate.
actual: 8 bugs — cross-format opponents, ratio displayed as percent without *100, wrong stat extraction, collapsed radar chart, non-proportional bars
errors: No error messages — data is wrong but doesn't crash
reproduction: Run any simulation from deck builder
started: First time using simulation feature (Phase 12)

## Eliminated

- hypothesis: Life at loss reads wrong player
  evidence: Actually reads correct player (testPlayer), but game.getPlayers() excludes losing players from ingamePlayers list. Life defaults to 20 because loop never matches.
  timestamp: 2026-03-21

- hypothesis: cardsDrawn zone calculation is wrong formula
  evidence: Formula is correct (deckSize - openingHand - remainingLibrary) but zone reading failed because losing player not in game.getPlayers(). Same root cause as life bug.
  timestamp: 2026-03-21

- hypothesis: Mana screw definition is too strict
  evidence: The definition is reasonable (3rd land by turn 4) but thirdLandTurn was always -1 because land log parsing used testPlayerName which was null (losing player excluded from getPlayers()). Additionally, games ending before turn 4 should not count.
  timestamp: 2026-03-21

## Evidence

- timestamp: 2026-03-20T00:01:00Z
  checked: SimulationSummary.computeFrom() rate computations
  found: All rates computed as 0-1 ratios
  implication: Frontend displays 0.5 as "0.5%" instead of "50%" — fixed by *100

- timestamp: 2026-03-20T00:01:00Z
  checked: SimulationHandler.collectAllDecks()
  found: Backend loads ALL .dck files with no format filter
  implication: Commander decks in gauntlet — fixed by sending format-filtered names from frontend

- timestamp: 2026-03-20T00:01:00Z
  checked: SimulationSummary.computeFrom() playstyle map
  found: Never populated
  implication: Collapsed radar — fixed by adding heuristic computation

- timestamp: 2026-03-21
  checked: Game.getPlayers() vs Game.getRegisteredPlayers()
  found: getPlayers() returns only surviving players (ingamePlayers). Losing players removed at game end.
  implication: ALL stat extraction failed for lost games: life defaulted to 20, cardsDrawn was 0, testPlayerName was null causing all log matches to fail (thirdLandTurn always -1, mana screw always triggered)

- timestamp: 2026-03-21
  checked: PlaystyleRadar SVG viewBox and label positioning
  found: viewBox 200x200 but labels at edge (x/y ~2.4). CSS class fill-muted-foreground may not render in SVG.
  implication: Labels clipped or invisible

## Resolution

root_cause: |
  Primary: Using game.getPlayers() (excludes losing players) instead of game.getRegisteredPlayers() (all players) in GameStatExtractor. This caused life to default to 20 on losses, cardsDrawn to be 0, testPlayerName to be null (breaking all log parsing including land drops), and mana screw to always trigger.
  Secondary: Radar chart viewBox too small (200x200) for labels at distance 97.6 from center. Labels used CSS class that may not render in SVG.
  Also: manaScrew counted games shorter than 4 turns as screwed.

fix: |
  1. GameStatExtractor: Switch all game.getPlayers() to game.getRegisteredPlayers()
  2. SimulationSummary: Only count mana screw for games lasting 4+ turns
  3. PlaystyleRadar: Expand viewBox to 260x260, use fill="currentColor" opacity={0.6}, add minimum polygon size
  4. OverviewTab: Rename labels (On Play -> Win Rate Going First, etc.), add title tooltips
  5. ManaTab: Rename labels (Avg Cards Drawn -> Cards Used From Library, etc.), add title tooltips

verification: Backend compiles (mvn compile). Frontend type-checks (tsc --noEmit).

files_changed:
  - forge-gui-web/src/main/java/forge/web/simulation/GameStatExtractor.java
  - forge-gui-web/src/main/java/forge/web/simulation/SimulationSummary.java
  - forge-gui-web/frontend/src/components/simulation/PlaystyleRadar.tsx
  - forge-gui-web/frontend/src/components/simulation/OverviewTab.tsx
  - forge-gui-web/frontend/src/components/simulation/ManaTab.tsx
