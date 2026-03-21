# Phase 10: Advanced Deck Stats - Research

**Researched:** 2026-03-20
**Domain:** Oracle text analysis, deck composition metrics, SVG data visualization
**Confidence:** HIGH

## Summary

Phase 10 adds oracle-text-based deck analysis to the existing StatsPanel: removal counts, ramp density, card draw sources, interaction range coverage, consistency metrics, and win condition analysis. The core work is (1) a backend DTO change to expose `oracleText` and `power`/`toughness` on deck card entries, (2) a new `deck-analysis.ts` module with regex-based classification functions, and (3) new chart/grid components rendered below existing stats.

The existing codebase has a clean separation: pure computation in `lib/deck-stats.ts`, rendering in `components/charts/`, and `StatsPanel.tsx` as the container using `useMemo`. New code follows this same pattern. The biggest technical risk is adventure cards: Forge's `CardRules.getOracleText()` only returns the primary face for adventure cards (missing the adventure spell text), so the backend DTO must combine both faces' oracle text.

**Primary recommendation:** Add `oracleText`, `power`, and `toughness` to `DeckDetailDto.DeckCardEntry` in the backend (trivial Java change), then build all analysis as pure frontend functions against the enriched type.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions

**Card Classification Rules:**
- Removal (broad definition, subcategorized): hard removal ("destroy target", "exile target", "destroy all" sweepers), soft removal ("deals X damage to target creature/planeswalker", "-X/-X", "fight", bounce, sacrifice-based), subcategorized into hard removal, soft removal, sweepers. Include planeswalker -X abilities that remove permanents.
- Ramp (inclusive): mana dorks ("add {" or "tap: add"), land search ("search your library for a ... land"), mana rocks (artifacts with "add {"), ritual effects ("add {X}{X}" type). Subcategorized: creatures, artifacts, spells.
- Card Draw (inclusive): dedicated draw ("draw X cards" where X > 1), cantrips ("draw a card"), card filtering ("scry", "surveil", "look at the top X"). Subcategorized: draw, cantrips, filtering.
- Win Conditions (heuristic-based): alternate win cons ("you win the game"), big threats (CMC 5+ creatures with power 4+), planeswalkers (all), combo indicators. Not perfect -- gives useful signal.
- Interaction Range: creature answers, enchantment answers, artifact answers, planeswalker answers, graveyard answers, land answers.
- Consistency Metrics: 4-of ratio (cards with 4 copies / total unique), tutor/search count, threat redundancy.

**Stats Display Layout:**
- New sections below existing StatsPanel content (mana curve, color distribution, type breakdown)
- Scrollable continuous view -- no separate tab
- Sections: "Deck Composition" (removal/ramp/draw with subcategory counts), "Interaction Range" (coverage grid), "Consistency" (4-of ratio, tutors, redundancy), "Win Conditions" (threats list with count)

**Interaction Range Visualization:**
- Coverage grid: threat types as rows (Creatures, Enchantments, Artifacts, Planeswalkers, Graveyards, Lands)
- Each row shows count of answers + green checkmark if any, red X if zero
- Simple, scannable at a glance

### Claude's Discretion
- Exact oracle text regex patterns for each classification
- Edge case handling (modal spells, split cards, adventure cards)
- Visual design of composition bars and grid styling
- Whether to show individual card names in each category or just counts

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| STATS-01 | User can see removal count, ramp density, card draw source count | Oracle text regex classification in `deck-analysis.ts`; backend DTO enrichment with oracleText field |
| STATS-02 | User can see interaction range analysis (creature, enchantment, artifact, graveyard answers) | InteractionRange grid component; regex patterns per permanent type |
| STATS-03 | User can see consistency metrics (4-of ratio, tutor count, threat redundancy) | Pure math from card quantities + "search your library" regex |
| STATS-04 | User can see win condition analysis (distinct win cons, redundancy) | Heuristic classification: CMC 5+ power 4+ creatures, planeswalkers, "you win the game" text |

</phase_requirements>

## Critical Finding: Backend DTO Gap

**`DeckDetailDto.DeckCardEntry` is missing `oracleText`, `power`, and `toughness`.**

