# Phase 9: Engine Integration UX - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Add smart auto-pass (automatically skip priority when no legal plays) and undo last spell (reverse the last cast before resolution). Both require deeper engine integration than Phase 8's UI work. This is a small, focused phase — just 2 features.

</domain>

<decisions>
## Implementation Decisions

### Auto-Pass (Smart Auto-Yield)
- Smart auto-pass: automatically pass priority when the player has no legal plays (no castable instants, no activatable abilities)
- No manual per-phase configuration — the system detects whether you CAN do something and only stops when you can
- On by default — dramatically speeds up gameplay. Users can disable via a toggle if desired
- Brief flash on phase strip (200ms highlight) when auto-pass skips a phase, so user stays oriented
- Implementation: backend-driven — WebGuiGame checks if player has legal responses before sending BUTTON_UPDATE. If no legal plays, auto-respond with OK instead of prompting the frontend
- Research noted: `isUiSetToSkipPhase` is hardcoded to `return false` — auto-yield must be a local data structure on WebGuiGame, not a WebSocket round-trip

### Undo Last Spell
- Show "Undo Last Spell [Z]" button only when `canUndoLastAction()` returns true — hide completely when unavailable
- Z keyboard shortcut (already reserved from Phase 8 shortcuts)
- Feedback: game log entry "Undo: [Spell Name] removed from stack, mana refunded" + natural stack panel update. No toast or special animation
- Research noted: undo only works on the stack (`MagicStack.undo()`). Cannot undo land drops, combat, or resolved spells
- Backend: need to send `canUndo` flag in BUTTON_UPDATE or GAME_STATE so frontend knows when to show the button
- When undo succeeds: engine handles mana refund via ManaRefundService, sends updated GAME_STATE

### Claude's Discretion
- Exact mechanism for detecting "no legal plays" (may need to check hand + mana + abilities)
- Where to place the auto-pass toggle (game settings, action bar, or lobby)
- How to send `canUndo` state to frontend (new field in BUTTON_UPDATE vs separate message)
- Phase strip flash animation timing and CSS

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Auto-Pass Engine Integration
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` — Where auto-pass logic needs to intercept before sending BUTTON_UPDATE
- `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` — Reference for how desktop handles auto-yield
- `forge-gui-web/frontend/src/components/game/PhaseStrip.tsx` — Where flash animation goes
- `forge-gui-web/frontend/src/components/game/ActionBar.tsx` — Auto-pass toggle location

### Undo Engine Integration
- `forge-game/src/main/java/forge/game/spellability/MagicStack.java` — `undo()` method
- `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` — `tryUndoLastAction()` reference
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` — Where undo needs to be wired
- `forge-gui-web/frontend/src/components/game/ActionBar.tsx` — Where undo button renders
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` — Z hotkey wiring

### Research
- `.planning/research/ARCHITECTURE.md` — Undo constraints, auto-yield architecture
- `.planning/research/PITFALLS.md` — `isUiSetToSkipPhase` hardcoded, undo only on stack
- `.planning/phases/08-gameplay-ux/08-RESEARCH.md` — Phase 8 research with engine details

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ActionBar.tsx` — Already redesigned in Phase 8 with Confirm/Pass split and shortcut hints. Undo button goes here.
- `useHotkeys` from `react-hotkeys-hook` — Already installed and wired for Space/Enter/Escape in Phase 8. Z binding adds to existing setup.
- `hasPriority` in gameStore — Already available for auto-pass frontend logic
- `GameBoard.tsx` hotkey wiring — Existing pattern for keyboard shortcuts

### Established Patterns
- BUTTON_UPDATE payload carries button labels, enable states, and focus hints — can add `canUndo` boolean
- Backend-driven game flow: WebGuiGame controls what the frontend sees
- Game log entries for state changes (established in Phase 8)

### Integration Points
- `WebGuiGame.updateButtons()` — Where auto-pass intercepts (check legal plays before sending)
- `WebGuiGame` — New `undoLastAction()` method wired to a new inbound message type (UNDO)
- `gameWebSocket.ts` — New `sendUndo()` method for Z key / undo button
- `gameStore.ts` — `canUndo` state derived from BUTTON_UPDATE

</code_context>

<specifics>
## Specific Ideas

- Auto-pass should feel invisible — the game just moves faster. You only notice it when it stops (because you have a play)
- Undo should feel forgiving — tap the wrong land, press Z, it's undone. Low friction

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 09-engine-integration-ux*
*Context gathered: 2026-03-20*
