# Phase 9: Engine Integration UX - Research

**Researched:** 2026-03-20
**Domain:** Forge engine auto-pass + undo integration for web client
**Confidence:** HIGH

## Summary

This phase adds two backend-driven features: smart auto-pass (skip priority when no legal plays) and undo last spell (reverse stack items with mana refund). Both features require changes to the Java backend (`WebGuiGame`, `WebServer`, `MessageType`) and the React frontend (`ActionBar`, `PhaseStrip`, `gameStore`, `gameWebSocket`).

The Forge engine already has all the primitives needed. `MagicStack.canUndo(player)` + `MagicStack.undo()` + `ManaRefundService.refundManaPaid()` handle undo completely. `PlayerControllerHuman.canUndoLastAction()` wraps these with priority checks. For auto-pass, `Card.getAllPossibleAbilities(player, true)` returns playable abilities per card, and `isUiSetToSkipPhase()` is the designated hook that currently returns `false` in `WebGuiGame`. The implementation is mostly wiring -- connecting existing engine capabilities to the WebSocket protocol and React components.

**Primary recommendation:** Implement backend changes first (auto-pass logic in `WebGuiGame.updateButtons()`, undo message handler in `WebServer`, `canUndo` field in `ButtonPayload`), then frontend changes (undo button in ActionBar, phase strip flash, auto-pass toggle, Z hotkey).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Smart auto-pass: automatically pass priority when the player has no legal plays (no castable instants, no activatable abilities)
- No manual per-phase configuration -- the system detects whether you CAN do something and only stops when you can
- On by default -- dramatically speeds up gameplay. Users can disable via a toggle if desired
- Brief flash on phase strip (200ms highlight) when auto-pass skips a phase, so user stays oriented
- Implementation: backend-driven -- WebGuiGame checks if player has legal responses before sending BUTTON_UPDATE. If no legal plays, auto-respond with OK instead of prompting the frontend
- Research noted: `isUiSetToSkipPhase` is hardcoded to `return false` -- auto-yield must be a local data structure on WebGuiGame, not a WebSocket round-trip
- Show "Undo Last Spell [Z]" button only when `canUndoLastAction()` returns true -- hide completely when unavailable
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

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| GUX-06 | User can set auto-yield for specific phases (e.g., always pass priority during upkeep when no instants) | Smart auto-pass checks `Card.getAllPossibleAbilities(player, true)` across all zones; `WebGuiGame.updateButtons()` is the interception point; `isUiSetToSkipPhase()` already exists as the engine hook |
| GUX-09 | User can undo the last spell cast (where the Forge engine supports it, labeled "Undo Last Spell") | `MagicStack.canUndo(player)` + `MagicStack.undo()` + `ManaRefundService.refundManaPaid()` provide full undo; `PlayerControllerHuman.canUndoLastAction()` wraps with priority check; new UNDO inbound message type needed |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| react-hotkeys-hook | Already installed | Z keyboard shortcut for undo | Already used for Space/Enter/Escape in Phase 8 GameBoard |
| zustand + immer | Already installed | `canUndo` and `autoPassEnabled` state in gameStore | Existing state management pattern |

### Supporting
No new libraries needed. All implementation uses existing Forge engine APIs and the established WebSocket protocol.

## Architecture Patterns

### Pattern 1: Backend-Driven Auto-Pass (No Legal Plays Detection)

**What:** Before sending BUTTON_UPDATE to the frontend, check if the human player has any legal plays. If not, auto-respond with OK (pass priority) instead of prompting the user.

**When to use:** Every time the engine calls `WebGuiGame.updateButtons()` with enabled buttons for the human player.

**How to detect "no legal plays":**

The engine already provides `Card.getAllPossibleAbilities(player, removeUnplayable=true)` which returns only playable abilities after checking mana, timing restrictions, etc. The check needs to cover:

