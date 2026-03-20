# Phase 5: Game Setup + Integration - Research

**Researched:** 2026-03-19
**Domain:** Lobby UI, game startup flow, deck selection, AI configuration
**Confidence:** HIGH

## Summary

Phase 5 connects the deck builder (Phase 3) and game board (Phase 4) with a lobby screen that lets users select a format, pick a deck, configure AI settings, and start a game. The backend changes are surgical: replace `getDefaultDeck()`/`getDefaultAiDeck()` in `WebServer.handleStartGame()` with user-selected decks loaded via `DeckHandler.findDeckFile()` patterns, and pass AI profile configuration through `GamePlayerUtil.createAiPlayer(name, profileOverride)`.

The frontend is a new `'lobby'` view in `App.tsx`'s state-based routing, reusing the existing `useDecks()` hook for deck listing and `DeckSummary.format` field for client-side filtering. The `GameWebSocket.sendStartGame()` method needs to include deck name, AI deck preference, format, and difficulty in its payload. The `GameOverScreen` already has a "Return to Lobby" button -- it just needs to pass back the last-used deck/format state.

**Primary recommendation:** Build the lobby as a single `GameLobby.tsx` component with local state for format/deck/AI settings. Wire the backend payload changes first (they are the smallest risk), then build the UI, then connect the deck-to-game shortcuts.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Lobby is a separate top-level view in App.tsx (alongside deck list, deck editor, game board)
- Centered card layout: single panel with "Play a Game" heading
- Format selector dropdown at top -- filters deck list below to show only matching decks
- Deck picker shows saved decks for the selected format as a selectable list (radio-style, one deck at a time)
- "Start Game" button at bottom -- disabled until a deck is selected
- Empty state when no decks match format: "No decks for {format}. Create one in the Deck Builder." with link to deck builder
- Collapsible "AI Settings" section below deck picker, collapsed by default with summary text
- AI settings: difficulty dropdown (Easy/Medium/Hard, default Medium) and AI deck dropdown (random or specific preconstructed deck)
- "Play This Deck" button in the deck editor header
- Clicking navigates to lobby with current deck pre-selected and format pre-filled
- After game ends, "Return to Lobby" takes user back to lobby with same format/deck pre-selected
- Replace hardcoded getDefaultDeck()/getDefaultAiDeck() with user-selected deck
- START_GAME payload includes: player deck name, AI deck name (or "random"), format, AI difficulty level
- Backend resolves deck names to Deck objects and configures AI difficulty

### Claude's Discretion
- AI preconstructed deck discovery (scan existing .dck files, or curate a list)
- AI difficulty mapping to Forge's AI configuration
- Lobby loading/connecting states
- Transition animations between views
- How "random deck" selection works (any deck in format, or a curated subset)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SETUP-01 | User can select a format (Commander, Standard, casual 60-card, Jumpstart) | Format selector dropdown filters useDecks() results by DeckSummary.format field |
| SETUP-02 | User can select a deck from saved decks for the game | Deck picker uses useDecks() hook, radio-style selection from filtered list |
| SETUP-03 | User can start a game against the AI | START_GAME payload extended with deckName/aiDeck/format/difficulty; backend resolves via DeckHandler patterns |
| SETUP-04 | User can navigate from deck builder to game with current deck pre-selected | "Play This Deck" button in DeckEditor navigates to lobby view with pre-filled state |
</phase_requirements>

## Standard Stack

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.x | UI framework | Project standard |
| Zustand + immer | 5.x | State management (gameStore) | Project standard |
| TanStack Query | 5.x | REST data fetching (useDecks) | Project standard |
| shadcn/ui (base-ui) | latest | UI components (Select, Button, Card, Badge) | Project standard |
| Tailwind CSS | 4.x | Styling | Project standard |

### Backend (Already in Project)
| Library | Version | Purpose |
|---------|---------|---------|
| Javalin | 7.x | HTTP server + WebSocket |
| Jackson | 2.x | JSON serialization |
| Forge engine | local | AI profiles, deck loading, game creation |