Current fields (from `DeckDetailDto.java` line 71-83):
```java
public static class DeckCardEntry {
    public String name;
    public int quantity;
    public String setCode;
    public String collectorNumber;
    public String manaCost;
    public String typeLine;
    public int cmc;
    public List<String> colors;
}
```

The frontend `DeckCardEntry` type (in `types/deck.ts`) mirrors this exactly -- no `oracleText` either.

**Required changes:**
1. Add `oracleText`, `power`, `toughness` to `DeckDetailDto.DeckCardEntry` (Java)
2. Populate them in `DeckDetailDto.toEntries()` using `rules.getOracleText()`, `rules.getIntPower()`, `rules.getIntToughness()`
3. Add the same fields to frontend `DeckCardEntry` type (TypeScript)

**Confidence: HIGH** -- Verified by reading both `DeckDetailDto.java` and `types/deck.ts`.

## Adventure Card Oracle Text Problem

**Forge's `CardRules.getOracleText()` only returns the primary face for adventure cards.**

From `CardSplitType.java`:
- `Split` cards: `FaceSelectionMethod.COMBINE` -- both halves concatenated with `\r\n\r\n`
- `Adventure` cards: `FaceSelectionMethod.USE_PRIMARY_FACE` -- ONLY the creature face
- `Transform`/`Modal` DFC: `FaceSelectionMethod.USE_ACTIVE_FACE` -- only front face
- `Flip`: `FaceSelectionMethod.USE_PRIMARY_FACE` -- only front face

**Impact:** Murderous Rider's `getOracleText()` returns "Lifelink\nWhen Murderous Rider dies, put it on the bottom of its owner's library." -- the removal text from Swift End ("Destroy target creature or planeswalker") is **lost**.

**Recommended fix:** In `DeckDetailDto.toEntries()`, always combine both faces:
```java
String oracleText = rules.getOracleText();
ICardFace otherPart = rules.getOtherPart();
if (otherPart != null && otherPart.getOracleText() != null
        && !oracleText.contains(otherPart.getOracleText())) {
    oracleText = oracleText + "\n\n" + otherPart.getOracleText();
}
dce.oracleText = oracleText;
```

This ensures adventure (Swift End), transform back faces (Tibalt, Cosmic Impostor), and modal DFC back faces are all included in the analysis text. Split cards already combine via `COMBINE`, so the `contains` check prevents double-inclusion.

**Confidence: HIGH** -- Verified by reading `CardSplitType.java`, `CardRules.getOracleText()`, and actual card files (bonecrusher_giant_stomp.txt, murderous_rider_swift_end.txt).

## Architecture Patterns

### Recommended Project Structure
```
forge-gui-web/
  src/main/java/forge/web/dto/
    DeckDetailDto.java              # Add oracleText, power, toughness to DeckCardEntry
  frontend/src/
    types/
      deck.ts                       # Add oracleText, power, toughness to DeckCardEntry
    lib/
      deck-stats.ts                 # Existing (unchanged)
      deck-analysis.ts              # NEW: oracle text classification functions
    components/
      charts/
        ManaCurveChart.tsx           # Existing (unchanged)
        ColorDistribution.tsx        # Existing (unchanged)
        TypeBreakdown.tsx            # Existing (reuse pattern for composition bars)
        CompositionBreakdown.tsx     # NEW: removal/ramp/draw horizontal bars
        InteractionGrid.tsx          # NEW: coverage grid with checkmarks
      deck-editor/
        StatsPanel.tsx              # Extend with new sections
```

### Pattern: Pure Computation + Rendering Separation
**What:** All analysis logic lives in `lib/deck-analysis.ts` as pure functions. Components only render.
**When to use:** Always -- established project pattern from `deck-stats.ts`.
**Example (follows existing `computeManaCurve` pattern):**
```typescript
// lib/deck-analysis.ts
import type { DeckCardEntry } from '../types/deck'

export interface DeckComposition {
  removal: { hard: CardMatch[]; soft: CardMatch[]; sweepers: CardMatch[] }
  ramp: { creatures: CardMatch[]; artifacts: CardMatch[]; spells: CardMatch[] }
  draw: { draw: CardMatch[]; cantrips: CardMatch[]; filtering: CardMatch[] }
}

export interface CardMatch {
  name: string
  quantity: number
}

export function analyzeDeckComposition(cards: DeckCardEntry[]): DeckComposition { ... }
export function analyzeInteractionRange(cards: DeckCardEntry[]): InteractionRange { ... }
export function analyzeConsistency(cards: DeckCardEntry[]): ConsistencyMetrics { ... }
export function analyzeWinConditions(cards: DeckCardEntry[]): WinConditionAnalysis { ... }
```