1. **Hand cards:** Can the player cast any instant/flash spell? Can they play a land (main phases only)?
2. **Battlefield cards:** Can they activate any abilities?
3. **Graveyard/Exile/Command:** Can they activate anything with flashback, escape, etc.?

The `Player` class provides `getCardsActivatableInExternalZones(true)` for zones beyond hand/battlefield, and `PlayerZone.getCardsPlayerCanActivate(player)` for hand and battlefield.

**Recommended approach -- check all zones:**
```java
// In WebGuiGame, add a method:
private boolean playerHasLegalPlays(Player player) {
    // Check hand (instants, flash spells, abilities)
    for (Card c : player.getCardsIn(ZoneType.Hand)) {
        if (!c.getAllPossibleAbilities(player, true).isEmpty()) {
            return true;
        }
    }
    // Check battlefield (activated abilities)
    for (Card c : player.getCardsIn(ZoneType.Battlefield)) {
        if (!c.getAllPossibleAbilities(player, true).isEmpty()) {
            return true;
        }
    }
    // Check external zones (flashback, escape, etc.)
    for (Card c : player.getCardsActivatableInExternalZones(true)) {
        if (!c.getAllPossibleAbilities(player, true).isEmpty()) {
            return true;
        }
    }
    return false;
}
```

**Critical requirement:** This check must NOT be a WebSocket round-trip. It runs entirely on the backend game thread before deciding whether to send BUTTON_UPDATE.

**Where to intercept:** Override `updateButtons()` in WebGuiGame. If the player has no legal plays AND auto-pass is enabled, call `getGameController().selectButtonOk()` directly instead of sending the BUTTON_UPDATE to the frontend. The engine will then advance to the next phase.

**Edge case -- must NOT auto-pass when:**
- A spell/ability is on the stack targeting the player (they might want to respond)
- Combat phases where declaring attackers/blockers is expected
- `InputPassPriority` specifically -- other input types (choosing targets, choosing modes) should never be auto-passed

**Source:** Direct analysis of `InputPassPriority.java` (lines 56-68), `Card.java` line 7493, `Player.java` line 1376.

### Pattern 2: Undo via Explicit Message Type

**What:** Add an `UNDO` inbound WebSocket message type that calls the engine's existing undo machinery.

**When to use:** When the user clicks the Undo button or presses Z.

**Why not reuse BUTTON_CANCEL:** The current cancel button path (`InputPassPriority.onCancel()`, line 82) already calls `tryUndoLastAction()`, and if undo fails, it falls through to auto-pass-until-end-of-turn. This dual behavior is confusing for a dedicated undo button. A separate UNDO message type is cleaner -- it only undoes, never passes.

**Implementation:**
```java
// WebServer.java -- add to switch statement:
case UNDO -> {
    GameSession session = activeSessions.get(gameId);
    if (session != null) {
        forge.interfaces.IGameController gc =
                session.webGuiGame.getGameController();
        if (gc != null) {
            gc.undoLastAction();
        }
    }
}
```

The engine handles everything: `PlayerControllerHuman.undoLastAction()` -> `tryUndoLastAction()` -> checks `canUndoLastAction()` -> calls `game.getStack().undo()` -> `ManaRefundService.refundManaPaid()` -> untaps lands, returns mana to pool. After undo, the engine re-prompts via `InputPassPriority.showMessageInitial()` which triggers a new `updateButtons()` call, naturally sending updated game state to the frontend.

**Source:** `PlayerControllerHuman.java` lines 2360-2387, `MagicStack.java` lines 189-206, `ManaRefundService.java` lines 21-56.

### Pattern 3: canUndo in BUTTON_UPDATE Payload

**What:** Add `canUndo` boolean to the BUTTON_UPDATE payload so the frontend knows when to show the undo button.

**Why BUTTON_UPDATE, not GAME_STATE:** The `canUndoLastAction()` check requires `Player` (not `PlayerView`) and the priority state. `BUTTON_UPDATE` already carries the priority context and is sent exactly when the engine prompts for input. Adding `canUndo` here is natural. `GameStateDto.from(GameView)` doesn't have access to the `Player` or `MagicStack` internals needed for the check.