### No New Dependencies Required
This phase uses only existing libraries. No new npm or Maven dependencies needed.

## Architecture Patterns

### Recommended Project Structure
```
forge-gui-web/frontend/src/
  components/
    lobby/
      GameLobby.tsx          # Main lobby view component
      FormatSelector.tsx     # Format dropdown (could be inline)
      DeckPicker.tsx         # Filtered deck list with radio selection
      AiSettings.tsx         # Collapsible AI configuration panel
  App.tsx                    # Add 'lobby' to View union type
  components/
    deck-editor/
      DeckEditor.tsx         # Add "Play This Deck" button
    game/
      GameOverScreen.tsx     # Update onReturnToLobby to pass state

forge-gui-web/src/main/java/forge/web/
  WebServer.java             # Update handleStartGame to accept payload
  protocol/
    InboundMessage.java      # Already sufficient (payload is Object)
    MessageType.java         # No changes needed
```

### Pattern 1: View State Routing (Existing Pattern)
**What:** App.tsx uses a discriminated union for view state, no react-router
**When to use:** Always -- this is the established project pattern
**Example:**
```typescript
// Current (App.tsx line 16-19):
type View =
  | { type: 'list' }
  | { type: 'editor'; deckName: string; format?: string }
  | { type: 'game'; gameId: string }

// Extended for Phase 5:
type View =
  | { type: 'list' }
  | { type: 'editor'; deckName: string; format?: string }
  | { type: 'lobby'; preSelectedDeck?: string; preSelectedFormat?: string }
  | { type: 'game'; gameId: string }
```

### Pattern 2: START_GAME Payload Extension
**What:** The START_GAME WebSocket message currently sends null payload. Extend it to carry game configuration.
**When to use:** When the client sends START_GAME
**Example:**
```typescript
// Frontend: gameWebSocket.ts
sendStartGame(config: {
  deckName: string
  aiDeckName: string | null  // null = random
  format: string
  aiDifficulty: string       // 'Easy' | 'Medium' | 'Hard'
}): void {
  this.send({ type: 'START_GAME', inputId: null, payload: config })
}
```

```java
// Backend: WebServer.handleStartGame() extracts from payload
@SuppressWarnings("unchecked")
Map<String, Object> config = (Map<String, Object>) msg.getPayload();
String deckName = (String) config.get("deckName");
String aiDeckName = (String) config.get("aiDeckName"); // nullable
String format = (String) config.get("format");
String aiDifficulty = (String) config.get("aiDifficulty");
```

### Pattern 3: Deck Resolution (Existing Pattern in DeckHandler)
**What:** Load a user deck by name from DECK_CONSTRUCTED_DIR using DeckSerializer
**When to use:** In handleStartGame when resolving player/AI deck names
**Example:**
```java
// Reuse DeckHandler's findDeckFile pattern (currently private -- extract to package-level)
File deckFile = findDeckFile(deckName);
Deck playerDeck = DeckSerializer.fromFile(deckFile);
```

### Pattern 4: AI Profile Mapping
**What:** Map Easy/Medium/Hard to Forge AI profile names
**When to use:** When configuring AI player in handleStartGame
**Research findings:** Forge has 4 AI profiles in `forge-gui/res/ai/`:
- `Cautious.ai` -- conservative play, good for "Easy"
- `Default.ai` -- balanced play, good for "Medium"
- `Reckless.ai` -- aggressive play, good for "Hard"
- `Experimental.ai` -- unpredictable, not suitable for difficulty mapping

**Recommended mapping:**
```java
String profile = switch (aiDifficulty) {
    case "Easy" -> "Cautious";
    case "Hard" -> "Reckless";
    default -> "Default"; // Medium
};
LobbyPlayer aiLobby = GamePlayerUtil.createAiPlayer(
    GuiDisplayUtil.getRandomAiName(), profile);
```