### Pattern: useMemo for Expensive Computations
**What:** StatsPanel wraps all analysis calls in `useMemo` keyed on `cards` array.
**Why:** Oracle text regex matching across 60+ cards is non-trivial; avoid recomputing on every render.
**Example (follows existing StatsPanel pattern):**
```typescript
const composition = useMemo(() => analyzeDeckComposition(cards), [cards])
const interactionRange = useMemo(() => analyzeInteractionRange(cards), [cards])
const consistency = useMemo(() => analyzeConsistency(cards), [cards])
const winConditions = useMemo(() => analyzeWinConditions(cards), [cards])
```

### Anti-Patterns to Avoid
- **Regex per card on every render:** Always memoize. Even 60 cards * 20 regexes = 1200 regex tests.
- **Separate API call for oracle text:** The text ships with the deck DTO. No extra fetch needed.
- **Mutating card data:** Analysis functions are pure -- they read `DeckCardEntry[]`, return new objects.

## Oracle Text Regex Patterns

Based on analysis of actual Forge oracle text format (verified by reading card files in `forge-gui/res/cardsfolder/`).

### Forge Oracle Text Format
- Newlines represented as `\n` in the string
- Mana symbols: `{W}`, `{U}`, `{B}`, `{R}`, `{G}`, `{C}`, `{1}`, `{2}`, etc.
- Tap symbol: `{T}`
- Keywords on their own line: "Lifelink", "Haste", "Flying"
- Modal spells: "Choose one --\n* Option A\n* Option B"
- No markdown or HTML -- plain text with `\n` line breaks

### Removal Patterns
```typescript
// Case-insensitive matching against card.oracleText

// Hard removal
const HARD_REMOVAL = [
  /destroy target (?:.*?)(?:creature|permanent|planeswalker)/i,
  /exile target (?:.*?)(?:creature|permanent|planeswalker)/i,
  /destroy all (?:.*?)creature/i,           // sweeper overlap
  /exile all (?:.*?)creature/i,             // sweeper overlap
]

// Sweepers (subset of hard removal, detect separately)
const SWEEPERS = [
  /destroy all (?:.*?)creature/i,
  /exile all (?:.*?)creature/i,
  /deals? \d+ damage to each creature/i,
  /all creatures get -\d+\/-\d+/i,
]

// Soft removal
const SOFT_REMOVAL = [
  /deals? \d+ damage to (?:target|any target)/i,  // Burns like Lightning Bolt
  /gets? -\d+\/-\d+/i,                             // Shrink effects
  /target (?:.*?) fights? /i,                       // Fight
  /fight target/i,
  /return target (?:.*?) to its owner's hand/i,     // Bounce
]
```

### Ramp Patterns
```typescript
// Mana dorks: creatures with tap-for-mana
// Must cross-reference typeLine for creature/artifact distinction
const MANA_PRODUCERS = /\{T\}:? add \{/i
const LAND_SEARCH = /search your library for (?:a|an|up to \w+)(?:\s\w+)* (?:basic )?land/i
const RITUAL = /add \{[WUBRGC]\}\{[WUBRGC]\}/i  // Adds 2+ mana of same type
```

### Card Draw Patterns
```typescript
const DRAW_MULTIPLE = /draw (?:two|three|four|five|six|seven|\d+) cards?/i  // X>1
const CANTRIP = /draw a card/i
const FILTERING = /\b(?:scry|surveil)\b/i
const LOOK_TOP = /look at the top \d+ cards?/i
```

