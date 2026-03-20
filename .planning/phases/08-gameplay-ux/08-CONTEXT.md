# Phase 8: Gameplay UX - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Improve the gameplay experience with clear priority information, visual targeting feedback, a readable action log, keyboard shortcuts, and flexible game setup options (AI difficulty, goldfish mode). Also add oracle text display alongside card images. This is the largest UX phase — all changes are to the game board and its components.

</domain>

<decisions>
## Implementation Decisions

### Priority & Action Clarity
- Pulsing primary-color border on the ActionBar when the player has priority, with text "You have priority"
- Dimmed ActionBar with "Waiting for opponent..." when player does not have priority
- Two distinct buttons: "Confirm" (green/primary) for confirming actions, "Pass" (muted/secondary) for passing priority
- Only show Confirm when there's something to confirm; always show Pass when player has priority
- Backend needs to send priority state — new WebSocket message type or flag in BUTTON_UPDATE

### Keyboard Shortcuts
- Arena-style: Space/Enter = Pass Priority, Escape = Cancel, Z = Undo
- Show shortcut hints on buttons (e.g., "Pass [Space]", "Cancel [Esc]")
- Use `react-hotkeys-hook@5` for keybinding management
- Simple, muscle-memory friendly — no F-key complexity

### Targeting & Selection UX
- When targeting: valid targets get a glowing primary-color ring, invalid cards dim to 40% opacity
- Click a valid target to select it — selected target gets a bright checkmark/ring
- Cancel button or Escape exits targeting mode
- Multi-target: click to toggle selection with numbered badges (1, 2, 3). Show count "2/3 selected". Confirm button appears once minimum met
- Fix fragile card-name matching — use card IDs from prompt choices to match battlefield cards directly

### Game Log
- Tabbed panel in the right column: "Stack" tab (existing stack display) and "Log" tab (scrollable game log)
- Full detail with type icons: every game action (land plays, spell casts, combat damage, life changes, triggers)
- Color-coded by entry type, turn/phase separators, auto-scroll to latest
- Backend: stream GameLog entries via WebSocket (new GAME_LOG message type, or repurpose existing MESSAGE type)
- Engine's GameLog has 18 entry types and is Observable — subscribe and forward entries

### AI Difficulty
- AI difficulty selector already exists in AiSettings.tsx (GameLobby)
- Already wired to backend via GameStartConfig.aiDifficulty → WebServer.handleStartGame maps Easy/Medium/Hard to AI profiles
- Requirement GUX-07 is already satisfied from v1.0 Phase 5 — verify and mark complete

### Goldfish / Solitaire Mode
- Add "Goldfish" option to the game lobby — plays against a dummy AI that does nothing (passes every priority, never casts spells)
- Useful for testing combos, measuring goldfish kill turns
- Backend: create a pass-only AI profile or skip AI turn entirely

### Oracle Text Display
- Show oracle text in the card hover preview (GameHoverPreview component and CardHoverPreview)
- Below the card image, show a text panel with: card name, mana cost, type line, oracle text, P/T
- CardDto already has oracleText field (added in v1.0 Phase 1)
- For deck builder hover, DeckCardEntry also has oracle text available

### Claude's Discretion
- Exact animation timing for priority pulse
- Game log entry formatting (timestamp format, icon set)
- How to subscribe to GameLog Observable from WebGuiGame
- Goldfish AI implementation details (pass-only profile vs skip AI)
- Oracle text panel layout and typography in hover preview

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Game UI Components
- `forge-gui-web/frontend/src/components/game/ActionBar.tsx` — Current button/prompt UI, needs priority indicator + two-button redesign
- `forge-gui-web/frontend/src/components/game/PhaseStrip.tsx` — Phase indicator, may need priority dot
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` — Main layout, targeting logic, card click handlers
- `forge-gui-web/frontend/src/components/game/GameCard.tsx` — Highlight modes (valid-target, invalid, attacker, blocker, playable)
- `forge-gui-web/frontend/src/components/game/ChoiceDialog.tsx` — Current choice selection UI
- `forge-gui-web/frontend/src/components/game/StackPanel.tsx` — Will become a tab in the new tabbed panel
- `forge-gui-web/frontend/src/components/game/GameHoverPreview.tsx` — Hover preview, needs oracle text
- `forge-gui-web/frontend/src/components/game/HandCard.tsx` — Hand card interaction

### State & WebSocket
- `forge-gui-web/frontend/src/stores/gameStore.ts` — Zustand store, needs gameLog and priority fields
- `forge-gui-web/frontend/src/lib/gameWebSocket.ts` — Message handler switch, needs GAME_LOG handler
- `forge-gui-web/frontend/src/lib/gameTypes.ts` — Message types, needs GAME_LOG type

### Backend
- `forge-gui-web/src/main/java/forge/web/WebGuiGame.java` — IGuiGame implementation, needs GameLog streaming
- `forge-gui-web/frontend/src/components/lobby/GameLobby.tsx` — Lobby for AI difficulty and goldfish mode
- `forge-gui-web/frontend/src/components/lobby/AiSettings.tsx` — AI settings collapsible panel

### Research
- `.planning/research/STACK.md` — Confirms react-hotkeys-hook@5 for keyboard shortcuts
- `.planning/research/ARCHITECTURE.md` — GameLog Observable pattern, priority state architecture
- `.planning/research/FEATURES.md` — Table stakes analysis for gameplay UX

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GameCard.tsx` highlight modes: already has `valid-target`, `invalid`, `playable` classes — can be leveraged for targeting UX
- `CombatOverlay.tsx` SVG arrow pattern: could inspire targeting arrows if needed later
- `StackPanel.tsx`: will be wrapped in a tab component alongside the new game log
- `AiSettings.tsx`: already has difficulty selector UI

### Established Patterns
- Zustand + immer for game state (add gameLog array, priority boolean)
- WebSocket message switch in gameWebSocket.ts (add GAME_LOG case)
- shadcn/ui Tabs component available for stack/log tabbed panel
- react-hotkeys-hook@5 is the only new dependency (Stack research confirmed)

### Integration Points
- `WebGuiGame.java` fire-and-forget methods: add GameLog entry sending alongside existing MESSAGE sends
- `gameStore.ts` actions: add `addLogEntry()`, `setHasPriority()`
- `ActionBar.tsx`: redesign button layout with Confirm/Pass split
- `GameBoard.tsx`: enhance targeting mode with card ID matching instead of name matching

</code_context>

<specifics>
## Specific Ideas

- Priority indicator should feel like Arena's orange glow — immediate visual feedback
- Game log should be clean like Arena's log but with full detail like MTGO — best of both
- Keyboard shortcuts should be instantly learnable — show hints on buttons
- Targeting should feel responsive — no ambiguity about which card is selected

</specifics>

<deferred>
## Deferred Ideas

- Auto-yield per phase (Phase 9 — requires deeper engine integration)
- Undo last spell (Phase 9 — requires engine's ManaRefundService)
- Targeting arrows from source to target (could add later, highlight mode is sufficient for now)

</deferred>

---

*Phase: 08-gameplay-ux*
*Context gathered: 2026-03-20*