### Pattern 5: Random AI Deck Selection
**What:** When user picks "random" for AI deck, backend picks a random user-saved deck matching the format
**Recommendation:** Scan DECK_CONSTRUCTED_DIR for .dck files, filter by format (stored in deck comment field), pick random. If no decks match format, fall back to a basic lands deck (same as current default). This avoids needing curated AI deck lists, which don't exist in the data directory (0 preconstructed .dck files found).
**Alternative:** Create a small set of hardcoded AI decks per format. More work, less flexibility, but guarantees playable AI decks even when user has no saved decks.
**Recommendation:** Use user's own saved decks as AI deck pool. If no decks exist for the format, use the Mountains/Forests fallback and log a warning. This is simplest and correct for v1 -- users will always have at least the deck they're playing with.

### Anti-Patterns to Avoid
- **Don't add react-router:** The project uses state-based view routing. Adding a router would break the established pattern.
- **Don't make DeckHandler.findDeckFile public:** Instead, extract a shared utility method or duplicate the pattern in WebServer. DeckHandler is a REST handler, not a shared service.
- **Don't store lobby state in Zustand:** Lobby state is local (format, selected deck, AI settings). Use React useState. Game state is in Zustand because it streams from WebSocket.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dropdown menus | Custom dropdown | shadcn/ui `Select` component | Already in project, handles accessibility |
| Collapsible section | Custom accordion | HTML `<details>`/`<summary>` or simple state toggle | Lightweight, no library needed |
| Deck list fetching | Manual fetch | `useDecks()` hook from Phase 3 | Already exists, uses TanStack Query |
| Format filtering | Backend endpoint | Client-side `.filter()` on `DeckSummary.format` | Data is already loaded, format is on each summary |
| Unique game ID | UUID library | `crypto.randomUUID()` | Built into browsers, already used in gameWebSocket |

## Common Pitfalls

### Pitfall 1: Format Field Mismatch
**What goes wrong:** User creates a deck with format "Commander" but the lobby filters by "commander" (case mismatch), showing no results.
**Why it happens:** `DeckSummaryDto.format` comes from `deck.getComment()` which stores whatever string the frontend sent during deck creation. No normalization.
**How to avoid:** Use case-insensitive comparison when filtering decks by format. The `CreateDeckPayload.format` sent from the deck builder uses title-case ("Commander", "Standard"), so the lobby should match the same casing or normalize both.
**Warning signs:** Deck count in lobby shows 0 when you know decks exist.

### Pitfall 2: WebSocket Message Backward Compatibility
**What goes wrong:** Changing the START_GAME payload format breaks existing game start flow if frontend and backend are out of sync during development.
**Why it happens:** The payload is `Object` (not typed), so Jackson will deserialize whatever JSON it receives.
**How to avoid:** Backend should handle both old (null payload) and new (config object) START_GAME messages. Check if payload is null and use defaults as fallback.
**Warning signs:** Game won't start, NullPointerException in handleStartGame.

### Pitfall 3: DeckSerializer.fromFile Returns Null
**What goes wrong:** User selects a deck, starts game, but deck file is corrupted or deleted between list and start.
**Why it happens:** Race condition between REST deck list and WebSocket game start.
**How to avoid:** Null-check the deserialized deck and send an ERROR message back through WebSocket if deck can't be loaded.
**Warning signs:** NullPointerException in handleStartGame, game silently fails to start.

### Pitfall 4: GameType Mismatch
**What goes wrong:** Using `GameType.Commander` for Commander games but `GameType.Constructed` for Standard/casual, and getting wrong deck validation.
**Why it happens:** Forge engine uses `GameType` to determine deck format rules. Each GameType maps to a DeckFormat.
**How to avoid:** Map the user's format selection to the correct `GameType` enum. "Commander" -> `GameType.Commander`, "Standard"/"casual 60-card" -> `GameType.Constructed`, "Jumpstart" -> `GameType.Constructed` (basic constructed rules).
**Warning signs:** Engine rejects valid decks or allows invalid ones.