**Implementation in WebGuiGame:**
```java
@Override
public void updateButtons(final PlayerView owner, final String label1, final String label2,
                          final boolean enable1, final boolean enable2, final boolean focus1) {
    // Check canUndo using the game controller
    boolean canUndo = false;
    forge.interfaces.IGameController gc = getGameController();
    if (gc instanceof PlayerControllerHuman pch) {
        canUndo = pch.canUndoLastAction();
    }

    send(MessageType.BUTTON_UPDATE, payloadMap(
            "playerId", owner != null ? owner.getId() : -1,
            "label1", label1,
            "label2", label2,
            "enable1", enable1,
            "enable2", enable2,
            "focus1", focus1,
            "canUndo", canUndo
    ));
    sendLogDelta();
}
```

**Source:** `WebGuiGame.java` lines 267-278, `PlayerControllerHuman.java` lines 2360-2366.

### Pattern 4: Phase Strip Flash Animation

**What:** Brief 200ms highlight on the phase strip pill when auto-pass skips a phase. Keeps the user oriented without interrupting flow.

**How to trigger:** When a `PHASE_UPDATE` arrives and the previous phase was auto-passed (no BUTTON_UPDATE between phase transitions), the PhaseStrip component shows a flash animation on the new phase pill.

**CSS approach:**
```css
@keyframes phase-flash {
  0% { background-color: hsl(var(--primary) / 0.8); }
  100% { background-color: transparent; }
}
.phase-flash {
  animation: phase-flash 200ms ease-out;
}
```

**Detection logic:** Track whether a BUTTON_UPDATE was received since the last PHASE_UPDATE. If no BUTTON_UPDATE was received (meaning auto-pass skipped the phase), add the flash class. This is a frontend-only concern -- no backend changes needed.

### Recommended Project Structure (changes only)

```
forge-gui-web/
  src/main/java/forge/web/
    WebGuiGame.java           # MODIFY: auto-pass logic, canUndo in BUTTON_UPDATE
    WebServer.java            # MODIFY: UNDO message handler
    protocol/
      MessageType.java        # MODIFY: add UNDO enum value
  frontend/src/
    components/game/
      ActionBar.tsx            # MODIFY: undo button, auto-pass toggle
      PhaseStrip.tsx           # MODIFY: flash animation on auto-pass
      GameBoard.tsx            # MODIFY: Z hotkey binding
    stores/
      gameStore.ts             # MODIFY: canUndo state, autoPassEnabled preference
    lib/
      gameTypes.ts             # MODIFY: canUndo in ButtonPayload, UNDO in InboundMessageType
      gameWebSocket.ts         # MODIFY: sendUndo() method
```

### Anti-Patterns to Avoid

- **Auto-pass as WebSocket round-trip:** NEVER send a "should I auto-pass?" question to the frontend. The check must be entirely backend-driven. `isUiSetToSkipPhase()` already runs on the game thread -- keep it there.
- **Overloading BUTTON_CANCEL for undo:** The cancel path has dual behavior (undo OR end-turn). A dedicated UNDO message is explicit and avoids confusion.
- **Auto-passing during combat:** The user expects to declare attackers/blockers. Only auto-pass during non-combat priority windows where `InputPassPriority` is the active input.
- **Checking `getAllPossibleAbilities` with `removeUnplayable=false`:** This returns ALL abilities including unplayable ones. MUST use `removeUnplayable=true` to only get actually castable/activatable abilities.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Undo spell + mana refund | Custom stack manipulation | `MagicStack.undo()` + `ManaRefundService` | Engine already handles edge cases: mana abilities that tapped multiple permanents, paying with different player's mana, etc. |
| Legal plays detection | Custom "can cast" check | `Card.getAllPossibleAbilities(player, true)` | Accounts for timing restrictions, mana availability, static abilities granting flash, alternate costs, etc. |
| Phase skip mechanism | Custom phase-skip flag | `isUiSetToSkipPhase()` override | Engine already calls this at the right time in the game loop |
| Auto-pass-until-end-of-turn | Custom turn tracking | `AbstractGuiGame.autoPassUntilEndOfTurn()` | Already implemented, handles cleanup and cancel properly |

