# Phase 8: Gameplay UX - Research

**Researched:** 2026-03-20
**Domain:** Game board UX (priority, targeting, game log, keyboard shortcuts, goldfish mode, oracle text)
**Confidence:** HIGH

## Summary

Phase 8 improves the gameplay experience across six areas: priority indicators, OK/Pass button clarity, targeting feedback, game log, keyboard shortcuts, goldfish mode, and oracle text display. All changes are scoped to the game board and its components plus the game lobby for goldfish mode.

The backend infrastructure is well-suited for these changes. The engine's `GameLog` already captures all 18 entry types via `GameLogFormatter` subscribing to the EventBus, and `GameLog extends java.util.Observable`. The `BUTTON_UPDATE.playerId` field already identifies who has priority. The `getChoices()` method sends display strings but not card IDs, causing the fragile name-matching in targeting. The AI profile system supports custom profiles but has no built-in "do nothing" mode for goldfish. `CardDto` already has `oracleText`, `setCode`, and `collectorNumber` fields.

**Primary recommendation:** Implement a `GAME_LOG` WebSocket message type that streams GameLog entries from the engine. Use `BUTTON_UPDATE.playerId` (already sent) as the priority signal -- no new backend message type needed for priority. Add a `DOES_NOTHING` AIOption or create a pass-only AI profile for goldfish mode. Fix targeting by sending card IDs alongside display strings in `PROMPT_CHOICE` payloads.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Pulsing primary-color border on the ActionBar when the player has priority, with text "You have priority"
- Dimmed ActionBar with "Waiting for opponent..." when player does not have priority
- Two distinct buttons: "Confirm" (green/primary) for confirming actions, "Pass" (muted/secondary) for passing priority
- Only show Confirm when there's something to confirm; always show Pass when player has priority
- Backend needs to send priority state -- new WebSocket message type or flag in BUTTON_UPDATE
- Arena-style shortcuts: Space/Enter = Pass Priority, Escape = Cancel, Z = Undo
- Show shortcut hints on buttons (e.g., "Pass [Space]", "Cancel [Esc]")
- Use `react-hotkeys-hook@5` for keybinding management
- When targeting: valid targets get a glowing primary-color ring, invalid cards dim to 40% opacity
- Click a valid target to select it -- selected target gets a bright checkmark/ring
- Multi-target: click to toggle selection with numbered badges (1, 2, 3). Show count "2/3 selected"
- Fix fragile card-name matching -- use card IDs from prompt choices to match battlefield cards directly
- Tabbed panel in the right column: "Stack" tab (existing stack display) and "Log" tab (scrollable game log)
- Full detail with type icons: every game action
- Color-coded by entry type, turn/phase separators, auto-scroll to latest
- Backend: stream GameLog entries via WebSocket (new GAME_LOG message type)
- AI difficulty selector already exists and is wired -- verify and mark GUX-07 complete
- Add "Goldfish" option to game lobby -- plays against a dummy AI that does nothing
- Show oracle text in the card hover preview (GameHoverPreview and CardHoverPreview)
- Below the card image, show a text panel with: card name, mana cost, type line, oracle text, P/T

### Claude's Discretion
- Exact animation timing for priority pulse
- Game log entry formatting (timestamp format, icon set)
- How to subscribe to GameLog Observable from WebGuiGame
- Goldfish AI implementation details (pass-only profile vs skip AI)
- Oracle text panel layout and typography in hover preview

