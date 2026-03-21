# Phase 11: Jumpstart Format - Research

**Researched:** 2026-03-20
**Domain:** Jumpstart format support (deck builder constraints, pack browsing, dual-pack game setup, backend merge)
**Confidence:** HIGH

## Summary

Phase 11 adds complete Jumpstart format support as a vertical slice touching three layers: the deck builder (20-card pack constraints), the game lobby (dual-pack picker replacing single deck picker), and the backend (pack listing API + pack merge at game start). All decisions are locked in CONTEXT.md -- no engine GameType changes needed, Jumpstart runs as `GameType.Constructed` with a merged 40-card deck.

The Forge engine already has robust Jumpstart pack data: 242 packs across JMP (121) and J22 (121) sets in `boosters-special.txt`, each defined as a `wholeSheet` reference to a printsheet. The existing `AdventureEventController.getJumpstartBoosters()` demonstrates the pattern: iterate `StaticData.instance().getSpecialBoosters()`, filter by edition code, open each `SealedTemplate` via `UnOpenedProduct`, and collect the resulting cards into `Deck` objects. We need a simpler version: just iterate and filter by JMP/J22 prefix, extract the theme name, and return pack metadata to the frontend.

The frontend codebase has clean patterns to extend. `DeckList.tsx` already has a format selector (needs "Jumpstart" added), `DeckEditor.tsx` passes format to `useDeckEditor` and `DeckPanel`, and `GameLobby.tsx` already has format-conditional patterns (e.g. Commander). The `DeckPicker.tsx` component is a simple radio-style list that can be reused directly for pack selection.

**Primary recommendation:** Build a new `GET /api/jumpstart/packs` endpoint that reads `StaticData.getSpecialBoosters()`, filters to JMP/J22 entries, extracts theme names and color identity, and returns pack summaries. On the frontend, add Jumpstart-conditional UI in GameLobby (dual PackPicker components) and adapt DeckEditor constraints for 20-card packs.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Reuse existing DeckEditor with Jumpstart-specific constraints: 20-card target instead of 60, format = "Jumpstart", no sideboard tab, validation enforces exactly 20 cards
- No new specialized view -- same editor, different rules
- "Browse Packs" button in the deck list when Jumpstart format is selected -- shows Forge's pre-built packs from `StaticData.getSpecialBoosters()` with theme names
- User can select a Forge pack to use directly or copy it to edit
- Packs saved as regular .dck files with format comment "Jumpstart"
- When Jumpstart is selected in the lobby: replace the single deck picker with two side-by-side pack pickers ("Pack 1" and "Pack 2")
- Each picker shows user's custom Jumpstart packs + Forge's built-in packs
- No merged deck preview -- just show the two selected pack names and Start button (surprise factor)
- Start button enabled only when both packs are selected
- Validation: exactly 2 packs selected before game can start
- AI randomly picks 2 packs from Forge's built-in pack pool -- different every game
- No GameType.Jumpstart -- use GameType.Constructed with merged 40-card deck
- Backend merges the two selected packs into a single 40-card deck at game start
- Same for AI: backend picks 2 random packs and merges them

### Claude's Discretion
- Pack browser UI layout (grid vs list of packs)
- How to present pack themes (icon, color indicator, card count)
- Exact API endpoints for listing available packs
- How to handle the 20-card validation message in the editor

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| JUMP-01 | User can create 20-card Jumpstart packs in the deck builder | DeckEditor already accepts `format` prop; DeckList needs "Jumpstart" in FORMAT_OPTIONS; useDeckEditor works unchanged; FormatValidationHandler already validates 20-card Jumpstart packs |
| JUMP-02 | User can browse and select from Forge's existing Jumpstart pack definitions | New `GET /api/jumpstart/packs` endpoint using `StaticData.getSpecialBoosters()` filtered by JMP/J22 prefix; frontend pack browser component |
| JUMP-03 | User can select two packs in game setup to merge into a 40-card deck | GameLobby needs Jumpstart-conditional dual-pack picker; GameStartConfig needs `pack1`/`pack2` fields; WebServer.handleStartGame needs merge logic |
| JUMP-04 | AI opponent selects two packs (random from available packs) | Backend picks 2 random packs from getSpecialBoosters() JMP/J22 pool, merges into AI deck |
| JUMP-05 | Jumpstart game setup validates exactly two packs selected before starting | Frontend: Start button disabled unless both pack1 and pack2 selected; Backend: handleStartGame validates both packs present |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.x | UI framework | Already in use |
| @tanstack/react-query | 5.x | Data fetching/caching | Already in use for all API calls |
| Javalin | 6.x | REST API server | Already in use |
| Forge StaticData | n/a | Pack definitions from boosters-special.txt | Engine's built-in Jumpstart data |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| lucide-react | existing | Icons for pack UI | Already in use throughout |
| shadcn/ui components | existing | Badge, Button, Select, etc. | Already in use |