**Key insight:** The Forge engine already implements every piece of game logic needed. This phase is purely about wiring existing engine APIs to the WebSocket protocol and React UI. Zero game-rule logic should be written.

## Common Pitfalls

### Pitfall 1: Auto-Pass Skipping When Player Has Responses to Stack Items
**What goes wrong:** Auto-pass fires because the player has no spells to cast, but there's a spell on the stack they should respond to (e.g., opponent's Lightning Bolt targeting their creature -- they have a regeneration ability).
**Why it happens:** The "no legal plays" check only looks at castable spells, not activatable abilities in response to stack items.
**How to avoid:** `Card.getAllPossibleAbilities(player, true)` already includes activated abilities. Ensure the check covers battlefield permanents, not just hand cards. Also, if the stack is non-empty, consider being more conservative -- only auto-pass if the player truly has zero playable abilities across ALL zones.
**Warning signs:** The AI resolves spells the player should have been able to respond to.

### Pitfall 2: Auto-Pass Creates Infinite Loop
**What goes wrong:** `updateButtons()` is overridden to auto-respond with OK. But calling `selectButtonOk()` triggers the engine to advance, which calls `updateButtons()` again, which auto-responds again... this creates a tight loop that never yields to the frontend.
**Why it happens:** The auto-pass intercept is in the fire-and-forget `updateButtons()` method, but `selectButtonOk()` is a blocking input queue operation.
**How to avoid:** Do NOT call `selectButtonOk()` directly from `updateButtons()`. Instead, override `isUiSetToSkipPhase()` to return true when appropriate. The engine's `InputPassPriority` already checks `gui.isUiSetToSkipPhase()` and skips automatically. This is the correct interception point -- it's designed for this exact purpose.
**Warning signs:** Game freezes, stack overflow, or the game advances through all phases instantly without stopping.

### Pitfall 3: Undo Button Visible But Non-Functional
**What goes wrong:** `canUndo` is sent in the initial BUTTON_UPDATE but becomes stale. The player sees the undo button but clicking it does nothing because the undo window has passed.
**Why it happens:** `canUndo` is checked at BUTTON_UPDATE time but other actions (opponent casting, triggers resolving) can clear the undo stack between the check and the user clicking.
**How to avoid:** The UNDO handler already guards with `canUndoLastAction()` before calling undo. If undo fails, the button stays visible until the next BUTTON_UPDATE naturally refreshes `canUndo`. This is acceptable behavior -- the click is simply a no-op, and the button disappears on the next state update.
**Warning signs:** Not a real problem as long as the handler guards properly. Do NOT flash an error -- just silently ignore.

### Pitfall 4: `getGameController()` Returns Wrong Controller Type
**What goes wrong:** `WebGuiGame.getGameController()` returns `IGameController` but `canUndoLastAction()` is on `PlayerControllerHuman`, not the interface.
**Why it happens:** `IGameController` only declares `undoLastAction()` (void), not `canUndoLastAction()` (boolean).
**How to avoid:** Cast to `PlayerControllerHuman` with instanceof check: `if (gc instanceof PlayerControllerHuman pch) { canUndo = pch.canUndoLastAction(); }`. This is safe because the web GUI always uses `PlayerControllerHuman` for the human player.
**Warning signs:** ClassCastException or `canUndo` always false.

