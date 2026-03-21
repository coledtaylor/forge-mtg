# Phase 10: Advanced Deck Stats - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Add oracle-text-based deck analysis to the existing StatsPanel: removal count, ramp density, card draw sources, interaction range, consistency metrics, and win condition analysis. Pure frontend computation from existing DeckCardEntry data (oracleText field). No backend changes needed.

</domain>

<decisions>
## Implementation Decisions

### Card Classification Rules

**Removal (broad definition, subcategorized):**
- Hard removal: "destroy target", "exile target", "destroy all" (sweepers)
- Soft removal: "deals X damage to target creature/planeswalker", "-X/-X", "fight", "return target ... to its owner's hand" (bounce), "sacrifice" (target-based)
- Subcategorize into: hard removal, soft removal, sweepers
- Include planeswalker -X abilities that remove permanents

**Ramp (inclusive):**
- Mana dorks: creatures with "add {" or "tap: add"
- Land search: "search your library for a ... land"
- Mana rocks: artifacts with "add {"
- Ritual effects: "add {X}{X}" type effects
- Subcategorize: creatures, artifacts, spells

**Card Draw (inclusive):**
- Dedicated draw: "draw X cards" where X > 1
- Cantrips: "draw a card" (draw 1 as part of effect)
- Card filtering: "scry", "surveil", "look at the top X"
- Subcategorize: draw, cantrips, filtering

**Win Conditions (heuristic-based):**
- Alternate win cons: "you win the game"
- Big threats: CMC 5+ creatures with power 4+
- Planeswalkers (all)
- Combo indicators: cards that reference "infinite", "untap" loops, or well-known combo patterns
- Won't be perfect — gives useful signal, not precise classification

**Interaction Range:**
- Creature answers: destroy/exile/damage creature
- Enchantment answers: destroy/exile enchantment
- Artifact answers: destroy/exile artifact
- Planeswalker answers: damage/destroy/exile planeswalker
- Graveyard answers: exile from graveyard, "exile all cards from graveyards"
- Land answers: destroy/exile land

**Consistency Metrics:**
- 4-of ratio: count of cards with 4 copies / total unique cards
- Tutor/search count: "search your library" effects
- Threat redundancy: number of distinct win condition cards

### Stats Display Layout
- Add new sections below the existing StatsPanel content (mana curve, color distribution, type breakdown)
- Scrollable continuous view — no separate tab
- Sections: "Deck Composition" (removal/ramp/draw with subcategory counts), "Interaction Range" (coverage grid), "Consistency" (4-of ratio, tutors, redundancy), "Win Conditions" (threats list with count)

### Interaction Range Visualization
- Coverage grid: threat types as rows (Creatures, Enchantments, Artifacts, Planeswalkers, Graveyards, Lands)
- Each row shows: count of answers + green checkmark if any exist, red X if zero
- Simple, scannable at a glance

### Claude's Discretion
- Exact oracle text regex patterns for each classification
- Edge case handling (modal spells, split cards, adventure cards)
- Visual design of composition bars and grid styling
- Whether to show individual card names in each category or just counts

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Stats Infrastructure
- `forge-gui-web/frontend/src/components/deck-editor/StatsPanel.tsx` — Current stats panel with mana curve, color dist, type breakdown
- `forge-gui-web/frontend/src/lib/deck-stats.ts` — Existing computation functions (computeManaCurve, etc.)
- `forge-gui-web/frontend/src/types/deck.ts` — DeckCardEntry type with oracleText field

### Chart Components
- `forge-gui-web/frontend/src/components/charts/ManaCurveChart.tsx` — SVG bar chart pattern
- `forge-gui-web/frontend/src/components/charts/TypeBreakdown.tsx` — Horizontal bar pattern
- `forge-gui-web/frontend/src/components/charts/ColorDistribution.tsx` — Donut chart pattern

### Research
- `.planning/research/FEATURES.md` — Advanced stats as table stakes feature
- `.planning/research/ARCHITECTURE.md` — Oracle text analysis approach

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `deck-stats.ts` — Existing pattern for pure computation functions. New analysis functions follow the same pattern.
- `TypeBreakdown.tsx` horizontal bars — Can be reused for removal/ramp/draw counts
- `DeckCardEntry.oracleText` — Already available from Phase 7 DTO enrichment
- `StatsPanel.tsx` — Container to extend with new sections

### Established Patterns
- Pure computation in `lib/` (e.g., `deck-stats.ts`), rendering in `components/charts/`
- SVG-based charts with Tailwind CSS styling
- `useMemo` for expensive computations in StatsPanel

### Integration Points
- `StatsPanel.tsx` — Add new sections below existing content
- `deck-stats.ts` or new `deck-analysis.ts` — Oracle text analysis functions
- `DeckCardEntry` — Input type for all analysis

</code_context>

<specifics>
## Specific Ideas

- Think of this like a deck doctor — at a glance you can see "this deck has 0 enchantment removal" and know it's a blind spot
- The coverage grid should feel like a checklist — green = covered, red = gap

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 10-advanced-deck-stats*
*Context gathered: 2026-03-21*