### Pitfall 5: View State Lost on Game Over
**What goes wrong:** User clicks "Return to Lobby" after game ends but loses their format/deck selection.
**Why it happens:** `onExit` in GameBoard/GameOverScreen currently navigates to `{ type: 'list' }` (deck list). Need to navigate to `{ type: 'lobby', preSelectedDeck, preSelectedFormat }` instead.
**How to avoid:** Store the game config (deck name, format) before navigating to game view. Pass it through the view state chain: lobby -> game -> game over -> lobby.

## Code Examples

### Example 1: Extended View Type and Lobby Navigation
```typescript
// App.tsx - Updated View type
type View =
  | { type: 'list' }
  | { type: 'editor'; deckName: string; format?: string }
  | { type: 'lobby'; preSelectedDeck?: string; preSelectedFormat?: string }
  | { type: 'game'; gameId: string; returnState?: { deckName: string; format: string } }

// Navigation from deck list to lobby
<Button onClick={() => setView({ type: 'lobby' })}>Play a Game</Button>

// Navigation from deck editor to lobby with pre-selection
<Button onClick={() => {
  flushSave()
  setView({ type: 'lobby', preSelectedDeck: deckName, preSelectedFormat: format })
}}>Play This Deck</Button>

// Navigation from game over back to lobby
const handleReturnToLobby = () => {
  if (view.type === 'game' && view.returnState) {
    setView({
      type: 'lobby',
      preSelectedDeck: view.returnState.deckName,
      preSelectedFormat: view.returnState.format,
    })
  } else {
    setView({ type: 'lobby' })
  }
}
```

### Example 2: Backend handleStartGame with Deck Resolution
```java
private static void handleStartGame(final WsContext ctx, final String gameId, final InboundMessage msg) {
    if (activeSessions.containsKey(gameId)) {
        Logger.warn("Game session already exists for gameId: {}", gameId);
        return;
    }

    // Parse config from payload (backward-compatible)
    String deckName = null;
    String aiDeckName = null;
    String format = "Constructed";
    String aiDifficulty = "Default";

    if (msg.getPayload() instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) msg.getPayload();
        deckName = (String) config.get("deckName");
        aiDeckName = (String) config.get("aiDeckName");
        format = (String) config.getOrDefault("format", "Constructed");
        aiDifficulty = (String) config.getOrDefault("aiDifficulty", "Medium");
    }

    // Resolve player deck
    Deck playerDeck = (deckName != null) ? loadDeckByName(deckName) : getDefaultDeck();
    Deck aiDeck = (aiDeckName != null) ? loadDeckByName(aiDeckName) : pickRandomDeck(format);

    // Map difficulty to AI profile
    String profile = switch (aiDifficulty) {
        case "Easy" -> "Cautious";
        case "Hard" -> "Reckless";
        default -> "Default";
    };

    // ... rest of game setup with playerDeck, aiDeck, profile
}
```