### Pitfall 5: Auto-Pass Needs Access to `Player`, Not Just `PlayerView`
**What goes wrong:** `WebGuiGame` only has access to `GameView` and `PlayerView` (view-layer objects). But `Card.getAllPossibleAbilities()` requires `Player` (game-layer object). `isUiSetToSkipPhase()` receives `PlayerView`, not `Player`.
**Why it happens:** The view/model separation in Forge. WebGuiGame is in the GUI layer.
**How to avoid:** For `isUiSetToSkipPhase()`, the check doesn't need `Player` -- it just needs to know if auto-pass is enabled (a local boolean). The actual "has legal plays" check happens inside the engine's `InputPassPriority` flow, not in `isUiSetToSkipPhase()`. The smarter approach: override `isUiSetToSkipPhase()` to return the user's auto-pass preference, and let the engine handle the rest. The engine already calls `card.getAllPossibleAbilities()` inside `InputPassPriority.onCardSelected()` when the user (or auto-pass) tries to interact.

**REVISED UNDERSTANDING:** The CONTEXT.md says "WebGuiGame checks if player has legal responses before sending BUTTON_UPDATE." But this is architecturally wrong given the view/model separation. The correct approach is:
1. `isUiSetToSkipPhase()` returns the user's auto-pass preference (simple boolean)
2. The engine's `InputPassPriority` handles the actual skip logic when `isUiSetToSkipPhase()` returns true
3. The engine itself already knows whether to prompt -- it checks playable cards internally

However, `isUiSetToSkipPhase` only skips entire phases (upkeep, draw, etc.), NOT individual priority passes within a phase. For "smart auto-pass when no legal plays," we need a different hook. The correct interception point is `mayAutoPass()` from `AbstractGuiGame`, which `InputPassPriority.allowAwaitNextInput()` checks at line 93.

**Better approach:** The engine already has auto-pass-until-end-of-turn. For "smart auto-pass when no legal plays," the backend should check legal plays BEFORE sending the BUTTON_UPDATE prompt. The `WebGuiGame` does need access to the `Game` object (not just `GameView`) to call `getAllPossibleAbilities`. Pass the `Game` reference to `WebGuiGame` at construction time (or via the `GameSession`).

## Code Examples

### Example 1: Adding UNDO to MessageType
```java
// MessageType.java -- add to inbound section:
UNDO  // after SELECT_CARD
```

### Example 2: WebServer UNDO Handler
```java
// WebServer.java -- add to switch(msg.getType()):
case UNDO -> {
    GameSession session = activeSessions.get(gameId);
    if (session != null) {
        forge.interfaces.IGameController gc =
                session.webGuiGame.getGameController();
        if (gc != null) {
            gc.undoLastAction();
        }
    }
}
```

### Example 3: canUndo in BUTTON_UPDATE
```java
// WebGuiGame.updateButtons() -- add canUndo to payload:
boolean canUndo = false;
forge.interfaces.IGameController gc = getGameController();
if (gc instanceof PlayerControllerHuman pch) {
    canUndo = pch.canUndoLastAction();
}
// ... existing payloadMap plus "canUndo", canUndo
```

### Example 4: Frontend sendUndo()
```typescript
// gameWebSocket.ts:
sendUndo(): void {
  this.send({ type: 'UNDO', inputId: null, payload: null })
}
```

### Example 5: Undo Button in ActionBar
```tsx
// Inside ActionBar button layout, after Cancel button:
{buttons.canUndo && (
  <Button variant="outline" size="sm" onClick={() => wsRef.current?.sendUndo()}>
    Undo <span className="text-xs opacity-60 ml-1">[Z]</span>
  </Button>
)}
```

### Example 6: Z Hotkey in GameBoard
```typescript
// GameBoard.tsx -- add alongside existing hotkeys:
useHotkeys('z', () => {
  wsRef.current?.sendUndo()
}, { enabled: buttons !== null && buttons.canUndo })
```

