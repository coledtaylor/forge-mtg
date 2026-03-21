---
date: "2026-03-21 00:05"
promoted: false
---

Investigate running multiple simulation games in parallel to speed up deck testing. Currently SimulationRunner uses a single-thread executor. FModel (card database) is read-only after startup — safe to share. Each Game instance has its own state and should be independent. GamePlayerUtil.createAiPlayer creates separate LobbyPlayer instances per game. Main unknown: whether ThreadUtil.invokeInGameThread or other engine internals use shared mutable state that would corrupt under concurrent access. Proposed fix: change newSingleThreadExecutor() to newFixedThreadPool(2-4) in SimulationRunner.java. Worst case: occasional game crashes caught by existing error handling and skipped — acceptable noise for statistical aggregation across 50-100 games. Worth profiling memory per concurrent Game object to determine safe concurrency level.