### Deferred Ideas (OUT OF SCOPE)
- Auto-yield per phase (Phase 9)
- Undo last spell (Phase 9)
- Targeting arrows from source to target
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| GUX-01 | Priority indicator showing when player has priority and whose turn it is | BUTTON_UPDATE.playerId already identifies priority holder; PhaseStrip already shows turn owner; ActionBar redesign with pulse animation |
| GUX-02 | Distinguish between "OK" (confirm action) and "Pass Priority" with clear labeling | ActionBar redesign: Confirm (green, shown only when confirming) + Pass (muted, shown when player has priority) |
| GUX-03 | Visual feedback when targeting cards (highlighted selection, confirm/cancel) | GameCard already has `valid-target`, `invalid` highlight modes; need card ID matching in PROMPT_CHOICE; multi-target numbered badges |
| GUX-04 | Scrollable game log showing all game actions chronologically | Engine GameLog with 18 entry types; new GAME_LOG WebSocket message; tabbed Stack/Log panel using shadcn Tabs |
| GUX-05 | Keyboard shortcuts for common actions | react-hotkeys-hook@5 integration; scoped to GameBoard; shortcut hints on buttons |
| GUX-07 | AI difficulty level selection | Already implemented in AiSettings.tsx + WebServer.handleStartGame mapping Easy/Medium/Hard to AI profiles |
| GUX-08 | Goldfish/solitaire mode with no opponent | New AIOption.DOES_NOTHING or pass-only AI profile; lobby UI option |
| CARD-04 | Oracle text alongside card image in hover preview | CardDto.oracleText already populated; enhance GameHoverPreview with text panel below image |
</phase_requirements>

## Standard Stack

### Core (New dependency)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| react-hotkeys-hook | 5.2.4 | Keyboard shortcut handling for game actions | Most mature React keyboard library. Declarative `useHotkeys` hook with scoped shortcuts (only active when component mounted/focused). ~2KB bundle. |

### Existing (No changes)
| Library | Purpose | Role in Phase 8 |
|---------|---------|-----------------|
| zustand + immer | Game state management | Add `gameLog`, `hasPriority`, `targetingState` fields |
| shadcn/ui Tabs | Tabbed UI components | Stack/Log tabbed panel (component already installed) |
| lucide-react | Icons | Log entry type icons, shortcut hint icons |
| @base-ui/react | Base UI primitives | Tabs primitive already used by shadcn tabs |

**Installation:**
```bash
cd forge-gui-web/frontend && npm install react-hotkeys-hook@5.2.4
```

**Version verification:** react-hotkeys-hook 5.2.4 is current as of 2026-03-20 (verified via npm registry).

## Architecture Patterns

### Backend: GameLog Streaming

**Problem:** The engine's `GameLog` is populated by `GameLogFormatter` (subscribes to EventBus via `@Subscribe`). The `GameLog extends java.util.Observable`. But `WebGuiGame` has access to `GameView` (via `getGameView()`), and `GameView.getGameLog()` returns the `GameLog` instance.

**Recommended approach:** Track a `lastLogIndex` counter in `WebGuiGame`. On every fire-and-forget method that changes game state (`updateZones`, `updatePhase`, `updateStack`, `updateButtons`, `showCombat`), call a `sendLogDelta()` method that fetches entries from `GameLog.getLogEntries(null)` since the last sent index and sends them via a new `GAME_LOG` message type.

```java
// In WebGuiGame.java
private int lastLogIndex = 0;

private void sendLogDelta() {
    final GameView gv = getGameView();
    if (gv == null) return;
    final GameLog gameLog = gv.getGameLog();
    if (gameLog == null) return;

    // getLogEntries returns in REVERSE order, so we need all entries
    final List<GameLogEntry> allEntries = gameLog.getLogEntries(null);
    final int totalEntries = allEntries.size();
    if (totalEntries <= lastLogIndex) return;

    // allEntries is reversed (newest first), so new entries are at indices 0..(totalEntries - lastLogIndex - 1)
    final int newCount = totalEntries - lastLogIndex;
    final List<Map<String, Object>> entries = new ArrayList<>();
    for (int i = newCount - 1; i >= 0; i--) {
        final GameLogEntry entry = allEntries.get(i);
        entries.add(payloadMap(
            "type", entry.type().name(),
            "message", entry.message(),
            "sourceCardId", entry.sourceCard() != null ? entry.sourceCard().getId() : -1
        ));
    }
    lastLogIndex = totalEntries;
    send(MessageType.GAME_LOG, entries);
}
```

**Why not Observable pattern:** `GameLog extends java.util.Observable` is deprecated since Java 9 and does not carry the new entry in the notification. The delta approach based on index is simpler and thread-safe.

### Backend: Priority State

