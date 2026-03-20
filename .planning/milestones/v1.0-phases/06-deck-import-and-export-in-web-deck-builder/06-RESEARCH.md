# Phase 6: Deck Import and Export in Web Deck Builder - Research

**Researched:** 2026-03-20
**Domain:** Deck list parsing (Java backend), modal UI (React/shadcn), clipboard API
**Confidence:** HIGH

## Summary

This phase adds deck import/export I/O to the existing web deck builder. The backend work centers on exposing Forge's existing `DeckRecognizer` parser via a new REST endpoint that accepts raw text and returns typed, color-coded parse results as JSON. Export is simpler -- the backend already has all card data needed; the endpoint formats deck contents in the requested format (generic text, MTGO, Arena, Forge .dck). The frontend work is two modal dialogs (import and export) built with the existing shadcn `Dialog` component, plus a toast notification system for clipboard confirmation.

The key architectural insight is that **all parsing and formatting happens server-side**. The frontend sends raw text to the parse endpoint and renders the structured response. This avoids duplicating any card database logic in the browser and leverages DeckRecognizer's battle-tested multi-format parsing. Export similarly calls the backend with a format parameter and receives formatted text to display/copy.

**Primary recommendation:** Build a POST `/api/decks/parse` endpoint wrapping DeckRecognizer, a GET `/api/decks/{name}/export?format=X` endpoint for export formatting, and two React modal components (ImportDeckDialog, ExportDeckDialog) using existing shadcn Dialog + Select primitives.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Support both paste textarea AND file upload (.dck, .dec, .txt files)
- Modal dialog with textarea on left, live preview on right
- File upload area above or below textarea (drag-and-drop or browse button)
- DeckRecognizer handles all format detection automatically -- no format selector needed for import
- Ask "Replace current deck or add to it?" each import -- user chooses per-import
- Live preview shows parsed results before committing -- user reviews then clicks Import
- Preview updates as user types/pastes (debounced)
- Partial import allowed -- recognized cards imported, unknown cards skipped with summary
- Each line color-coded by DeckRecognizer token type (green/yellow/red/gray)
- Summary counts at bottom: "N recognized, N warnings, N not found"
- Import button always enabled -- shows skip count if unknowns exist
- Four export formats: Generic text, MTGO, Arena, Forge .dck
- Copy to clipboard -- primary action button, no file download
- Export modal shows text preview (read-only textarea) with format dropdown
- Confirmation toast on successful copy
- Import and Export icon buttons in DeckPanel header bar
- Both open as centered modal dialogs (shadcn Dialog component)
- Available for both new and existing decks

### Claude's Discretion
- Exact modal sizing and responsive behavior
- Debounce timing for live preview
- Icon choices for Import/Export buttons
- Toast styling and duration
- Whether to auto-detect deck name from import metadata

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DECK-14 | User can import deck lists via text paste ("4 Lightning Bolt" format) | DeckRecognizer.parseCardList() accepts string array, returns typed Token list with LEGAL_CARD, UNKNOWN_CARD, etc. New REST endpoint wraps this. Frontend ImportDeckDialog sends text, renders token-colored preview, commits recognized cards via existing deck update API. |

</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| DeckRecognizer (Java) | Forge built-in | Multi-format deck text parsing | Already handles MTGO, Arena, TappedOut, Deckstats, XMage, generic formats with 7+ regex patterns |
| DeckSerializer (Java) | Forge built-in | Forge .dck format serialization | Existing serialization with metadata sections |
| CardPool.toCardList() | Forge built-in | Card list text generation | Produces "count CardName\|SetCode" lines |
| shadcn Dialog | base-ui/react | Modal dialogs | Already used in project, properly configured |
| shadcn Select | base-ui/react | Export format dropdown | Already exists as ui/select.tsx |
| sonner | (to install) | Toast notifications | shadcn-compatible toast library for clipboard confirmation |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| navigator.clipboard API | Browser built-in | Copy to clipboard | Export copy-to-clipboard action |
| FileReader API | Browser built-in | Read uploaded deck files | File upload import path |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| sonner | Custom toast div | sonner is the shadcn-recommended toast; custom div lacks animation/stacking |
| Server-side export formatting | Client-side formatting | Client lacks setCode/collectorNumber data for some cards; server has authoritative card data |