No new dependencies needed. Everything builds on the existing stack.

## Architecture Patterns

### Recommended Project Structure
```
forge-gui-web/
  src/main/java/forge/web/
    api/
      JumpstartHandler.java        # NEW: GET /api/jumpstart/packs
    dto/
      JumpstartPackDto.java         # NEW: Pack summary DTO
    WebServer.java                  # MODIFY: handleStartGame pack merge + route registration
  frontend/src/
    api/
      jumpstart.ts                  # NEW: fetchJumpstartPacks()
    hooks/
      useJumpstartPacks.ts          # NEW: useQuery wrapper
    components/
      lobby/
        GameLobby.tsx               # MODIFY: Jumpstart-conditional dual picker
        PackPicker.tsx              # NEW: Dual-use picker for built-in + user packs
        DeckPicker.tsx              # UNCHANGED (reused pattern)
      deck-editor/
        DeckEditor.tsx              # MINOR: hide sideboard tab for Jumpstart
        DeckPanel.tsx               # MINOR: hide sideboard tab for Jumpstart
      DeckList.tsx                  # MODIFY: add "Jumpstart" to FORMAT_OPTIONS
    types/
      game.ts                       # MODIFY: add pack1/pack2 to GameStartConfig
      jumpstart.ts                  # NEW: JumpstartPack type
```

### Pattern 1: Backend Pack Listing
**What:** New REST endpoint that reads Forge's built-in Jumpstart pack definitions and returns pack summaries
**When to use:** Frontend needs to display available packs for browsing and game setup

The endpoint iterates `StaticData.instance().getSpecialBoosters()`, filters entries whose name starts with "JMP " or "J22 ", extracts the theme name (strip set code and variant number), opens each template via `UnOpenedProduct` to get actual card list, computes color identity, and returns DTO list.

```java
// Source: Forge engine pattern from AdventureEventController.getJumpstartBoosters()
public static void listPacks(final Context ctx) {
    List<JumpstartPackDto> packs = new ArrayList<>();
    for (SealedTemplate template : StaticData.instance().getSpecialBoosters()) {
        String name = template.getName(); // e.g. "JMP Above the Clouds 1"
        if (!name.startsWith("JMP ") && !name.startsWith("J22 "))
            continue;

        UnOpenedProduct product = new UnOpenedProduct(template);
        List<PaperCard> cards = product.get();

        // Extract theme: strip set code prefix, strip trailing variant number
        String setCode = name.substring(0, 3);
        String theme = name.substring(4).replaceAll("\\s+\\d+$", "");

        // Compute color identity from cards
        Set<String> colors = computeColors(cards);

        packs.add(new JumpstartPackDto(name, theme, setCode, cards.size(), colors));
    }
    ctx.json(packs);
}
```

### Pattern 2: Backend Pack Merge at Game Start
**What:** When format is "Jumpstart", load two packs and merge into one 40-card Deck
**When to use:** handleStartGame receives pack1 + pack2 instead of deckName

```java
// In handleStartGame, when format is "Jumpstart":
if ("Jumpstart".equalsIgnoreCase(format)) {
    String pack1Name = (String) config.get("pack1");
    String pack2Name = (String) config.get("pack2");

    // Load packs: check if user deck or built-in pack
    Deck pack1Deck = loadPackByName(pack1Name);
    Deck pack2Deck = loadPackByName(pack2Name);

    // Merge into single deck
    Deck merged = new Deck("Jumpstart - " + pack1Name + " + " + pack2Name);
    merged.getMain().addAll(pack1Deck.getMain());
    merged.getMain().addAll(pack2Deck.getMain());
    playerDeck = merged;

    // AI: pick 2 random packs from built-in pool
    aiDeck = mergeRandomJumpstartPacks();
}
```