**Current state:** `BUTTON_UPDATE` already sends `playerId` identifying who has priority. When `buttons !== null` in the frontend, the human player has priority (the engine only sends BUTTON_UPDATE when it wants human input). When `buttons === null`, it is the AI's turn or between inputs.

**Recommended approach:** Do NOT add a new message type. Instead, use the existing signals:
- `buttons !== null` --> human has priority (BUTTON_UPDATE is only sent when the engine waits for human input)
- `buttons === null` (idle state) --> waiting for opponent
- The `activePlayerId` from `TURN_UPDATE` tells whose turn it is
- The `phase` from `PHASE_UPDATE` tells which phase is active

This is already how the ActionBar works. The improvement is just visual: pulse animation when `buttons !== null`, dimmed when `buttons === null`.

### Backend: Targeting Card ID Fix

**Problem:** `getChoices()` sends display strings (e.g., `"Lightning Bolt"`) but not card IDs. The frontend's `handleBattlefieldCardClick` does a fragile `card.name.toLowerCase().includes(choice.toLowerCase())` match.

**Solution:** Add a `choiceCardIds` field to the PROMPT_CHOICE payload. When the choices are `GameEntityView` instances (cards/players), include their IDs alongside the display strings.

```java
// In WebGuiGame.getChoices(), after building displayChoices:
final List<Integer> choiceIds = new ArrayList<>();
for (final T choice : choices) {
    if (choice instanceof GameEntityView) {
        choiceIds.add(((GameEntityView) choice).getId());
    } else {
        choiceIds.add(-1);
    }
}
payload.put("choiceIds", choiceIds);
```

The frontend can then match `card.id` directly against `choiceIds[index]` instead of fuzzy name matching.

### Backend: Goldfish AI

**Analysis of options:**

1. **New AIOption.DOES_NOTHING** -- Add to the `AIOption` enum, check in `PlayerControllerAi` to skip all AI actions. This requires modifying `AIOption.java`, `LobbyPlayerAi.java`, and `PlayerControllerAi.java` (3 files in forge-ai).

2. **Pass-only AI profile file** -- Create a `Goldfish.ai` profile with all aggression/casting preferences set to 0. This uses the existing profile system but may not fully suppress all actions (the AI might still block or play lands).

3. **Skip AI player entirely** -- Don't register an AI player. This would break the engine's 2-player assumption.

**Recommendation:** Option 1 is cleanest. Add `DOES_NOTHING` to `AIOption`. In `PlayerControllerAi`, check if this option is set and auto-pass on all priority holds. The AI still exists as a player (starts at 20 life) but never casts spells, never attacks, never blocks. This is exactly the goldfish experience.

**Implementation in WebServer:**
```java
// In handleStartGame, when aiDifficulty is "Goldfish":
Set<AIOption> options = EnumSet.of(AIOption.DOES_NOTHING);
aiPlayer.setPlayer(GamePlayerUtil.createAiPlayer("Goldfish", 0, 0, options, ""));
```

### Frontend: ActionBar Redesign

**Current structure:** Single component with 3 branches: buttons mode, prompt mode, idle. The buttons mode shows OK/Cancel/Pass Priority as 3 separate buttons.

**New structure:**
```
ActionBar
  |-- Priority indicator (pulsing border + "You have priority" text)
  |-- Confirm button (green, only when there's an action to confirm, shows shortcut hint)
  |-- Pass button (muted, always shown when player has priority, shows shortcut hint)
  |-- Cancel button (when prompts require it)
  |-- Idle state (dimmed "Waiting for opponent...")
```

**Key insight:** The engine already distinguishes between these via `BUTTON_UPDATE`:
- `label1` is typically "OK" (confirm/pass) and `label2` is "Cancel" (or "Undo" when available)
- `enable1: true` means the player can act
- The distinction between "confirming an action" and "passing priority" comes from the prompt context

**Simpler approach:** Map `buttons.label1` to either Confirm or Pass based on whether a prompt is active. If `prompt !== null` and `buttons !== null`, the OK button means "Confirm". If `prompt === null` and `buttons !== null`, the OK button means "Pass Priority".

### Frontend: Targeting Mode

