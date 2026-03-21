---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Polish, Formats & Simulation
status: verifying
stopped_at: Phase 10 context gathered
last_updated: "2026-03-21T02:51:13.686Z"
last_activity: 2026-03-21 — Auto-pass toggle, undo button, Z hotkey, phase flash (Phase 9 Plan 2)
progress:
  total_phases: 6
  completed_phases: 3
  total_plans: 9
  completed_plans: 9
  percent: 100
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-20)

**Core value:** Build a deck in the browser and play a full game of Magic against the AI
**Current focus:** Phase 9 — Engine Integration UX

## Current Position

Phase: 9 of 12 (Engine Integration UX) -- COMPLETE
Plan: 2 of 2 in current phase
Status: Phase 9 complete (awaiting human verification of auto-pass + undo)
Last activity: 2026-03-21 — Auto-pass toggle, undo button, Z hotkey, phase flash (Phase 9 Plan 2)

Progress: [██████████] 100% (v2.0 Phase 9)

## Performance Metrics

**v1.0 Summary:**
- 6 phases, 16 plans, 108 commits
- 11,344 LOC (4,643 Java + 6,701 TypeScript)
- 3 days (2026-03-18 to 2026-03-20)

**v2.0:**

| Phase | Plan | Duration | Tasks | Files |
|-------|------|----------|-------|-------|
| 07 | 01 | 12min | 2 | 10 |
| 07 | 02 | 3min | 2 | 6 |
| 07 | 03 | 6min | 2 | 10 |
| 08 | 01 | 8min | 2 | 7 |
| 08 | 02 | 4min | 2 | 5 |
| 08 | 03 | 6min | 2 | 9 |
| 08 | 04 | 32min | 3 | 8 |
| 09 | 01 | 4min | 2 | 3 |
| Phase 09 P02 | 2min | 2 tasks | 6 files |

## Accumulated Context

### Decisions

- [v2.0 roadmap]: Backend DTO enrichment ships first (shared dependency unlocking card quality, priority, undo)
- [v2.0 roadmap]: Simulation is Phase 12 (highest risk, most new code, last on stable foundation)
- [v2.0 roadmap]: Jumpstart uses GameType.Constructed with UI-layer pack merge (no engine changes)
- [07-01]: CardDto uses CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY for preferred-printing resolution
- [07-02]: Custom format handlers intercept before FModel.getFormats().get() for non-engine formats
- [07-02]: GameStartConfig re-exported from original locations for backwards compatibility
- [Phase 07]: Bundled AI decks use no-overwrite install pattern allowing user customization
- [08-01]: Delta-based log streaming via lastLogIndex counter instead of Observable pattern
- [08-01]: choiceIds sent alongside display strings in PROMPT_CHOICE for direct card ID matching
- [08-01]: clearButtons called after sendButtonOk/sendButtonCancel for immediate waiting state
- [08-02]: CSS @keyframes priority-pulse injected via style tag (component-scoped, no build tooling changes)
- [08-02]: Keyboard shortcuts disabled during PROMPT_CHOICE and PROMPT_AMOUNT to prevent accidental confirmations
- [08-02]: Goldfish mode hides AI deck picker entirely (cleaner UX for solitaire)
- [08-03]: Hover callbacks pass full CardDto instead of card name string for oracle text access
- [08-03]: StackPanel reuses shared GameHoverPreview instead of its own StackHoverPreview
- [08-04]: Targeting activates when choiceIds contain battlefield/hand card IDs, falls back to ChoiceDialog otherwise
- [08-04]: Tasks 1+2 merged into single commit since selectionIndex and selected-target were inseparable from targeting logic
- [08-04]: chooseSingleEntityForEffect and chooseEntitiesForEffect needed choiceIds for entity-based targeting prompts
- [08-04]: ActionBar suppresses ChoiceDialog during targeting mode to avoid duplicate controls
- [08-04]: Goldfish DOES_NOTHING implemented via doesNothing flag on PlayerControllerAi
- [Phase 09-01]: Auto-pass uses HostedMatch.getGame() lazy resolution instead of eager Game reference
- [Phase 09-01]: Human player identified via PlayerControllerHuman instanceof check
- [Phase 09-01]: Auto-pass only fires when stack is empty (conservative: let player respond to stack items)
- [Phase 09-02]: Auto-pass defaults to ON (localStorage forge-auto-pass, absent = true)
- [Phase 09-02]: Phase flash uses 300ms ease-out animation from primary to transparent
- [Phase 09-02]: Auto-pass preference synced to backend on first BUTTON_UPDATE via useRef guard

### Blockers/Concerns

- [Research]: Verify `HostedMatch.startMatch()` with empty guis map before Phase 12 planning
- [Research]: Verify `StaticData.getSpecialBoosters()` edition codes before Phase 11 planning
- [Research]: Investigate `PlayerControllerHuman` undo constraints before Phase 9 planning

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260320-eyo | Fix deck import: commander detection, card metadata reload, error display, unfound notification, dialog overflow | 2026-03-20 | 0c0aa85 | [260320-eyo-fix-deck-import-commander-detection-card](./quick/260320-eyo-fix-deck-import-commander-detection-card/) |

## Session Continuity

Last session: 2026-03-21T02:51:13.683Z
Stopped at: Phase 10 context gathered
Resume file: .planning/phases/10-advanced-deck-stats/10-CONTEXT.md