### Interaction Range Patterns
```typescript
const CREATURE_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)creature/i
const ENCHANTMENT_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)(?:enchantment|permanent|nonland permanent)/i
const ARTIFACT_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)(?:artifact|permanent|nonland permanent)/i
const PLANESWALKER_ANSWER = /(?:destroy|exile|damage)(?:.*?)(?:target|each)(?:.*?)planeswalker/i
const GRAVEYARD_ANSWER = /exile (?:.*?)(?:from (?:a |target )?graveyard|all cards from (?:.*?)graveyard)/i
const LAND_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)land/i
```

### Win Condition Patterns
```typescript
const ALT_WIN = /you win the game/i
// Big threats: use typeLine + cmc + power (not oracle text)
function isBigThreat(card: DeckCardEntry): boolean {
  return card.typeLine.toLowerCase().includes('creature')
    && card.cmc >= 5
    && card.power >= 4
}
// Planeswalkers: use typeLine
function isPlaneswalker(card: DeckCardEntry): boolean {
  return card.typeLine.toLowerCase().includes('planeswalker')
}
```

### Consistency Metrics (No Regex Needed)
```typescript
function fourOfRatio(cards: DeckCardEntry[]): number {
  const unique = cards.length
  const fourOfs = cards.filter(c => c.quantity >= 4).length
  return unique > 0 ? fourOfs / unique : 0
}

const TUTOR = /search your library/i
```

**Confidence: HIGH** -- Patterns verified against actual Forge oracle text from card files.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| SVG chart rendering | Custom canvas/WebGL charts | SVG elements + Tailwind (existing pattern) | Project already uses hand-crafted SVG for ManaCurveChart, TypeBreakdown, ColorDistribution. Keep consistent. |
| Card classification ML | NLP/ML classifier for card roles | Regex heuristics against oracle text | Deterministic, fast, debuggable. ML adds complexity for marginal accuracy improvement. |
| Charting library | Recharts, Chart.js, D3 | Raw SVG (existing pattern) | No external charting deps in this project. All charts are hand-crafted SVG. |

**Key insight:** This project uses zero external charting libraries. All visualization is hand-crafted SVG with Tailwind CSS classes. New charts must follow the same approach.

## Common Pitfalls

### Pitfall 1: Missing oracleText on DeckCardEntry
**What goes wrong:** Code tries to access `card.oracleText` but it's `undefined` because the backend never sent it.
**Why it happens:** `DeckDetailDto.DeckCardEntry` currently has no `oracleText` field.
**How to avoid:** Backend DTO enrichment MUST ship first. Add `oracleText`, `power`, `toughness` to both Java and TypeScript types.
**Warning signs:** `TypeError: Cannot read properties of undefined` on `card.oracleText.match(...)`.

### Pitfall 2: Adventure Card Blind Spots
**What goes wrong:** Murderous Rider not counted as removal because `getOracleText()` returns only the creature face.
**Why it happens:** Forge's `CardSplitType.Adventure` uses `USE_PRIMARY_FACE`, not `COMBINE`.
**How to avoid:** In `DeckDetailDto.toEntries()`, explicitly combine `mainPart.getOracleText()` and `otherPart.getOracleText()` for all card types.
**Warning signs:** Adventure cards with removal/draw effects missing from analysis.

### Pitfall 3: Regex Over-Matching on Card Names
**What goes wrong:** A card named "Destroy Evil" triggers the "destroy target" regex on its own name embedded in oracle text like "Destroy Evil can't be countered."
**Why it happens:** Oracle text sometimes references the card's own name.
**How to avoid:** Regex patterns should target action phrases ("destroy target", "exile target") not just keywords. The patterns above are already structured this way.
**Warning signs:** Cards incorrectly classified due to name substring matching.

### Pitfall 4: Double-Counting Cards in Multiple Categories
**What goes wrong:** Cryptic Command counted as removal AND card draw AND bounce.
**Why it happens:** Modal spells legitimately belong in multiple categories.
**How to avoid:** This is actually CORRECT behavior. A card that removes AND draws should count in both. The user wants to see "how many of my cards can remove things" -- Cryptic Command is one of them. Just ensure the total in "Deck Composition" is labeled as "cards with X" not implying mutual exclusion.
**Warning signs:** Totals don't add up to deck size (expected -- this is fine).

