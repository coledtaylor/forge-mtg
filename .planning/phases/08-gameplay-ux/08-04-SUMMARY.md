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
    - forge-gui-web/frontend/src/components/game/ActionBar.tsx
    - forge-gui-web/frontend/src/lib/gameWebSocket.ts
    - forge-gui-web/src/main/java/forge/web/WebGuiGame.java
    - forge-ai/src/main/java/forge/ai/LobbyPlayerAi.java
    - forge-ai/src/main/java/forge/ai/PlayerControllerAi.java

key-decisions:
  - "Targeting activates when choiceIds contain battlefield/hand card IDs, falls back to ChoiceDialog otherwise"
  - "Task 2 merged into Task 1 commit since selectionIndex and selected-target highlight were inseparable from targeting logic"
  - "chooseSingleEntityForEffect and chooseEntitiesForEffect needed choiceIds for targeting to work on entity-based prompts"
  - "ActionBar suppresses ChoiceDialog during targeting mode to avoid duplicate controls"
  - "Goldfish DOES_NOTHING implemented via doesNothing flag on PlayerControllerAi rather than profile-based approach"

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

- **Duration:** 4 min (initial) + 28 min (verification fixes)
- **Started:** 2026-03-20T23:02:24Z
- **Completed:** 2026-03-20T23:35:17Z
- **Tasks:** 3 (Tasks 1-2 automated; Task 3 human-verify with fixes)
- **Files modified:** 8

## Accomplishments
- Replaced fragile name-based card matching with reliable card ID matching via choiceIds
- Valid targets glow with primary-color ring; invalid cards dim to 40% opacity
- Single-target prompts auto-confirm on click; multi-target shows count bar with Confirm/Cancel
- Numbered selection badges (1, 2, 3...) show multi-target selection order on cards
- Escape key cancels targeting mode; selected-target highlight has ring-offset for distinction

## Task Commits

Each task was committed atomically:

1. **Task 1+2: Targeting mode with card ID matching + numbered badges** - `71362e749b` (feat)
2. **Task 3: Human verification fixes (targeting, game log, goldfish)** - `52110a8c88` (fix)

## Files Created/Modified
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` - Targeting mode orchestration: useEffect for entering targeting, handleBattlefieldCardClick with ID matching, confirmTargeting/cancelTargeting, targeting bar UI
- `forge-gui-web/frontend/src/components/game/BattlefieldZone.tsx` - Highlight mode computation per card from targetingState, passes highlightMode and selectionIndex to GameCard via LaneRow
- `forge-gui-web/frontend/src/components/game/GameCard.tsx` - Exported HighlightMode type, added selected-target highlight class, selectionIndex prop, numbered badge overlay

## Decisions Made
- Targeting activates only when choiceIds contain battlefield/hand card IDs -- non-card prompts still use ChoiceDialog
- Tasks 1 and 2 merged into a single commit because selectionIndex prop, selected-target highlight, and numbered badge were all needed for the code to compile cleanly with Task 1's BattlefieldZone changes

## Deviations from Plan

### Auto-fixed Issues (Human Verification)

**1. [Rule 1 - Bug] Targeting highlights not working**
- **Found during:** Task 3 (human verification)
- **Issue:** `chooseSingleEntityForEffect` and `chooseEntitiesForEffect` in WebGuiGame did not include `choiceIds` in the PROMPT_CHOICE payload, so the frontend never detected targeting prompts as card-based. Also, `chooseSingleEntityForEffect` deserialized the response as `Integer` but the frontend sends `List<Integer>`, causing a JSON mismatch. Additionally, the ActionBar rendered ChoiceDialog text buttons alongside card highlights, causing confusion.
- **Fix:** Added `choiceIds` to both methods, changed `chooseSingleEntityForEffect` to accept `List<Integer>` response, suppressed ChoiceDialog in ActionBar when targeting mode is active, added targeting prompt message to ActionBar status text.
- **Files modified:** `WebGuiGame.java`, `ActionBar.tsx`
- **Commit:** `52110a8c88`

**2. [Rule 1 - Bug] Game log empty**
- **Found during:** Task 3 (human verification)
- **Issue:** The GAME_STATE WebSocket handler called `clearGameLog()` on every state update. Since GAME_STATE fires on every zone/card/mana/life update, the log was being wiped continuously. The subsequent `GAME_LOG` delta message only sent new entries, so old entries were permanently lost.
- **Fix:** Removed `clearGameLog()` from the GAME_STATE handler. The game log is additive and only cleared on full reset.
- **Files modified:** `gameWebSocket.ts`
- **Commit:** `52110a8c88`

**3. [Rule 1 - Bug] Goldfish AI still plays**
- **Found during:** Task 3 (human verification)
- **Issue:** `LobbyPlayerAi` did not store the `AIOption` set or expose it, and `PlayerControllerAi` had no check for `DOES_NOTHING`. The option was passed at creation but never propagated to the controller.
- **Fix:** Added `aiOptions` field and `getAiOptions()` getter to `LobbyPlayerAi`. Added `doesNothing` flag to `PlayerControllerAi` with checks in `chooseSpellAbilityToPlay`, `declareAttackers`, `declareBlockers`, `getAbilityToPlay`, and `playChosenSpellAbility`. `LobbyPlayerAi.createControllerFor` now calls `setDoesNothing(true)` when DOES_NOTHING is present.
- **Files modified:** `LobbyPlayerAi.java`, `PlayerControllerAi.java`
- **Commit:** `52110a8c88`

## Issues Encountered
- Pre-existing TypeScript errors in deck-editor and lobby components (unrelated to this plan) -- out of scope, not addressed
- Pre-existing checkstyle failure in forge-gui-web -- out of scope, not addressed

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All Phase 8 gameplay UX features complete (priority, action bar, game log, oracle text, targeting, goldfish)
- Human verification passed with 3 fixes applied
- Ready for Phase 9 (undo/concede)

## Self-Check: PASSED

All files exist, all commits verified.

---
*Phase: 08-gameplay-ux*
*Completed: 2026-03-20*