**Current flow:**
1. Engine sends `PROMPT_CHOICE` with card names as `choices`
2. `isTargetingMode` is set when `prompt?.type === 'PROMPT_CHOICE'`
3. User clicks battlefield card -> `handleBattlefieldCardClick` fuzzy-matches card name to choice string
4. Sends back the matching choice index

**Improved flow:**
1. Engine sends `PROMPT_CHOICE` with `choices` (display strings) AND `choiceIds` (card IDs)
2. `gameStore` enters targeting mode with `targetingState: { validTargetIds: number[], selectedIds: number[], min, max }`
3. `BattlefieldZone` passes `highlightMode` to each `GameCard` based on whether its ID is in `validTargetIds`
4. User clicks valid target -> adds to `selectedIds`, shows numbered badge
5. For single-target, auto-confirms. For multi-target, shows count and Confirm button

**Key change:** `GameCard` already supports `valid-target` and `invalid` highlight modes with appropriate CSS classes. The missing piece is just the data flow to determine which cards are valid targets.

### Frontend: Game Log Panel

**Architecture:**
```
Right column (220px):
  Tabs (shadcn/ui)
    Tab: "Stack" -> existing StackPanel content
    Tab: "Log" -> new GameLogPanel
      - scrollable container with overflow-y-auto
      - entries grouped by turn/phase separators
      - color-coded by GameLogEntryType
      - auto-scroll to bottom on new entries
      - no virtualization needed (rarely exceeds 300 entries)
```