### Example 7: Auto-Pass Toggle in ActionBar
```tsx
// Small toggle button in the action bar status area:
<button
  className="text-xs text-muted-foreground hover:text-foreground"
  onClick={() => wsRef.current?.sendToggleAutoPass()}
>
  {autoPassEnabled ? 'Auto-pass ON' : 'Auto-pass OFF'}
</button>
```

### Example 8: Smart Auto-Pass in WebGuiGame
```java
// WebGuiGame -- override updateButtons to check legal plays:
@Override
public void updateButtons(final PlayerView owner, final String label1, final String label2,
                          final boolean enable1, final boolean enable2, final boolean focus1) {
    // Check if auto-pass should fire
    if (autoPassEnabled && enable1 && !hasLegalPlays()) {
        // Auto-respond with OK -- skip prompting the frontend
        forge.interfaces.IGameController gc = getGameController();
        if (gc != null) {
            // Notify frontend of phase transition (flash)
            send(MessageType.PHASE_UPDATE, payloadMap(
                "phase", getGameView() != null && getGameView().getPhase() != null
                    ? getGameView().getPhase().name() : null,
                "autoPass", true
            ));
            gc.selectButtonOk();
            return; // Don't send BUTTON_UPDATE to frontend
        }
    }
    // Normal path -- send to frontend for user input
    // ... existing code with canUndo added
}
```

**WARNING:** The above pattern may cause the infinite loop described in Pitfall 2. The safer approach is to track whether we're already in an auto-pass flow:
```java
private volatile boolean inAutoPass = false;

@Override
public void updateButtons(...) {
    if (autoPassEnabled && enable1 && !inAutoPass && !hasLegalPlays()) {
        inAutoPass = true;
        try {
            send(MessageType.PHASE_UPDATE, payloadMap("phase", ..., "autoPass", true));
            gc.selectButtonOk();
            return;
        } finally {
            inAutoPass = false;
        }
    }
    // ... normal path
}
```