### Pitfall 5: Empty oracleText for Basic Lands
**What goes wrong:** Analysis crashes on null/empty oracleText.
**Why it happens:** Basic lands may have empty oracle text in Forge.
**How to avoid:** Guard every regex match with `if (!card.oracleText) return`. Lands are generally excluded from analysis anyway (like mana curve).

## Code Examples

### Backend DTO Enrichment
```java
// In DeckDetailDto.toEntries(), add to the loop body:
dce.power = rules.getIntPower();
dce.toughness = rules.getIntToughness();

// Combine all faces for oracle text analysis
String oracleText = rules.getOracleText();
ICardFace otherPart = rules.getOtherPart();
if (otherPart != null && otherPart.getOracleText() != null
        && !oracleText.contains(otherPart.getOracleText())) {
    oracleText = oracleText + "\n\n" + otherPart.getOracleText();
}
dce.oracleText = oracleText;
```

### Frontend Type Update
```typescript
// types/deck.ts - DeckCardEntry
export interface DeckCardEntry {
  name: string
  quantity: number
  setCode: string
  collectorNumber: string
  manaCost: string
  typeLine: string
  cmc: number
  colors: string[]
  oracleText: string   // NEW
  power: number         // NEW
  toughness: number     // NEW
}
```

### Analysis Function Pattern
```typescript
// lib/deck-analysis.ts (follows deck-stats.ts pattern)
export function analyzeDeckComposition(cards: DeckCardEntry[]): DeckComposition {
  const result: DeckComposition = {
    removal: { hard: [], soft: [], sweepers: [] },
    ramp: { creatures: [], artifacts: [], spells: [] },
    draw: { draw: [], cantrips: [], filtering: [] },
  }

  for (const card of cards) {
    if (!card.oracleText) continue
    const text = card.oracleText.toLowerCase()
    const typeLine = card.typeLine.toLowerCase()

    // Removal classification
    if (SWEEPERS.some(r => r.test(text))) {
      result.removal.sweepers.push({ name: card.name, quantity: card.quantity })
    } else if (HARD_REMOVAL.some(r => r.test(text))) {
      result.removal.hard.push({ name: card.name, quantity: card.quantity })
    } else if (SOFT_REMOVAL.some(r => r.test(text))) {
      result.removal.soft.push({ name: card.name, quantity: card.quantity })
    }

    // Ramp classification
    if (MANA_PRODUCERS.test(text) || LAND_SEARCH.test(text) || RITUAL.test(text)) {
      if (typeLine.includes('creature')) {
        result.ramp.creatures.push({ name: card.name, quantity: card.quantity })
      } else if (typeLine.includes('artifact')) {
        result.ramp.artifacts.push({ name: card.name, quantity: card.quantity })
      } else {
        result.ramp.spells.push({ name: card.name, quantity: card.quantity })
      }
    }

    // Draw classification (similar pattern)
    // ...
  }
  return result
}
```

### Interaction Range Grid Component
```typescript
// components/charts/InteractionGrid.tsx (follows TypeBreakdown SVG pattern)
interface InteractionGridProps {
  range: InteractionRange
}

// InteractionRange type:
// { creatures: number, enchantments: number, artifacts: number,
//   planeswalkers: number, graveyards: number, lands: number }

export function InteractionGrid({ range }: InteractionGridProps) {
  const rows = [
    { label: 'Creatures', count: range.creatures },
    { label: 'Enchantments', count: range.enchantments },
    { label: 'Artifacts', count: range.artifacts },
    { label: 'Planeswalkers', count: range.planeswalkers },
    { label: 'Graveyards', count: range.graveyards },
    { label: 'Lands', count: range.lands },
  ]
  // Render as SVG rows: label | green check or red X | count
  // ...
}
```