### Pattern 3: Frontend Jumpstart-Conditional Lobby
**What:** GameLobby shows dual PackPicker when format is "Jumpstart"
**When to use:** Format-conditional rendering in the lobby, following existing Commander pattern

```typescript
// GameLobby.tsx -- Jumpstart branch
const isJumpstart = selectedFormat === 'Jumpstart'

{isJumpstart ? (
  <div className="flex gap-4">
    <PackPicker
      label="Pack 1"
      userPacks={filteredDecks}
      builtInPacks={jumpstartPacks}
      selectedPack={pack1}
      onSelect={setPack1}
    />
    <PackPicker
      label="Pack 2"
      userPacks={filteredDecks}
      builtInPacks={jumpstartPacks}
      selectedPack={pack2}
      onSelect={setPack2}
    />
  </div>
) : (
  <DeckPicker ... />
)}
```

### Pattern 4: GameStartConfig Extension
**What:** Add optional pack fields for Jumpstart
**When to use:** Jumpstart games send pack names instead of deckName

```typescript
export interface GameStartConfig {
  deckName: string
  aiDeckName: string | null
  format: string
  aiDifficulty: string
  pack1?: string  // Jumpstart only
  pack2?: string  // Jumpstart only
}
```

### Anti-Patterns to Avoid
- **Creating a new GameType.Jumpstart:** The engine has no Jumpstart GameType. Use `GameType.Constructed` with the merged 40-card deck. Adding a GameType requires engine changes across many files.
- **Opening UnOpenedProduct on every API call:** Pack generation is randomized. For the pack browser, we need consistent results. Cache the pack list at server startup or use the template metadata (name, card count from template slots) instead of opening products.
- **Merging packs on the frontend:** The merge must happen on the backend where the engine resolves card names to PaperCard objects. The frontend just sends pack names.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pack definitions | Custom pack JSON files | `StaticData.getSpecialBoosters()` | Forge already has 242 Jumpstart packs defined in boosters-special.txt |
| Card resolution | Manual card name lookup | `FModel.getMagicDb().getCommonCards().getCard()` | Existing engine pattern used throughout DeckHandler |
| Deck serialization | Custom file format | `DeckSerializer.writeDeck()` / `.fromFile()` | Forge .dck format already supports Jumpstart packs as regular decks |
| Format validation | Custom 20-card check | `FormatValidationHandler` | Already handles "Jumpstart" format with 20-card validation (Phase 7) |
| Color identity computation | Manual color parsing | `PaperCard.getRules().getColorIdentity()` | Engine already computes color identity per card |

**Key insight:** The Forge engine already has all Jumpstart data and infrastructure. The work is purely building the UI and the API bridge, not reimplementing format logic.

## Common Pitfalls

### Pitfall 1: UnOpenedProduct Randomization
**What goes wrong:** `UnOpenedProduct.get()` generates a random selection from the template's print sheets. Calling it multiple times gives different cards for the same template.
**Why it happens:** Most templates use `wholeSheet` which returns ALL cards from the print sheet (deterministic), but some might use slot-based randomization.
**How to avoid:** For JMP/J22 entries, the format is `1 wholeSheet("JMP Above the Clouds 1")` -- this means "take 1 copy of the entire sheet." Since `wholeSheet` returns the full print sheet contents, it IS deterministic. Verify this assumption during implementation by checking that the same template returns the same cards across calls.
**Warning signs:** Pack card counts varying between API calls for the same pack name.

### Pitfall 2: Pack Name vs Theme Name
**What goes wrong:** The SealedTemplate name is "JMP Above the Clouds 1" (full identifier including set code and variant number). The user-facing theme should be "Above the Clouds" but the backend merge needs the full template name.
**Why it happens:** Multiple variants exist for the same theme (e.g., "JMP Above the Clouds 1" through "JMP Above the Clouds 4") with slightly different card lists.
**How to avoid:** API returns both `id` (full template name) and `theme` (display name). Frontend displays themes; backend receives IDs. Each variant is a separate selectable pack.

