---
phase: 05-game-setup-integration
verified: 2026-03-20T15:00:00Z
status: passed
score: 8/8 must-haves verified
re_verification: false
---

# Phase 5: Game Setup Integration Verification Report

**Phase Goal:** Users can go from building a deck to playing it against the AI in one seamless flow
**Verified:** 2026-03-20
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | User can select a format from a dropdown (Commander, Standard, Casual 60-card, Jumpstart) and see only matching decks | VERIFIED | `GameLobby.tsx:35-40` defines `FORMAT_OPTIONS` with all 4 values; `matchesFormat()` filters deck list client-side on format change |
| 2 | User can select a deck from the filtered list (radio-style, one at a time) | VERIFIED | `DeckPicker.tsx:64-93` renders each deck as a clickable div; `isSelected` drives `border-l-[3px] border-primary bg-primary/10` styling; `onSelect` replaces single string state |
| 3 | User can click Start Game and a game session starts with the selected deck against the AI | VERIFIED | `GameLobby.tsx:86-96` calls `onStartGame` with `gameConfig`; `App.tsx:57-67` routes to game view; `gameWebSocket.ts:46-53` sends `START_GAME` with config on `onopen`; `WebServer.java:186-267` receives payload and starts match |
| 4 | Start Game button is disabled until a deck is selected | VERIFIED | `GameLobby.tsx:147` — `disabled={!selectedDeck \|\| isStarting}` |
| 5 | AI settings collapsible section allows difficulty and AI deck selection | VERIFIED | `AiSettings.tsx` — `useState(false)` for `expanded`; ChevronRight/ChevronDown toggle; Difficulty and AI Deck Select dropdowns rendered when expanded |
| 6 | User can click "Play This Deck" in the deck editor and arrive at the lobby with deck and format pre-selected | VERIFIED | `DeckPanel.tsx:82-87` renders Play button; `DeckEditor.tsx:54-57` calls `flushSave()` then `onPlayDeck?.()`; `App.tsx:76-80` navigates to `{ type: 'lobby', preSelectedDeck: view.deckName, preSelectedFormat: view.format }` |
| 7 | After a game ends, "Return to Lobby" takes user back to lobby with the same format and deck pre-selected | VERIFIED | `GameOverScreen.tsx:45-47` calls `onReturnToLobby`; `GameBoard.tsx:215` wires `onReturnToLobby={onExit}`; `App.tsx:37-47` returns to lobby with `view.returnState.deckName` and `view.returnState.format` |
| 8 | When a user starts a game from the lobby, the game launches using the selected deck and AI difficulty setting | VERIFIED | `gameWebSocket.ts:49-52` — `started` flag ensures `sendStartGame(this.gameConfig)` fires exactly once on first connect; `WebServer.java:208-216` maps Easy/Medium/Hard to Cautious/Default/Reckless profiles and Commander to `GameType.Commander` |

**Score:** 8/8 truths verified

---

### Required Artifacts

| Artifact | min_lines | Actual Lines | Status | Notes |
|----------|-----------|-------------|--------|-------|
| `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` | 80 | 173 | VERIFIED | Format selector, DeckPicker, AiSettings, Start button, returnState wiring all present |
| `forge-gui-web/frontend/src/components/lobby/DeckPicker.tsx` | 40 | 96 | VERIFIED | Radio-style selection, color dots, skeleton loading, empty state |
| `forge-gui-web/frontend/src/components/lobby/AiSettings.tsx` | 30 | 93 | VERIFIED | Collapsible with ChevronRight/ChevronDown, Difficulty and AI Deck dropdowns |
| `forge-gui-web/src/main/java/forge/web/WebServer.java` | — | — | VERIFIED | `loadDeckByName`, `pickRandomDeck`, `scanAndLoadDeck`, `collectDecksByFormat`, AI profile switch, GameType mapping all present |
| `forge-gui-web/frontend/src/components/deck-editor/DeckEditor.tsx` | — | 150 | VERIFIED | `onPlayDeck` prop, `handlePlayDeck` calls `flushSave()` then delegate |
| `forge-gui-web/frontend/src/components/game/GameBoard.tsx` | — | 218 | VERIFIED | `gameConfig` prop accepted, passed to `useGameWebSocket` |
| `forge-gui-web/frontend/src/hooks/useGameWebSocket.ts` | — | 33 | VERIFIED | Accepts `GameStartConfig`, passes to `ws.connect(gameConfig)` |
| `forge-gui-web/frontend/src/components/game/GameOverScreen.tsx` | — | 54 | VERIFIED | `onReturnToLobby` prop on "Return to Lobby" button |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `GameLobby.tsx` | `gameWebSocket.ts` | `sendStartGame` with config payload | WIRED | `GameLobby` calls `onStartGame(gameId, deckName, format, gameConfig)` which flows through App → GameBoard → useGameWebSocket → GameWebSocket.connect |
| `gameWebSocket.ts` | `WebServer.java` | `START_GAME` WebSocket message with config object | WIRED | `gameWebSocket.ts:166-173` sends `{ type: 'START_GAME', inputId: null, payload: config }`; `WebServer.java:199` receives via `instanceof Map` check |
| `App.tsx` | `GameLobby.tsx` | View union type includes `lobby` | WIRED | `App.tsx:21` — `{ type: 'lobby'; preSelectedDeck?: string; preSelectedFormat?: string }` in View union; lobby rendered at lines 52-68 |
| `DeckEditor.tsx` | `App.tsx` | `onPlayDeck` callback navigates to lobby view | WIRED | `DeckEditor.tsx:15` accepts `onPlayDeck?`; `App.tsx:76-80` provides it with `preSelectedDeck`/`preSelectedFormat` |
| `useGameWebSocket.ts` | `gameWebSocket.ts` | `ws.connect(gameConfig)` passes config to WebSocket onopen | WIRED | `useGameWebSocket.ts:23` — `ws.connect(gameConfig)`; `gameWebSocket.ts:49-52` — fires `sendStartGame` on `onopen` if `!started` |
| `GameOverScreen.tsx` | `App.tsx` | `onReturnToLobby` triggers lobby view with returnState | WIRED | `GameOverScreen.tsx:45` button calls `onReturnToLobby`; `GameBoard.tsx:215` wires `onReturnToLobby={onExit}`; `App.tsx:37-47` checks `view.returnState` and navigates to lobby |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| SETUP-01 | 05-01-PLAN.md | User can select a format (Commander, Standard, casual 60-card, Jumpstart) | SATISFIED | `FORMAT_OPTIONS` array in `GameLobby.tsx`; `matchesFormat()` filters deck list |
| SETUP-02 | 05-01-PLAN.md | User can select a deck from saved decks for the game | SATISFIED | `DeckPicker.tsx` renders `useDecks()` data filtered by format; radio-style selection in `GameLobby` state |
| SETUP-03 | 05-01-PLAN.md | User can start a game against the AI | SATISFIED | Start Game button triggers `handleStartGame`; backend creates `RegisteredPlayer` with AI using `createAiPlayer` and resolved AI deck |
| SETUP-04 | 05-02-PLAN.md | User can navigate from deck builder to game with the current deck pre-selected | SATISFIED | "Play This Deck" in `DeckPanel.tsx` calls `onPlayDeck`; `App.tsx` routes to lobby with `preSelectedDeck`/`preSelectedFormat` |