### Example 3: Format Filtering on Deck List
```typescript
// Inside GameLobby component
const { data: allDecks } = useDecks()
const [selectedFormat, setSelectedFormat] = useState<string>('Constructed')

const filteredDecks = useMemo(() => {
  if (!allDecks) return []
  if (selectedFormat === 'casual 60-card') {
    // Casual matches any constructed-style deck or decks with empty format
    return allDecks.filter(d =>
      d.format === '' || d.format.toLowerCase() === 'constructed' || d.format.toLowerCase() === 'casual 60-card'
    )
  }
  return allDecks.filter(d =>
    d.format.toLowerCase() === selectedFormat.toLowerCase()
  )
}, [allDecks, selectedFormat])
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded Mountain/Forest default decks | User-selected decks from constructed dir | Phase 5 | Users play real games with real decks |
| START_GAME with null payload | START_GAME with config object | Phase 5 | Full game configuration from client |
| No lobby screen, direct game start | Lobby with format/deck/AI selection | Phase 5 | 3-click game start flow |

## Open Questions

1. **Casual 60-card format string**
   - What we know: Deck format is stored as the deck comment field. "Commander" and "Standard" are clear. The requirement lists "casual 60-card" as a format option.
   - What's unclear: What format string do casual decks have? Likely empty string or "Constructed".
   - Recommendation: Treat empty-format decks and "Constructed" decks as matching the "casual 60-card" selection. The lobby format list shows: Commander, Standard, Casual 60-card, Jumpstart.

2. **Jumpstart format**
   - What we know: Jumpstart is listed as a required format option. Forge has no `GameType.Jumpstart`.
   - What's unclear: Whether any user decks will have "Jumpstart" as their format.
   - Recommendation: Include it in the dropdown for completeness but use `GameType.Constructed` on the backend. If no decks match, the empty state message guides users to create one.

3. **AI deck pool when user has no saved decks**
   - What we know: DECK_CONSTRUCTED_DIR has 0 preconstructed .dck files. AI "random deck" scans user-saved decks.
   - What's unclear: What happens when user has only 1 deck (the one they're playing) -- AI gets the same deck?
   - Recommendation: Allow it for v1. Both players having the same deck is a valid casual game. A future enhancement could ship curated AI decks.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (backend), Vitest (frontend -- not yet configured) |
| Config file | forge-gui-web/build.gradle (JUnit), frontend/vite.config.ts (Vitest potential) |
| Quick run command | `cd forge-gui-web && ../gradlew test --tests "forge.web.*"` |
| Full suite command | `cd forge-gui-web && ../gradlew test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SETUP-01 | Format selection filters deck list | manual-only | N/A -- UI interaction, no frontend test framework | N/A |
| SETUP-02 | Deck selection from filtered list | manual-only | N/A -- UI interaction | N/A |
| SETUP-03 | START_GAME with config resolves decks/AI | unit | `../gradlew test --tests "forge.web.WebServerStartGameTest"` | No -- Wave 0 |
| SETUP-04 | Deck editor "Play This Deck" navigates to lobby | manual-only | N/A -- UI navigation | N/A |

### Sampling Rate
- **Per task commit:** `cd forge-gui-web && ../gradlew test --tests "forge.web.*"` (existing tests still pass)
- **Per wave merge:** `cd forge-gui-web && ../gradlew test`
- **Phase gate:** Full suite green + manual lobby flow verification

### Wave 0 Gaps
- [ ] `forge-gui-web/src/test/java/forge/web/WebServerStartGameTest.java` -- tests handleStartGame with config payload (deck resolution, AI profile mapping, null-payload backward compat)
- [ ] No frontend test framework configured -- all UI behavior is manual-only for v1

## Sources

### Primary (HIGH confidence)
- `WebServer.java` (lines 180-213) -- current handleStartGame with default decks, verified by direct file read
- `GamePlayerUtil.java` (lines 46-97) -- createAiPlayer overloads with profile parameter, verified by direct file read
- `AiProfileUtil.java` -- AI profile system: 4 profiles (Cautious, Default, Experimental, Reckless), verified by file listing + source read
- `DeckHandler.java` -- findDeckFile pattern, DeckSerializer.fromFile usage, verified by direct file read
- `App.tsx` -- View union type pattern, verified by direct file read
- `gameWebSocket.ts` -- sendStartGame sends null payload currently, verified by direct file read
- `DeckSummaryDto.java` -- format field from deck comment, verified by direct file read
- `GameType.java` -- Commander, Constructed enum values available, verified by direct file read

### Secondary (MEDIUM confidence)
- AI profile difficulty mapping (Cautious=Easy, Default=Medium, Reckless=Hard) -- based on profile names and Forge community convention, not formally documented

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in project, no new dependencies
- Architecture: HIGH -- extends established view routing pattern, verified all integration points
- Pitfalls: HIGH -- identified from direct code analysis of existing patterns
- AI difficulty mapping: MEDIUM -- profile names suggest difficulty but no official mapping exists

**Research date:** 2026-03-19
**Valid until:** 2026-04-19 (stable -- all code is local to this project)