### Pitfall 3: User Packs vs Built-in Packs in Merge
**What goes wrong:** The merge logic in handleStartGame needs to handle two sources: user-created .dck files (loaded via `loadDeckByName`) and Forge built-in packs (loaded via `StaticData.getSpecialBoosters()`).
**Why it happens:** User packs are on disk; built-in packs are in-memory template definitions.
**How to avoid:** Use a naming convention or prefix to distinguish. Built-in packs use their full template name (e.g., "JMP Angels 1"). User packs use their deck name. The merge handler tries user deck first, falls back to built-in pack lookup.

### Pitfall 4: DeckList Missing Jumpstart Format
**What goes wrong:** Users can't create Jumpstart packs because "Jumpstart" isn't in DeckList's FORMAT_OPTIONS.
**Why it happens:** FORMAT_OPTIONS is hardcoded and doesn't include Jumpstart.
**How to avoid:** Add `{ value: 'Jumpstart', label: 'Jumpstart' }` to both `DeckList.tsx` FORMAT_OPTIONS and ensure the "Create Deck" dialog passes format="Jumpstart" to the backend, which stores it via `deck.setComment("Jumpstart")`.

### Pitfall 5: Sideboard Tab in Jumpstart Editor
**What goes wrong:** Jumpstart packs have no sideboard, but the DeckEditor shows sideboard tabs.
**Why it happens:** DeckPanel always renders the sideboard tab regardless of format.
**How to avoid:** Pass `isJumpstartFormat` prop (or check `format === 'Jumpstart'`) and hide the sideboard tab, similar to how `isCommanderFormat` controls commander-specific UI.

### Pitfall 6: AI Deck Selection for Jumpstart
**What goes wrong:** `pickRandomDeck(format)` looks for .dck files with comment "Jumpstart" -- there may be none initially.
**Why it happens:** AI deck selection currently only looks at saved .dck files, not built-in Jumpstart packs.
**How to avoid:** For Jumpstart format, bypass pickRandomDeck entirely. Instead, pick 2 random built-in packs from getSpecialBoosters() filtered to JMP/J22 and merge them.

## Code Examples

### Backend: JumpstartPackDto
```java
// Source: Project pattern from DeckSummaryDto
public class JumpstartPackDto {
    private final String id;       // "JMP Angels 1" (full template name)
    private final String theme;    // "Angels" (display name)
    private final String setCode;  // "JMP" or "J22"
    private final int cardCount;   // typically 20
    private final List<String> colors; // ["W"] etc.

    // Constructor, getters...
}
```

### Backend: Loading a Built-in Pack as Deck
```java
// Source: AdventureEventController.getJumpstartBoosters() pattern
private static Deck loadBuiltInPack(String templateName) {
    for (SealedTemplate template : StaticData.instance().getSpecialBoosters()) {
        if (template.getName().equals(templateName)) {
            UnOpenedProduct product = new UnOpenedProduct(template);
            Deck deck = new Deck(templateName);
            deck.getMain().add(product.get());
            return deck;
        }
    }
    return null;
}
```

### Frontend: JumpstartPack Type
```typescript
// Source: Project pattern from types/deck.ts
export interface JumpstartPack {
  id: string       // Full template name for backend
  theme: string    // Display name
  setCode: string  // "JMP" or "J22"
  cardCount: number
  colors: string[]
}
```

### Frontend: PackPicker Component
```typescript
// Source: Adapted from DeckPicker.tsx pattern
interface PackPickerProps {
  label: string
  userPacks: DeckSummary[]
  builtInPacks: JumpstartPack[]
  selectedPack: string | null
  onSelect: (packId: string) => void
  isLoading: boolean
}
```