No orphaned requirements — REQUIREMENTS.md maps only SETUP-01 through SETUP-04 to Phase 5, and both plans claim exactly these IDs.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `GameLobby.tsx` | 69 | `return []` when no format selected | Info | Correct behavior — empty array while format not yet selected, not a stub |
| `GameLobby.tsx` | 111 | `placeholder="Select a format"` string | Info | SelectValue UI placeholder text, not an implementation stub |

No blockers or warnings found.

---

### Human Verification Required

#### 1. Lobby renders correctly with no decks

**Test:** Start the app with no `.dck` files in the user's deck directory, navigate to lobby, select a format.
**Expected:** "No decks for {format}." message appears with "Create one in the Deck Builder." link.
**Why human:** File system state varies per environment; can't verify empty-state rendering programmatically.

#### 2. Format filtering correctness for "Casual 60-card"

**Test:** Create a deck with no format set (empty comment), navigate to lobby, select "Casual 60-card".
**Expected:** The deck appears in the filtered list.
**Why human:** The `matchesFormat()` logic covers `df === ''` for this case but requires a real deck to exercise the codepath.

#### 3. Start Game end-to-end with selected deck

**Test:** Select a format, select a deck, click "Start Game". Watch the game board load.
**Expected:** The game board connects, the START_GAME message is sent with the selected deck name, and the backend resolves the correct deck (not the hardcoded default).
**Why human:** Requires a running backend and actual deck files; the START_GAME payload transmission through the WebSocket cannot be inspected statically.

#### 4. Return to Lobby with preserved state

**Test:** Start a game, wait for game over, click "Return to Lobby".
**Expected:** The lobby appears with the same format and deck pre-selected that were used to start the game.
**Why human:** Requires full game loop completion; behavior is a runtime state machine, not statically verifiable.

---

### Verified Commits

All four phase commits exist in git history and were verified:

| Hash | Description |
|------|-------------|
| `cb6931efaa` | feat(05-01): add config payload handling to START_GAME with deck resolution |
| `e0ad4476ef` | feat(05-01): add lobby UI with format/deck selection, AI settings, and game start flow |
| `16ba5f470d` | feat(05-02): add Play This Deck button in deck editor |
| `65407e0696` | fix(05-02): prevent START_GAME re-send on WebSocket reconnect |

---

### Summary

All 8 observable truths verified. All 4 requirements (SETUP-01 through SETUP-04) satisfied by concrete implementation in the codebase. All key links are wired end-to-end. No blocker or warning anti-patterns. Four human verification items cover runtime behavior that requires a live environment.

The phase goal — users can go from building a deck to playing it against the AI in one seamless flow — is achieved:

- Deck list has a "Play a Game" button going directly to the lobby
- Deck editor has a "Play This Deck" button that flushes save and pre-selects the current deck in the lobby
- The lobby provides format filtering, deck selection, and AI configuration before starting
- The backend resolves deck names to actual Deck objects and maps AI difficulty to engine profiles
- After a game ends, "Return to Lobby" returns with format and deck pre-selected

---

_Verified: 2026-03-20_
_Verifier: Claude (gsd-verifier)_