**Installation:**
```bash
cd forge-gui-web/frontend && npm install sonner
```

## Architecture Patterns

### Recommended Project Structure
```
forge-gui-web/
  src/main/java/forge/web/
    api/
      DeckImportExportHandler.java    # New: parse + export endpoints
    dto/
      ParseResultDto.java             # New: parse response DTO
      ParseTokenDto.java              # New: individual token DTO
  frontend/src/
    api/
      decks.ts                        # Add: parseDeckText(), exportDeck()
    components/
      deck-editor/
        ImportDeckDialog.tsx           # New: import modal
        ExportDeckDialog.tsx           # New: export modal
        DeckPanel.tsx                  # Modified: add Import/Export buttons
        DeckEditor.tsx                 # Modified: wire import callback
      ui/
        sonner.tsx                     # New: toast provider component
    hooks/
      useDeckEditor.ts                # Modified: add importCards method
    types/
      deck.ts                         # Modified: add ParseResult types
```

### Pattern 1: Backend Parse Endpoint
**What:** POST `/api/decks/parse` accepts `{ text: string }` body, returns typed token array
**When to use:** Every import preview request (debounced from frontend)
**Example:**
```java
// Source: DeckRecognizer.java analysis
public static void parse(final Context ctx) {
    Map<String, String> body = ctx.bodyAsClass(Map.class);
    String text = body.get("text");
    String[] lines = text.split("\\r?\\n");

    DeckRecognizer recognizer = new DeckRecognizer();
    List<DeckRecognizer.Token> tokens = recognizer.parseCardList(lines);

    List<ParseTokenDto> result = new ArrayList<>();
    for (DeckRecognizer.Token token : tokens) {
        result.add(ParseTokenDto.from(token));
    }
    ctx.json(result);
}
```

### Pattern 2: Export Endpoint with Format Parameter
**What:** GET `/api/decks/{name}/export?format=generic|mtgo|arena|forge` returns formatted text
**When to use:** When user opens export modal or changes format dropdown
**Example:**
```java
// Source: CardPool.toCardList(), DeckSerializer.serializeDeck()
public static void export(final Context ctx) {
    String name = ctx.pathParam("name");
    String format = ctx.queryParam("format");
    // Load deck, format based on format param, return as { text: "..." }
}
```

### Pattern 3: Debounced Frontend Parse Calls
**What:** Debounce textarea input, send to parse endpoint, render colored results
**When to use:** Import modal live preview
**Example:**
```typescript
// Debounce parse requests as user types
const [parseResult, setParseResult] = useState<ParseToken[]>([])
const debouncedParse = useMemo(
  () => debounce(async (text: string) => {
    if (!text.trim()) { setParseResult([]); return }
    const result = await parseDeckText(text)
    setParseResult(result)
  }, 300),
  []
)
```

### Pattern 4: Import Commit via Existing Update API
**What:** When user clicks Import, convert parsed tokens to card map and call existing update endpoint
**When to use:** Final import action
**Example:**
```typescript
// Convert parsed tokens to UpdateDeckPayload format
const mainCards: Record<string, number> = {}
const sideboardCards: Record<string, number> = {}
for (const token of tokens.filter(t => t.type === 'LEGAL_CARD' || t.type === 'LIMITED_CARD')) {
    const target = token.section === 'Sideboard' ? sideboardCards : mainCards
    target[token.cardName] = (target[token.cardName] || 0) + token.quantity
}
```

### Anti-Patterns to Avoid
- **Client-side parsing:** Never reimplement DeckRecognizer logic in TypeScript -- always use the server endpoint. DeckRecognizer has 1000+ lines of battle-tested regex patterns.
- **Blocking the main thread with large pastes:** Always debounce and use async fetch for parse calls.
- **Synchronous clipboard writes:** Use async `navigator.clipboard.writeText()` with try/catch for older browser fallback.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Deck text parsing | Custom TypeScript parser | DeckRecognizer via REST endpoint | 1000+ lines of regex patterns, handles 6+ formats, edge cases for split cards, DFCs, set codes |
| Deck format detection | Format selector/detection | DeckRecognizer auto-detection | Parser tries all patterns and returns best match automatically |
| Toast notifications | Custom toast div | sonner library | Handles animation, stacking, auto-dismiss, accessible by default |
| Modal dialogs | Custom overlay | shadcn Dialog | Already exists in project, handles backdrop, focus trap, animation |
| Clipboard interaction | Custom fallback system | navigator.clipboard.writeText() | Modern API, supported in all target browsers (desktop Chrome/Firefox/Edge) |