### Frontend: useJumpstartPacks Hook
```typescript
// Source: Project pattern from hooks/useDecks.ts
export function useJumpstartPacks() {
  return useQuery({
    queryKey: ['jumpstart-packs'],
    queryFn: () => fetchApi<JumpstartPack[]>('/api/jumpstart/packs'),
    staleTime: Infinity, // Pack definitions never change during session
  })
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate GameType per format | Application-level format handling | This project (v2.0) | Jumpstart works without engine changes |
| Adventure mode pack browsing | Direct StaticData API | This project | Simpler, no adventure dependency |

**Deprecated/outdated:**
- `AdventureEventController.getJumpstartBoosters()` filters by CardBlock landSet code -- this is specific to adventure mode. Our implementation filters by set code prefix ("JMP"/"J22") directly from template names, which is simpler and doesn't require CardBlock setup.

## Open Questions

1. **wholeSheet determinism**
   - What we know: JMP/J22 entries use `1 wholeSheet("JMP Theme N")` format, which should return the entire print sheet
   - What's unclear: Whether `UnOpenedProduct.get()` with wholeSheet is truly deterministic or has any randomization
   - Recommendation: Test during implementation by calling twice and comparing. If non-deterministic, cache results at startup.

2. **Pack variant display**
   - What we know: Some themes have 2-4 variants (e.g., "Above the Clouds 1-4") with different card lists
   - What's unclear: Whether to show each variant as a separate pack or group by theme
   - Recommendation: Show each variant as a separate pack (matching paper Jumpstart where you'd get a random variant). Display as "Above the Clouds (1)" etc.

3. **Browse Packs in DeckList vs DeckEditor**
   - What we know: CONTEXT.md says "Browse Packs button in the deck list when Jumpstart format is selected"
   - What's unclear: Whether this means a button that opens a dialog/panel in DeckList, or navigates to a separate view
   - Recommendation: Add a "Browse Packs" button in DeckList that opens a dialog showing Forge's built-in packs, with "Use Pack" (saves as .dck) and "Copy to Edit" (creates new deck pre-filled with pack cards) actions.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (no frontend test infrastructure exists) |
| Config file | none -- no test framework configured |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| JUMP-01 | Create 20-card pack in editor | manual | Manual: create Jumpstart deck, add cards, check validation | N/A |
| JUMP-02 | Browse built-in packs | manual | `curl http://localhost:8080/api/jumpstart/packs` | N/A |
| JUMP-03 | Select 2 packs, start game | manual | Manual: select format, pick 2 packs, click Start | N/A |
| JUMP-04 | AI selects 2 random packs | manual | Manual: observe game log for AI deck name | N/A |
| JUMP-05 | Validation: exactly 2 packs | manual | Manual: try Start with 0 or 1 pack selected | N/A |

### Sampling Rate
- **Per task commit:** Manual verification via browser
- **Per wave merge:** Full manual walkthrough of all 5 requirements
- **Phase gate:** All 5 JUMP requirements verified manually

### Wave 0 Gaps
No test infrastructure to set up -- this project uses manual verification via browser. The backend endpoint can be smoke-tested via curl.

## Sources

### Primary (HIGH confidence)
- `forge-core/src/main/java/forge/StaticData.java` -- getSpecialBoosters() implementation, reads boosters-special.txt
- `forge-core/src/main/java/forge/item/SealedTemplate.java` -- Pack template structure: name, slots, edition
- `forge-gui/res/blockdata/boosters-special.txt` -- 578 lines, 242 JMP+J22 entries, wholeSheet format
- `forge-gui-mobile/src/forge/adventure/util/AdventureEventController.java` -- getJumpstartBoosters() reference implementation
- All frontend source files read directly from codebase

### Secondary (MEDIUM confidence)
- `forge-core/src/main/java/forge/item/generation/BoosterGenerator.java` -- wholeSheet handling (returns full print sheet contents)

### Tertiary (LOW confidence)
- wholeSheet determinism assumption -- based on code reading, not runtime verification

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in use, no new dependencies
- Architecture: HIGH -- patterns directly follow existing codebase conventions (DeckHandler, DeckPicker, GameLobby)
- Pitfalls: HIGH -- identified through direct code reading of engine Jumpstart implementation
- Pack data: HIGH -- verified 242 JMP/J22 entries in boosters-special.txt with exact format

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- Forge engine pack data doesn't change frequently)
