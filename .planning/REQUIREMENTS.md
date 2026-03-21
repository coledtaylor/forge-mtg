# Requirements: Forge Web Client v2.0

**Defined:** 2026-03-20
**Core Value:** Build a deck in the browser and play a full game of Magic against the AI — the complete loop from deckbuilding to gameplay must work end-to-end.

## v2.0 Requirements

Requirements for v2.0 milestone. Each maps to roadmap phases.

### Gameplay UX

- [x] **GUX-01**: User can see a clear priority indicator showing when they have priority and whose turn it is
- [x] **GUX-02**: User can distinguish between "OK" (confirm current action) and "Pass Priority" (pass to next phase) with clear labeling and visual distinction
- [x] **GUX-03**: User can see visual feedback when targeting cards (highlighted selection, confirm/cancel targeting mode)
- [x] **GUX-04**: User can see a scrollable game log showing all game actions in chronological order
- [x] **GUX-05**: User can use keyboard shortcuts for common actions (pass priority, confirm, undo, full control toggle)
- [x] **GUX-06**: User can set auto-yield for specific phases (e.g., always pass priority during upkeep when no instants)
- [x] **GUX-07**: User can select AI difficulty level (Easy, Medium, Hard) in game setup
- [x] **GUX-08**: User can play in goldfish/solitaire mode with no opponent (for testing combos and goldfishing kill turns)
- [x] **GUX-09**: User can undo the last spell cast (where the Forge engine supports it, labeled "Undo Last Spell")

### Card Quality

- [x] **CARD-01**: Card images use direct Scryfall set/collector-number URLs instead of name-based lookups (faster, more reliable)
- [x] **CARD-02**: Card images prefer recent core set or standard-legal printings for recognizable, mainstream art
- [x] **CARD-03**: Card images are English-only (no foreign-language art variants)
- [x] **CARD-04**: User can see card oracle text alongside the card image (hover preview or detail panel)

### Jumpstart Format

- [x] **JUMP-01**: User can create 20-card Jumpstart packs in the deck builder
- [x] **JUMP-02**: User can browse and select from Forge's existing Jumpstart pack definitions
- [x] **JUMP-03**: User can select two packs in game setup to merge into a 40-card deck
- [x] **JUMP-04**: AI opponent selects two packs in game setup (random or from available packs)
- [x] **JUMP-05**: Jumpstart game setup validates that exactly two packs are selected before starting

### Deck Simulation

- [x] **SIM-01**: User can trigger a simulation run from the deck builder screen
- [x] **SIM-02**: User can configure the number of games to simulate (e.g., 10, 50, 100, 500)
- [x] **SIM-03**: Simulation runs headless AI vs AI games using the selected deck against a gauntlet of opponent decks
- [ ] **SIM-04**: User can see overall win rate, win rate by matchup, and win rate on play vs draw
- [ ] **SIM-05**: User can see mulligan statistics (keep rate, average mulligans, win rate after mulligan)
- [ ] **SIM-06**: User can see speed statistics (average kill turn, fastest/slowest win, time to first threat)
- [x] **SIM-07**: User can see mana statistics (screw rate, flood rate, average turn for 3rd/4th land drop)
- [ ] **SIM-08**: User can see resource statistics (average cards drawn per game, turns with empty hand, average life at win/loss)
- [ ] **SIM-09**: User can see per-card performance (win rate when drawn, dead card rate, cards stuck in hand)
- [x] **SIM-10**: Simulation computes an Elo rating for the deck based on gauntlet performance
- [ ] **SIM-11**: Simulation classifies the deck's play style (aggro/midrange/control/combo radar chart)
- [x] **SIM-12**: User can see simulation progress in real time (games completed / total, running stats updating)

### Advanced Deck Stats

- [x] **STATS-01**: User can see advanced deck statistics: removal count, ramp density, card draw source count
- [x] **STATS-02**: User can see interaction range analysis (can the deck answer creatures, enchantments, artifacts, graveyards?)
- [x] **STATS-03**: User can see consistency metrics (4-of ratio, tutor/search count, redundancy in threats)
- [x] **STATS-04**: User can see win condition analysis (number of distinct win cons, redundancy assessment)

### Tech Debt

- [x] **DEBT-01**: Format validation handles "Casual 60-card" and "Jumpstart" format strings without returning 400
- [x] **DEBT-02**: GameStartConfig type consolidated into a single shared definition
- [x] **DEBT-03**: AI deck selection provides meaningful decks for all formats (not 60 Forests fallback)

## Future Requirements

Deferred beyond v2.0.

### Gameplay Enhancements

- **GAME-18**: User can see animated card effects (tap animations, damage indicators)
- **GAME-19**: User can spectate AI vs AI games in real time (not just headless)

### Social

- **SOCIAL-01**: User can share deck simulation results (export as image or link)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multiplayer / networked play | Local-only, single user |
| User accounts or authentication | No need for local tool |
| Mobile-optimized UI | Desktop browser is the target |
| Adventure mode | Gameplay focus only |
| Draft / Sealed / Limited modes | Complex UI flow, separate milestone |
| Animated card effects | Enormous effort for visual polish that doesn't improve gameplay |
| Real-time AI spectating | Headless simulation is sufficient for deck testing; spectating is v3 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| GUX-01 | Phase 8 | Complete |
| GUX-02 | Phase 8 | Complete |
| GUX-03 | Phase 8 | Complete |
| GUX-04 | Phase 8 | Complete |
| GUX-05 | Phase 8 | Complete |
| GUX-06 | Phase 9 | Complete |
| GUX-07 | Phase 8 | Complete |
| GUX-08 | Phase 8 | Complete |
| GUX-09 | Phase 9 | Complete |
| CARD-01 | Phase 7 | Complete |
| CARD-02 | Phase 7 | Complete |
| CARD-03 | Phase 7 | Complete |
| CARD-04 | Phase 8 | Complete |
| JUMP-01 | Phase 11 | Complete |
| JUMP-02 | Phase 11 | Complete |
| JUMP-03 | Phase 11 | Complete |
| JUMP-04 | Phase 11 | Complete |
| JUMP-05 | Phase 11 | Complete |
| SIM-01 | Phase 12 | Complete |
| SIM-02 | Phase 12 | Complete |
| SIM-03 | Phase 12 | Complete |
| SIM-04 | Phase 12 | Pending |
| SIM-05 | Phase 12 | Pending |
| SIM-06 | Phase 12 | Pending |
| SIM-07 | Phase 12 | Complete |
| SIM-08 | Phase 12 | Pending |
| SIM-09 | Phase 12 | Pending |
| SIM-10 | Phase 12 | Complete |
| SIM-11 | Phase 12 | Pending |
| SIM-12 | Phase 12 | Complete |
| STATS-01 | Phase 10 | Complete |
| STATS-02 | Phase 10 | Complete |
| STATS-03 | Phase 10 | Complete |
| STATS-04 | Phase 10 | Complete |
| DEBT-01 | Phase 7 | Complete |
| DEBT-02 | Phase 7 | Complete |
| DEBT-03 | Phase 7 | Complete |

**Coverage:**
- v2.0 requirements: 37 total
- Mapped to phases: 37
- Unmapped: 0

---
*Requirements defined: 2026-03-20*
*Last updated: 2026-03-20 after roadmap creation*
