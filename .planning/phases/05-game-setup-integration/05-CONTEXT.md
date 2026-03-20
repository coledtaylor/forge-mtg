# Phase 5: Game Setup + Integration - Context

**Gathered:** 2026-03-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Lobby screen for starting games against the AI. Format selection, deck picker, optional AI settings (difficulty + deck), and the seamless flow from deck builder to game. Replaces the hardcoded default decks with user-selected decks.

</domain>

<decisions>
## Implementation Decisions

### Lobby Screen
- Separate top-level view in App.tsx (alongside deck list, deck editor, game board)
- Centered card layout: single panel with "Play a Game" heading
- Format selector dropdown at top — filters deck list below to show only matching decks
- Deck picker shows saved decks for the selected format as a selectable list (radio-style, one deck at a time)
- "Start Game" button at bottom — disabled until a deck is selected
- Empty state when no decks match format: "No decks for {format}. Create one in the Deck Builder." with link to deck builder

### AI Settings
- Collapsible "AI Settings" section below the deck picker
- Collapsed by default, shows summary text: "Default difficulty · Random deck"
- Expand to reveal: difficulty dropdown (Easy/Medium/Hard, default Medium) and AI deck dropdown (random format-appropriate deck as default, or select a specific preconstructed deck)
- If user doesn't touch AI settings, game uses default difficulty + random AI deck matching the format

### Deck-to-Game Flow
- "Play This Deck" button in the deck editor header
- Clicking navigates to the lobby with the current deck pre-selected and format pre-filled from the deck's format field
- After a game ends, "Return to Lobby" takes user back to lobby with the same format and deck pre-selected (one click to rematch)

### Backend Changes
- Replace hardcoded `getDefaultDeck()` / `getDefaultAiDeck()` with user-selected deck loaded from the constructed deck directory
- START_GAME inbound message needs to include: player deck name, AI deck name (or "random"), format, AI difficulty level
- Backend resolves deck names to actual Deck objects and configures AI difficulty

### Claude's Discretion
- AI preconstructed deck discovery (scan existing .dck files, or curate a list)
- AI difficulty mapping to Forge's AI configuration
- Lobby loading/connecting states
- Transition animations between views
- How "random deck" selection works (any deck in format, or a curated subset)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Game Start (Phase 1)
- `forge-gui-web/src/main/java/forge/web/WebServer.java` — `handleStartGame()` creates game session, `getDefaultDeck()`/`getDefaultAiDeck()` need replacement
- `forge-gui-web/src/main/java/forge/web/protocol/InboundMessage.java` — START_GAME payload format
- `forge-gui-web/src/main/java/forge/web/protocol/MessageType.java` — Message types

### Deck Management (Phase 2/3)
- `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` — Deck CRUD, `findDeckFile()` for resolving deck names
- `forge-gui-web/frontend/src/types/deck.ts` — `DeckSummary` with format field
- `forge-gui-web/frontend/src/hooks/useDecks.ts` — `useDecks()` hook for listing decks
- `forge-gui-web/frontend/src/api/decks.ts` — Deck API client

### Game Board (Phase 4)
- `forge-gui-web/frontend/src/components/game/GameBoard.tsx` — Game board component, expects `gameId` prop
- `forge-gui-web/frontend/src/components/game/GameOverScreen.tsx` — "Return to Lobby" navigation target
- `forge-gui-web/frontend/src/lib/gameWebSocket.ts` — WebSocket manager, sends START_GAME
- `forge-gui-web/frontend/src/App.tsx` — View routing (list, editor, game — needs lobby view added)

### Forge AI Configuration
- `forge-game/src/main/java/forge/player/GamePlayerUtil.java` — `createAiPlayer()` method
- `forge-localinstance/src/main/java/forge/localinstance/properties/ForgeConstants.java` — `DECK_CONSTRUCTED_DIR` for finding decks

No external specs — requirements fully captured in decisions above and REQUIREMENTS.md (SETUP-01 through SETUP-04).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `useDecks()` hook: Already fetches deck list with format field — can filter client-side by format
- `DeckSummary` type: Has `name`, `cardCount`, `colors`, `format`, `path` — everything needed for the lobby deck picker
- View routing pattern in `App.tsx`: Already has `type: 'list' | 'editor' | 'game'` — add `'lobby'`
- shadcn/ui `select`, `button`, `card`, `dialog`, `badge`: All available for lobby UI
- `GameOverScreen`: Already has "Return to Lobby" button — needs to navigate to lobby view with state

### Established Patterns
- State-based view routing in App.tsx (no react-router)
- TanStack Query for REST data fetching
- Tailwind CSS + shadcn/ui dark theme
- WebSocket `START_GAME` message triggers game session creation

### Integration Points
- `App.tsx` view state needs `'lobby'` type with optional pre-selected deck/format
- `WebServer.handleStartGame()` needs to accept deck name + AI config in START_GAME payload
- `gameWebSocket.ts` `connect()` or `sendStartGame()` needs to pass deck/AI config
- `GameOverScreen` "Return to Lobby" needs to pass back the last-used deck/format
- `DeckEditor` needs a "Play This Deck" button that navigates to lobby with deck context

</code_context>

<specifics>
## Specific Ideas

- Lobby should feel like a clean pre-game screen, not a settings page — "Play a Game" heading, focused layout
- AI settings hidden by default so the happy path is: pick format → pick deck → Start Game (3 clicks)
- "Play This Deck" from deck editor is the power-user shortcut — format + deck both pre-filled, just hit Start

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-game-setup-integration*
*Context gathered: 2026-03-19*