**Key insight:** The entire parsing complexity is already solved by Forge's DeckRecognizer. The only engineering work is wrapping it in a REST endpoint and building clean UI around it.

## Common Pitfalls

### Pitfall 1: DeckRecognizer Requires StaticData Initialization
**What goes wrong:** DeckRecognizer calls `StaticData.instance()` to resolve card names against the database. If called before FModel initialization, it throws NullPointerException.
**Why it happens:** The recognizer validates card names against the loaded card database.
**How to avoid:** The web server already initializes FModel in `main()` before any endpoint is available. The parse endpoint runs after server startup, so this is safe. But verify in the handler that StaticData is available.
**Warning signs:** NullPointerException from StaticData.instance() in parse endpoint.

### Pitfall 2: Large Paste Performance
**What goes wrong:** User pastes a 500+ line deck list, debounced parse fires, server processes all lines synchronously.
**Why it happens:** DeckRecognizer.parseCardList() iterates every line and does regex matching + DB lookups.
**How to avoid:** Use 300ms debounce on the frontend. DeckRecognizer is fast (regex + HashMap lookups), but debouncing prevents flooding during rapid typing. Consider a reasonable line limit (e.g., 1000 lines).
**Warning signs:** Slow preview updates, multiple concurrent parse requests.

### Pitfall 3: Import Mode Confusion (Replace vs Add)
**What goes wrong:** User imports without understanding whether it replaces or adds to their deck.
**Why it happens:** Missing or unclear UI indication of the import mode.
**How to avoid:** Show explicit "Replace" and "Add to deck" buttons (not a toggle that defaults silently). The user decision from CONTEXT.md specifies asking per-import.
**Warning signs:** Users accidentally overwriting their deck contents.

### Pitfall 4: Clipboard API Requires HTTPS or localhost
**What goes wrong:** `navigator.clipboard.writeText()` fails silently or throws in non-secure contexts.
**Why it happens:** Clipboard API requires secure context (HTTPS or localhost).
**How to avoid:** The web server runs on localhost:8080, which is a secure context. Add try/catch and fallback to `document.execCommand('copy')` with a hidden textarea for edge cases.
**Warning signs:** Copy button appears to do nothing, no toast appears.

### Pitfall 5: Dialog Component Max-Width Too Small
**What goes wrong:** The import modal with textarea + preview side-by-side is cramped at the default `sm:max-w-sm` Dialog width.
**Why it happens:** Default shadcn DialogContent has `sm:max-w-sm` (384px) -- way too narrow for a two-panel layout.
**How to avoid:** Override DialogContent className with `sm:max-w-3xl` or `sm:max-w-4xl` for the import modal. The dialog component already accepts className for overriding.
**Warning signs:** Horizontal scrolling in textarea, truncated preview content.

### Pitfall 6: Token Section May Be Null
**What goes wrong:** DeckRecognizer tokens for cards may have `tokenSection` as null (defaults to Main).
**Why it happens:** When no section header (e.g., "Sideboard") appears before a card line, the token section is null.
**How to avoid:** In the DTO mapping, treat null tokenSection as "Main" explicitly.
**Warning signs:** Cards imported without section assignment, all going to main even when sideboard section was expected.

## Code Examples