### Example 9: Detecting Legal Plays (requires Game reference)
```java
// WebGuiGame -- needs Game reference passed at construction:
private boolean hasLegalPlays() {
    if (game == null) return true; // fail-open: prompt user if we can't check
    Player humanPlayer = game.getPlayer(humanPlayerId);
    if (humanPlayer == null) return true;

    // Check all zones for playable abilities
    for (Card c : humanPlayer.getCardsIn(ZoneType.Hand)) {
        if (!c.getAllPossibleAbilities(humanPlayer, true).isEmpty()) return true;
    }
    for (Card c : humanPlayer.getCardsIn(ZoneType.Battlefield)) {
        if (!c.getAllPossibleAbilities(humanPlayer, true).isEmpty()) return true;
    }
    for (Card c : humanPlayer.getCardsActivatableInExternalZones(true)) {
        if (!c.getAllPossibleAbilities(humanPlayer, true).isEmpty()) return true;
    }
    return false;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `isUiSetToSkipPhase` returns false | Override to check auto-pass preference | Phase 9 | Enables per-user auto-skip |
| Cancel button dual-purpose (undo OR end-turn) | Dedicated UNDO message type | Phase 9 | Clean separation of concerns |
| No `canUndo` in frontend state | `canUndo` in BUTTON_UPDATE payload | Phase 9 | Frontend shows/hides undo conditionally |

## Open Questions

1. **Game reference for WebGuiGame**
   - What we know: `WebGuiGame` currently only accesses `GameView` (via `getGameView()`). Smart auto-pass needs `Player` and `Card` objects from the `Game` layer.
   - What's unclear: Where to pass the `Game` reference -- constructor, or via a setter after game start?
   - Recommendation: Pass the `Game` reference via a setter called in `GameSession` after `hostedMatch.startMatch()`. The `HostedMatch` holds the `Game` instance. Store as `private Game game` on WebGuiGame.

2. **Auto-pass vs engine's auto-yield**
   - What we know: `isUiSetToSkipPhase` skips entire phases. `autoPassUntilEndOfTurn` skips all remaining phases in a turn. Neither checks for legal plays.
   - What's unclear: Whether intercepting `updateButtons()` is the right level, or if we should add a new engine-level hook.
   - Recommendation: Intercept at `updateButtons()` in `WebGuiGame`. This is the narrowest scope -- only affects web clients, no engine changes needed. Guard against infinite loops with the `inAutoPass` flag.

3. **Auto-pass toggle persistence**
   - What we know: The toggle needs to survive page refreshes for good UX.
   - What's unclear: Whether to use localStorage or a WebSocket message to set it.
   - Recommendation: Frontend `localStorage` for the preference, sent to backend as a one-time SET_AUTO_PASS message on game start. Backend stores in `WebGuiGame.autoPassEnabled`. Simple, no persistence layer needed.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No frontend test framework installed |
| Config file | none |
| Quick run command | Manual browser testing |
| Full suite command | Manual browser testing |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GUX-06 | Auto-pass skips priority when no legal plays | manual-only | Manual: play a game, observe phases auto-skip when hand is empty or no instants | N/A |
| GUX-06 | Auto-pass toggle works | manual-only | Manual: toggle auto-pass off, verify all phases prompt; toggle on, verify skipping resumes | N/A |
| GUX-06 | Phase strip flashes on auto-pass | manual-only | Manual: observe 200ms flash animation on skipped phases | N/A |
| GUX-09 | Undo button visible when canUndo is true | manual-only | Manual: cast a spell, verify undo button appears; resolve it, verify button disappears | N/A |
| GUX-09 | Z key triggers undo | manual-only | Manual: cast a spell, press Z, verify spell removed from stack and mana refunded | N/A |
| GUX-09 | Undo button hidden when unavailable | manual-only | Manual: start of game (empty stack), verify no undo button visible | N/A |

**Justification for manual-only:** No test framework is installed. Both features require a running Forge engine with WebSocket connectivity. The behavior is inherently end-to-end (backend legal-plays check + WebSocket message + frontend render). Unit testing individual components would require mocking the entire engine game loop.

### Sampling Rate
- **Per task commit:** Manual browser test of the specific feature
- **Per wave merge:** Full game playthrough testing both features
- **Phase gate:** Play a complete game with auto-pass on and use undo at least once

### Wave 0 Gaps
None -- no test infrastructure needed for manual testing.

## Sources

### Primary (HIGH confidence)
- `PlayerControllerHuman.java` lines 2360-2387 -- canUndoLastAction(), tryUndoLastAction(), undoLastAction() complete flow
- `MagicStack.java` lines 189-230 -- canUndo(player), undo(), clearUndoStack() implementation
- `ManaRefundService.java` lines 1-57 -- full mana refund flow including recursive ability refunds
- `InputPassPriority.java` lines 56-68 -- how desktop handles undo button label and auto-pass-until-EOT
- `WebGuiGame.java` lines 267-278 -- current updateButtons() implementation, line 928 isUiSetToSkipPhase hardcoded false
- `AbstractGuiGame.java` lines 431-458 -- autoPassUntilEndOfTurn, autoPassCancel, mayAutoPass
- `Card.java` line 7493 -- getAllPossibleAbilities(player, removeUnplayable) signature
- `Player.java` line 1376 -- getCardsActivatableInExternalZones(includeCommandZone)
- `WebServer.java` lines 138-178 -- existing inbound message switch statement pattern
- `MessageType.java` -- current enum values (need UNDO addition)

### Secondary (MEDIUM confidence)
- `.planning/research/ARCHITECTURE.md` -- v2.0 architecture analysis, auto-yield flow description
- `.planning/research/PITFALLS.md` -- auto-yield as WebSocket round-trip pitfall, undo expectations pitfall

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new libraries needed, all existing
- Architecture: HIGH -- based on direct engine code analysis of undo/auto-pass mechanisms
- Pitfalls: HIGH -- identified through code tracing (infinite loop risk, view/model separation, controller casting)

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- Forge engine APIs rarely change)