**Log entry type to icon/color mapping (Claude's discretion):**

| Entry Type | Color | Icon Suggestion |
|------------|-------|----------------|
| TURN | primary | Hash/number |
| PHASE | muted | Clock |
| STACK_ADD | blue | Plus circle |
| STACK_RESOLVE | green | Check circle |
| DAMAGE | red | Zap |
| LIFE | yellow | Heart |
| LAND | green | Leaf |
| COMBAT | orange | Sword |
| ZONE_CHANGE | gray | Arrow right |
| MULLIGAN | purple | Shuffle |
| INFORMATION | muted | Info |

### Frontend: Oracle Text in Hover Preview

**Current GameHoverPreview:** Takes `cardName` string, renders Scryfall image via name-based URL. No oracle text.

**Current CardHoverPreview (deck editor):** Takes `{setCode, collectorNumber, name}`, renders Scryfall image via direct URL. No oracle text.

**Problem:** Both hover components receive only card name or set/collector info -- they don't receive the full `CardDto` with oracle text.

**Solution:** Change hover callbacks to pass the full `CardDto` (or at least `{name, setCode, collectorNumber, oracleText, type, manaCost, power, toughness}`) instead of just the card name. Then render a text panel below the card image.

**Layout:**
```
+-------------------+
|                   |
|   Card Image      |
|   (260px wide)    |
|                   |
+-------------------+
| Card Name    {MC} |
| Type Line         |
|                   |
| Oracle text body  |
| (wrapped, small)  |
|                   |
| P/T (bottom-right)|
+-------------------+
```

### Recommended File Changes

```
Backend (Java):
  MODIFY  forge-gui-web/.../protocol/MessageType.java      -- Add GAME_LOG
  MODIFY  forge-gui-web/.../WebGuiGame.java                -- Add sendLogDelta(), call on state changes
  MODIFY  forge-gui-web/.../WebServer.java                 -- Handle "Goldfish" aiDifficulty
  MODIFY  forge-ai/.../AIOption.java                       -- Add DOES_NOTHING
  MODIFY  forge-ai/.../LobbyPlayerAi.java                  -- Pass DOES_NOTHING to controller
  MODIFY  forge-ai/.../PlayerControllerAi.java             -- Auto-pass when DOES_NOTHING set

Frontend (TypeScript):
  MODIFY  lib/gameTypes.ts                                 -- Add GAME_LOG type, GameLogEntry type, choiceIds to PromptPayload
  MODIFY  lib/gameWebSocket.ts                             -- Handle GAME_LOG message
  MODIFY  stores/gameStore.ts                              -- Add gameLog[], targeting state, hasPriority derived state
  MODIFY  components/game/ActionBar.tsx                    -- Redesign with Confirm/Pass split, priority pulse, shortcut hints
  MODIFY  components/game/GameBoard.tsx                    -- Targeting mode with card ID matching, keyboard shortcuts
  MODIFY  components/game/GameCard.tsx                     -- Pass cardDto to hover callbacks instead of just name
  MODIFY  components/game/GameHoverPreview.tsx             -- Accept CardDto, show oracle text panel
  MODIFY  components/game/BattlefieldZone.tsx              -- Pass CardDto to hover, targeting highlights
  MODIFY  components/game/HandZone.tsx                     -- Same hover/targeting changes
  MODIFY  components/game/StackPanel.tsx                   -- Wrap in tab, extract content
  MODIFY  components/lobby/GameLobby.tsx                   -- Add Goldfish option
  MODIFY  components/lobby/AiSettings.tsx                  -- Add Goldfish difficulty option
  MODIFY  types/game.ts                                    -- Add goldfish to config
  NEW     components/game/GameLogPanel.tsx                 -- Scrollable game log with type icons
  NEW     components/game/RightPanel.tsx                   -- Tabbed container for Stack + Log
```

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Keyboard shortcuts | Manual addEventListener + useEffect cleanup | react-hotkeys-hook@5 `useHotkeys` | Scope management, focus handling, modifier key combos, and React lifecycle integration are all handled. Manual approaches break when multiple shortcut contexts overlap (e.g., game board vs dialog) |
| Tab component | Custom tab state + button group | shadcn/ui Tabs (already installed) | Accessible by default (ARIA roles, keyboard nav). Already styled to match the design system |
| Log auto-scroll | Manual scroll position tracking | `useRef` + `scrollIntoView({ behavior: 'smooth' })` on new entry | One-liner pattern vs custom scroll position calculation |

## Common Pitfalls

### Pitfall 1: Fragile Card Name Matching in Targeting
**What goes wrong:** The current `handleBattlefieldCardClick` matches choices by `card.name.toLowerCase().includes(choice.toLowerCase())`. This fails for: tokens with identical names, split cards, adventures, MDFCs, and any card whose display string differs from its CardDto.name.
**Why it happens:** `getChoices()` only sends display strings, not entity IDs.
**How to avoid:** Send `choiceIds` (entity IDs) alongside `choices` (display strings) in PROMPT_CHOICE payload. Match by ID on the frontend.
**Warning signs:** Clicking a battlefield card doesn't select it as a target, or selects the wrong card.

### Pitfall 2: GameLog Entry Order
**What goes wrong:** `GameLog.getLogEntries(null)` returns entries in REVERSE chronological order (newest first). If the frontend naively appends, the log displays backwards.
**Why it happens:** The method iterates `log.size()-1` down to `0`.
**How to avoid:** Reverse the delta entries before sending, or send with explicit sequence numbers. The `sendLogDelta` pattern above already handles this by iterating `newCount-1` down to `0`.

### Pitfall 3: Priority Pulse on Every BUTTON_UPDATE
**What goes wrong:** The ActionBar re-renders and re-triggers the pulse animation on every BUTTON_UPDATE, even if priority didn't change (e.g., the engine just updated button labels).
**Why it happens:** BUTTON_UPDATE fires frequently during the engine's processing.
**How to avoid:** Track a `hasPriority` boolean in the store. Only toggle it when `buttons` transitions from null to non-null or vice versa, not on every update. Use CSS `@keyframes` with `animation-iteration-count: infinite` so the pulse runs continuously once started, rather than re-triggering.

### Pitfall 4: Keyboard Shortcuts Firing During Dialogs
**What goes wrong:** User presses Space to pass priority but a ChoiceDialog is showing -- the shortcut fires instead of the dialog's own input handling.
**Why it happens:** Global keyboard listeners don't respect UI context.
**How to avoid:** Use react-hotkeys-hook's `enabled` option. Disable game shortcuts when `prompt !== null`. Example: `useHotkeys('space', handler, { enabled: prompt === null && buttons?.enable1 })`.

### Pitfall 5: Goldfish AI Still Playing Lands
**What goes wrong:** A "do nothing" AI profile might still play lands (land drops are automatic in some code paths).
**Why it happens:** Land playing logic in the engine may bypass the AI controller's action selection.
**How to avoid:** Check `PlayerControllerAi.canPlayLand()` and other land-related methods. The `DOES_NOTHING` check should return `null` for `getSpellAbilityToPlay` and skip land selection entirely.

### Pitfall 6: Hover Preview Passing Card Name vs CardDto
**What goes wrong:** Changing `onHoverEnter(cardName, e)` to `onHoverEnter(cardDto, e)` in GameCard requires updating every component that uses GameCard hover callbacks -- BattlefieldZone, HandZone, StackPanel.
**Why it happens:** The hover interface currently only passes a string.
**How to avoid:** Change the interface in one pass across all consuming components. The cardDto includes name, oracleText, setCode, collectorNumber, type, manaCost, power, toughness.

## Code Examples

### react-hotkeys-hook Integration (GameBoard)
```tsx
// Source: react-hotkeys-hook v5 documentation pattern
import { useHotkeys } from 'react-hotkeys-hook'

// Inside GameBoard component:
const buttons = useGameStore((s) => s.buttons)
const prompt = useGameStore((s) => s.prompt)

// Pass priority - only when no prompt active and buttons enabled
useHotkeys('space, enter', () => {
  wsRef.current?.sendButtonOk()
}, { enabled: prompt === null && buttons !== null && buttons.enable1 })

// Cancel - only when cancel is enabled
useHotkeys('escape', () => {
  wsRef.current?.sendButtonCancel()
}, { enabled: buttons !== null && buttons.enable2 })
```

### GameLog Message Type
```typescript
// In gameTypes.ts
export interface GameLogEntry {
  type: string  // GameLogEntryType name: TURN, PHASE, STACK_ADD, DAMAGE, etc.
  message: string
  sourceCardId: number  // -1 if no source card
}

// In OutboundMessageType union:
| 'GAME_LOG'
```

### Targeting State in Store
```typescript
// In gameStore.ts
interface TargetingState {
  validTargetIds: number[]     // card IDs that are valid targets
  selectedTargetIds: number[]  // currently selected targets
  min: number                  // minimum required selections
  max: number                  // maximum allowed selections
  promptInputId: string        // for sending response
}

// In GameState:
targetingState: TargetingState | null
gameLog: GameLogEntry[]
```

### Oracle Text Hover Panel
```tsx
// In enhanced GameHoverPreview
function OracleTextPanel({ card }: { card: CardDto }) {
  return (
    <div className="w-[260px] bg-card rounded-b-lg border border-t-0 border-border p-3 space-y-1">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-foreground">{card.name}</span>
        {card.manaCost && (
          <span className="text-xs text-muted-foreground">{card.manaCost}</span>
        )}
      </div>
      {card.type && (
        <div className="text-xs text-muted-foreground">{card.type}</div>
      )}
      {card.oracleText && (
        <div className="text-xs text-foreground whitespace-pre-line leading-relaxed">
          {card.oracleText}
        </div>
      )}
      {card.type?.toLowerCase().includes('creature') && (
        <div className="text-xs text-foreground text-right font-semibold">
          {card.power}/{card.toughness}
        </div>
      )}
    </div>
  )
}
```

### Goldfish AI Option Check
```java
// In PlayerControllerAi, add early returns:
@Override
public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities) {
    if (lobbyPlayer.getAiOptions() != null
        && lobbyPlayer.getAiOptions().contains(AIOption.DOES_NOTHING)) {
        return null;  // Never play anything
    }
    // ... existing logic
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `java.util.Observable` | EventBus (`@Subscribe`) | Java 9 (2017) | GameLog still extends Observable but the active subscription is via EventBus. Don't use Observer pattern |
| Name-based Scryfall URLs | Set/collector-number URLs | Phase 7 (this milestone) | CardDto now has setCode/collectorNumber. GameHoverPreview should switch from name-based to direct URLs |

**Deprecated/outdated:**
- `java.util.Observable` (used by GameLog): Deprecated since Java 9 but still functional. Do NOT subscribe via addObserver -- use the EventBus pattern or the delta-based approach instead.

## Open Questions

1. **PlayerControllerAi method signatures for DOES_NOTHING**
   - What we know: `PlayerControllerAi` is the AI controller. It has methods like `getSpellAbilityToPlay`, `attackerAction`, `blockerAction`.
   - What's unclear: The exact list of methods that need DOES_NOTHING early returns. Need to audit during implementation.
   - Recommendation: Check `declareAttackers`, `declareBlockers`, `canPlayLand`, `getSpellAbilityToPlay` at minimum. Simplest approach: override `passPriority` to always return true if possible.

2. **GameLog thread safety**
   - What we know: The GameLog is written on the game thread and read by `sendLogDelta()` which runs on the WebSocket/Javalin thread.
   - What's unclear: Whether `GameLog.getLogEntries()` is thread-safe (it iterates a plain `ArrayList`).
   - Recommendation: Synchronize access or read the log size first, then iterate. The delta approach with `lastLogIndex` is inherently safe if we only read size once per call.

3. **ChoiceDialog vs Targeting Mode overlap**
   - What we know: Some PROMPT_CHOICE messages are for targeting (choose a creature to destroy) and some are for modal choices (choose a mode for Cryptic Command). Both use the same message type.
   - What's unclear: How to distinguish "this is a targeting prompt where battlefield cards are valid" from "this is a modal choice that should show as a button list".
   - Recommendation: If `choiceIds` contains IDs that match battlefield card IDs, enter targeting mode. Otherwise, show as ChoiceDialog buttons. This heuristic should work for all common cases.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (no test framework configured) |
| Config file | none |
| Quick run command | N/A |
| Full suite command | `cd forge-gui-web/frontend && npm run lint && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GUX-01 | Priority indicator visible when player has priority | manual-only | Visual verification: pulse animation on ActionBar | N/A |
| GUX-02 | Confirm vs Pass buttons distinct | manual-only | Visual verification: two distinct buttons | N/A |
| GUX-03 | Targeting feedback with card highlights | manual-only | Play a removal spell, verify valid targets highlight | N/A |
| GUX-04 | Scrollable game log | manual-only | Play several turns, verify log shows entries | N/A |
| GUX-05 | Keyboard shortcuts work | manual-only | Press Space to pass, Escape to cancel | N/A |
| GUX-07 | AI difficulty selection | manual-only | Already exists -- verify in lobby | N/A |
| GUX-08 | Goldfish mode | manual-only | Select Goldfish, start game, verify AI does nothing | N/A |
| CARD-04 | Oracle text in hover preview | manual-only | Hover over card, verify oracle text panel appears | N/A |

### Sampling Rate
- **Per task commit:** `cd forge-gui-web/frontend && npm run build` (type check + build)
- **Per wave merge:** `cd forge-gui-web/frontend && npm run lint && npm run build`
- **Phase gate:** Full build green + manual gameplay test

### Wave 0 Gaps
None -- no test framework to configure. Validation is build success + manual gameplay testing.

## Sources

### Primary (HIGH confidence)
- Direct codebase analysis of all referenced files (WebGuiGame.java, GameLog.java, GameLogFormatter.java, GameLogEntryType.java, GameLogEntry.java, AiProfileUtil.java, AIOption.java, LobbyPlayerAi.java, PlayerControllerAi.java, ActionBar.tsx, GameBoard.tsx, GameCard.tsx, gameStore.ts, gameWebSocket.ts, gameTypes.ts, StackPanel.tsx, PhaseStrip.tsx, GameHoverPreview.tsx, CardHoverPreview.tsx, AiSettings.tsx, GameLobby.tsx, tabs.tsx, MessageType.java, WebServer.java)
- `.planning/research/STACK.md` -- react-hotkeys-hook@5 recommendation
- `.planning/research/ARCHITECTURE.md` -- GameLog streaming and priority state design

### Secondary (MEDIUM confidence)
- npm registry: react-hotkeys-hook 5.2.4 (verified current version)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- only one new dependency (react-hotkeys-hook), verified version
- Architecture: HIGH -- all patterns derived from direct codebase analysis
- Pitfalls: HIGH -- identified from actual code inspection of current fragile patterns

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable patterns, no fast-moving dependencies)