### Backend: Parse Token DTO
```java
// New DTO for parse results
public class ParseTokenDto {
    public String type;      // "LEGAL_CARD", "UNKNOWN_CARD", etc.
    public int quantity;
    public String text;       // Display text (card name or original line)
    public String cardName;   // Resolved card name (null for non-card tokens)
    public String setCode;    // Set code (null for non-card tokens)
    public String collectorNumber;
    public String section;    // "Main", "Sideboard", "Commander", null

    public static ParseTokenDto from(DeckRecognizer.Token token) {
        ParseTokenDto dto = new ParseTokenDto();
        dto.type = token.getType().name();
        dto.quantity = token.getQuantity();
        dto.text = token.getText();
        if (token.isCardToken() && token.getCard() != null) {
            PaperCard pc = token.getCard();
            dto.cardName = pc.getName();
            dto.setCode = pc.getEdition();
            dto.collectorNumber = pc.getCollectorNumber();
        }
        dto.section = token.getTokenSection() != null
            ? token.getTokenSection().name() : null;
        return dto;
    }
}
```

### Backend: Export Formatting
```java
// Generic text: "4 Lightning Bolt"
private static String formatGeneric(Deck deck) {
    StringBuilder sb = new StringBuilder();
    appendSection(sb, deck.getOrCreate(DeckSection.Main));
    CardPool side = deck.get(DeckSection.Sideboard);
    if (side != null && !side.isEmpty()) {
        sb.append("\nSideboard\n");
        appendSection(sb, side);
    }
    return sb.toString();
}

private static void appendSection(StringBuilder sb, CardPool pool) {
    for (Map.Entry<PaperCard, Integer> e : pool) {
        sb.append(e.getValue()).append(" ").append(e.getKey().getName()).append("\n");
    }
}

// MTGO: "4 Lightning Bolt (2XM)"
// Arena: "4 Lightning Bolt (2XM) 123"
// Forge .dck: full metadata + sections via DeckSerializer
```

### Frontend: Import Dialog Structure
```typescript
// ImportDeckDialog.tsx
<Dialog open={open} onOpenChange={onOpenChange}>
  <DialogContent className="sm:max-w-3xl max-h-[80vh]">
    <DialogHeader>
      <DialogTitle>Import Deck</DialogTitle>
    </DialogHeader>
    <div className="grid grid-cols-2 gap-4 min-h-[400px]">
      {/* Left: Input */}
      <div className="flex flex-col gap-2">
        {/* File upload area */}
        <div className="border-2 border-dashed rounded p-3 text-center cursor-pointer">
          Drop .dck/.dec/.txt file or click to browse
        </div>
        {/* Textarea */}
        <textarea
          className="flex-1 resize-none font-mono text-sm"
          placeholder="Paste deck list here..."
          onChange={(e) => debouncedParse(e.target.value)}
        />
      </div>
      {/* Right: Preview */}
      <div className="flex flex-col overflow-y-auto">
        {/* Color-coded token lines */}
        {/* Summary counts */}
      </div>
    </div>
    <DialogFooter>
      <Button onClick={() => handleImport('replace')}>Replace Deck</Button>
      <Button onClick={() => handleImport('add')}>Add to Deck</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

### Frontend: Export Dialog Structure
```typescript
// ExportDeckDialog.tsx
<Dialog open={open} onOpenChange={onOpenChange}>
  <DialogContent className="sm:max-w-lg">
    <DialogHeader>
      <DialogTitle>Export Deck</DialogTitle>
    </DialogHeader>
    <Select value={format} onValueChange={setFormat}>
      {/* Generic, MTGO, Arena, Forge .dck */}
    </Select>
    <textarea readOnly className="font-mono text-sm h-64" value={exportText} />
    <DialogFooter>
      <Button onClick={handleCopy}>Copy to Clipboard</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

