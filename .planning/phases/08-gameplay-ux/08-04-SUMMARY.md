---
phase: 08-gameplay-ux
plan: 04
subsystem: ui
tags: [targeting, card-id, zustand, react, tailwind]

requires:
  - phase: 08-01
    provides: "TargetingState store, choiceIds in PromptPayload, delta log streaming"
  - phase: 08-02
    provides: "Priority indicator, ActionBar with Confirm/Pass split, keyboard shortcuts"
  - phase: 08-03
    provides: "Game log panel, oracle text in hover previews"
provides:
  - "Card ID-based targeting mode replacing fragile name matching"
  - "Visual highlights: glowing ring on valid targets, dim on invalid"
  - "Numbered selection badges for multi-target order display"
  - "Single-target auto-confirm, multi-target Confirm/Cancel bar"
  - "Escape key integration for targeting cancellation"
affects: [09-undo-concede, 12-simulation]

tech-stack:
  added: []
  patterns: [card-id-matching-via-choiceIds, targeting-state-driven-highlights]

key-files:
  created: []
  modified:
    - forge-gui-web/frontend/src/components/game/GameBoard.tsx
    - forge-gui-web/frontend/src/components/game/BattlefieldZone.tsx
    - forge-gui-web/frontend/src/components/game/GameCard.tsx

key-decisions:
  - "Targeting activates when choiceIds contain battlefield/hand card IDs, falls back to ChoiceDialog otherwise"
  - "Task 2 merged into Task 1 commit since selectionIndex and selected-target highlight were inseparable from targeting logic"

patterns-established:
  - "Card ID matching: targeting uses numeric IDs from choiceIds instead of string name matching"
  - "Highlight mode composition: BattlefieldZone computes per-card highlight from targetingState, passes to GameCard"

requirements-completed: [GUX-03]

duration: 4min
completed: 2026-03-20
---

# Phase 8 Plan 4: Targeting UX Summary

**Card ID-based targeting mode with visual highlights, numbered multi-target badges, and auto-confirm for single-target prompts**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-20T23:02:24Z
- **Completed:** 2026-03-20T23:06:56Z
- **Tasks:** 2 (Tasks 1-2 automated; Task 3 is human-verify checkpoint)
- **Files modified:** 3

## Accomplishments
- Replaced fragile name-based card matching with reliable card ID matching via choiceIds
- Valid targets glow with primary-color ring; invalid cards dim to 40% opacity
- Single-target prompts auto-confirm on click; multi-target shows count bar with Confirm/Cancel
- Numbered selection badges (1, 2, 3...) show multi-target selection order on cards
- Escape key cancels targeting mode; selected-target highlight has ring-offset for distinction

## Task Commits

Each task was committed atomically:

1. **Task 1+2: Targeting mode with card ID matching + numbered badges** - `71362e749b` (feat)

**Plan metadata:** pending (docs: complete plan)

## Files Created/Modified
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` - Targeting mode orchestration: useEffect for entering targeting, handleBattlefieldCardClick with ID matching, confirmTargeting/cancelTargeting, targeting bar UI
- `forge-gui-web/frontend/src/components/game/BattlefieldZone.tsx` - Highlight mode computation per card from targetingState, passes highlightMode and selectionIndex to GameCard via LaneRow
- `forge-gui-web/frontend/src/components/game/GameCard.tsx` - Exported HighlightMode type, added selected-target highlight class, selectionIndex prop, numbered badge overlay

## Decisions Made
- Targeting activates only when choiceIds contain battlefield/hand card IDs -- non-card prompts still use ChoiceDialog
- Tasks 1 and 2 merged into a single commit because selectionIndex prop, selected-target highlight, and numbered badge were all needed for the code to compile cleanly with Task 1's BattlefieldZone changes

## Deviations from Plan

None - plan executed exactly as written. Tasks 1 and 2 were combined into one commit for technical correctness (BattlefieldZone passes selectionIndex which requires GameCard to accept it).

## Issues Encountered
- Pre-existing TypeScript errors in deck-editor and lobby components (unrelated to this plan) -- out of scope, not addressed

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All Phase 8 gameplay UX features complete (priority, action bar, game log, oracle text, targeting)
- Awaiting human verification checkpoint (Task 3) to confirm all features work end-to-end
- Ready for Phase 9 (undo/concede) after verification

## Self-Check: PASSED

All files exist, all commits verified.

---
*Phase: 08-gameplay-ux*
*Completed: 2026-03-20*