### StatsPanel Extension
```typescript
// StatsPanel.tsx - add below existing sections
const composition = useMemo(() => analyzeDeckComposition(cards), [cards])
const interactionRange = useMemo(() => analyzeInteractionRange(cards), [cards])
const consistency = useMemo(() => analyzeConsistency(cards), [cards])
const winConditions = useMemo(() => analyzeWinConditions(cards), [cards])

// In JSX, after existing Deck Summary section:
<section>
  <h3 className="text-[14px] font-semibold text-foreground mb-3">Deck Composition</h3>
  <CompositionBreakdown composition={composition} />
</section>
<section>
  <h3 className="text-[14px] font-semibold text-foreground mb-3">Interaction Range</h3>
  <InteractionGrid range={interactionRange} />
</section>
// ... etc
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual card counting | Oracle text regex analysis | This phase | Automated deck composition analysis |
| No interaction coverage visibility | Coverage grid | This phase | Users see blind spots at a glance |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (no frontend test infrastructure exists) |
| Config file | none -- see Wave 0 |
| Quick run command | N/A |
| Full suite command | N/A |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| STATS-01 | Oracle text classifies removal/ramp/draw correctly | unit | N/A (no test infra) | N/A Wave 0 |
| STATS-02 | Interaction range counts per permanent type | unit | N/A | N/A Wave 0 |
| STATS-03 | 4-of ratio, tutor count computed correctly | unit | N/A | N/A Wave 0 |
| STATS-04 | Win condition heuristic identifies big threats + alt wins | unit | N/A | N/A Wave 0 |

### Wave 0 Gaps
No frontend test infrastructure exists in this project. All prior phases (7-9) shipped without tests. Setting up Vitest is possible but deferred per project convention -- validation is done visually and via manual testing.

**Manual validation approach (project convention):**
- Load a known deck in the editor
- Visually verify each stat section renders correct counts
- Spot-check specific cards (e.g., Swords to Plowshares = hard removal, Cultivate = ramp spell)

## Open Questions

1. **Should card names be shown in each category, or just counts?**
   - What we know: CONTEXT.md lists this as Claude's discretion
   - Recommendation: Show counts prominently with expandable card name lists. Counts give the at-a-glance view ("12 removal"), card names let users verify ("wait, why is X counted as removal?"). Use a simple disclosure/collapsible pattern.

2. **How to handle lands that produce mana but aren't "ramp"?**
   - What we know: Sol Ring (artifact) is ramp. Command Tower (land) produces mana but isn't ramp in the traditional sense.
   - Recommendation: Exclude lands from ramp analysis entirely. Ramp means "acceleration beyond land drops" -- the user's locked definition specifically mentions creatures, artifacts, and spells as subcategories. Lands with "add {" are just mana sources, not ramp.

3. **Transform DFC back faces in analysis?**
   - What we know: `getOracleText()` returns only front face for Transform/Modal DFC. The recommended DTO fix combines both faces.
   - Recommendation: Include both faces. Valki // Tibalt should count as a planeswalker win condition even though the front face is a creature.

## Sources

### Primary (HIGH confidence)
- `forge-gui-web/src/main/java/forge/web/dto/DeckDetailDto.java` -- Current DTO structure, missing oracleText/power/toughness
- `forge-gui-web/frontend/src/types/deck.ts` -- Frontend type definition, confirms no oracleText
- `forge-core/src/main/java/forge/card/CardSplitType.java` -- Adventure uses USE_PRIMARY_FACE (oracle text only from creature half)
- `forge-core/src/main/java/forge/card/CardRules.java:280-288` -- getOracleText() switch on aggregation method
- `forge-gui/res/cardsfolder/` -- Actual oracle text format from card definition files
- `forge-gui-web/frontend/src/lib/deck-stats.ts` -- Existing computation pattern
- `forge-gui-web/frontend/src/components/deck-editor/StatsPanel.tsx` -- Existing rendering pattern
- `forge-gui-web/frontend/src/components/charts/TypeBreakdown.tsx` -- SVG horizontal bar pattern

### Secondary (MEDIUM confidence)
- Oracle text regex patterns -- based on analysis of actual card text from Forge card files, but not exhaustively tested against all 20,000+ cards

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new libraries, uses existing SVG + Tailwind pattern
- Architecture: HIGH -- follows established deck-stats.ts + charts pattern exactly
- Backend DTO change: HIGH -- verified exact Java class structure and missing fields
- Adventure card fix: HIGH -- verified CardSplitType enum and actual card file oracle text
- Oracle text regex patterns: MEDIUM -- verified against sample cards, but edge cases exist across 20,000+ cards
- Pitfalls: HIGH -- identified from code inspection

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable domain, no external dependencies changing)