### Frontend: Clipboard with Toast
```typescript
import { toast } from 'sonner'

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(exportText)
    toast.success('Copied to clipboard')
  } catch {
    // Fallback for edge cases
    const textarea = document.createElement('textarea')
    textarea.value = exportText
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    toast.success('Copied to clipboard')
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Forge desktop DeckRecognizer with Swing UI callbacks | Expose via REST, render in React | This phase | Reuses proven parser, modern UI |
| execCommand('copy') for clipboard | navigator.clipboard.writeText() | 2020+ | Async, cleaner API, secure context only |
| Custom modal overlays | base-ui Dialog primitive | Already in project | Accessible, animated, focus-trapped |

**Deprecated/outdated:**
- `document.execCommand('copy')`: Deprecated but still works as fallback. Primary path should use Clipboard API.

## Open Questions

1. **Sonner vs custom toast**
   - What we know: No toast system exists in the project yet. sonner is the shadcn-recommended toast library.
   - What's unclear: Whether the user prefers sonner or a simpler custom implementation.
   - Recommendation: Use sonner -- it is lightweight, shadcn-compatible, and avoids building toast infrastructure from scratch. Single dependency add.

2. **Auto-detect deck name from import**
   - What we know: DeckRecognizer parses DECK_NAME tokens from lines like "Name: My Deck" or "// Name: My Deck".
   - What's unclear: Whether to auto-populate the deck name from imported metadata (Claude's Discretion item).
   - Recommendation: Yes, auto-detect. If a DECK_NAME token is found, suggest it as the deck name (but don't force -- let existing name stand if deck already has one).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Java backend), no frontend test framework |
| Config file | Maven pom.xml (JUnit), none for frontend |
| Quick run command | `cd forge-gui-web && mvn test -pl . -Dtest=DeckImportExportHandlerTest` |
| Full suite command | `cd forge-gui-web && mvn test -pl .` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DECK-14 | Parse endpoint returns typed tokens for valid deck text | unit | `mvn test -pl forge-gui-web -Dtest=DeckImportExportHandlerTest#testParseValidDeckText -f pom.xml` | Wave 0 |
| DECK-14 | Parse endpoint handles unknown cards gracefully | unit | `mvn test -pl forge-gui-web -Dtest=DeckImportExportHandlerTest#testParseUnknownCards -f pom.xml` | Wave 0 |
| DECK-14 | Export endpoint returns correctly formatted text for each format | unit | `mvn test -pl forge-gui-web -Dtest=DeckImportExportHandlerTest#testExportFormats -f pom.xml` | Wave 0 |
| DECK-14 | Import modal renders and preview updates | manual-only | Manual: paste text, verify color-coded preview appears | N/A |
| DECK-14 | Export copy-to-clipboard works | manual-only | Manual: open export, click copy, paste elsewhere | N/A |

### Sampling Rate
- **Per task commit:** `cd forge-gui-web && mvn test -pl . -Dtest=DeckImportExportHandlerTest`
- **Per wave merge:** `cd forge-gui-web && mvn test -pl .`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `forge-gui-web/src/test/java/forge/web/DeckImportExportHandlerTest.java` -- covers DECK-14 parse/export endpoints
- [ ] No frontend test infrastructure exists (no vitest/jest configured) -- UI testing is manual

## Sources

### Primary (HIGH confidence)
- `forge-core/src/main/java/forge/deck/DeckRecognizer.java` -- Full source analysis of parser, token types, regex patterns
- `forge-core/src/main/java/forge/deck/io/DeckSerializer.java` -- Full source analysis of .dck serialization
- `forge-core/src/main/java/forge/deck/CardPool.java` -- toCardList() method for text export
- `forge-gui-web/src/main/java/forge/web/WebServer.java` -- REST endpoint registration patterns with Javalin 7
- `forge-gui-web/src/main/java/forge/web/api/DeckHandler.java` -- Existing deck CRUD handler patterns
- `forge-gui-web/frontend/src/components/ui/dialog.tsx` -- DialogContent accepts className override, base-ui primitives
- `forge-gui-web/frontend/src/components/deck-editor/DeckPanel.tsx` -- Header bar structure with existing icon buttons
- `forge-gui-web/frontend/src/hooks/useDeckEditor.ts` -- Deck state management and save patterns
- `forge-gui-web/frontend/src/types/deck.ts` -- DeckCardEntry, DeckDetail, UpdateDeckPayload types

### Secondary (MEDIUM confidence)
- navigator.clipboard API -- Well-established browser API, secure context requirement verified

### Tertiary (LOW confidence)
- sonner library recommendation -- Based on shadcn ecosystem knowledge; version should be verified at install time

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- All core libraries are already in the project or part of Forge
- Architecture: HIGH -- Follows established REST handler + React component patterns from Phases 2-5
- Pitfalls: HIGH -- Based on direct source code analysis of DeckRecognizer, Dialog component, and clipboard API
- Export formatting: HIGH -- CardPool.toCardList() and DeckSerializer.serializeDeck() analyzed directly

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable -- no external dependency changes expected)
