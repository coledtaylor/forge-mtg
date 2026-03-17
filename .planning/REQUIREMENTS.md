# Requirements: Forge Web Client

**Defined:** 2026-03-16
**Core Value:** Build a deck in the browser and play a full game of Magic against the AI

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### API & Infrastructure

- [ ] **API-01**: Java backend exposes REST endpoints for card search with filters (name, type, color, CMC, format legality)
- [ ] **API-02**: Java backend exposes REST endpoints for deck CRUD (create, save, load, delete)
- [ ] **API-03**: Java backend exposes WebSocket endpoint for real-time game state during matches
- [ ] **API-04**: Backend implements IGuiGame interface to bridge Forge engine events to WebSocket messages
- [ ] **API-05**: Backend implements IGuiBase interface for web-compatible platform operations
- [ ] **API-06**: Backend initializes Forge card database and static data on startup

### Deck Builder

- [ ] **DECK-01**: User can search cards by name, type, color, CMC, and format legality
- [ ] **DECK-02**: User can see card images fetched from Scryfall API
- [ ] **DECK-03**: User can create, name, save, load, and delete decks
- [ ] **DECK-04**: User can view deck as a text list with card names and quantities
- [ ] **DECK-05**: User can view deck as a visual card image grid/gallery
- [ ] **DECK-06**: User can add and remove cards with quantity controls
- [ ] **DECK-07**: User can see mana curve chart for their deck
- [ ] **DECK-08**: User can see card type distribution (creatures, instants, etc.)
- [ ] **DECK-09**: User can see color distribution across the deck
- [ ] **DECK-10**: User can see format validation results (legal/illegal with reasons)
- [ ] **DECK-11**: User can manage sideboard cards (add, remove, move to/from main)
- [ ] **DECK-12**: User can set a commander for Commander format decks
- [ ] **DECK-13**: User can quickly add basic lands with quantity controls

### Gameplay

- [ ] **GAME-01**: User can see the battlefield with all zones rendered (hand, battlefield, graveyard, exile, library, stack)
- [ ] **GAME-02**: User can see cards rendered with tap/untap state, counters, and attachments
- [ ] **GAME-03**: User can see phase/turn indicator showing current game phase
- [ ] **GAME-04**: User can see both players' life totals
- [ ] **GAME-05**: User can see mana pool with available mana
- [ ] **GAME-06**: User can see the stack with spells/abilities in resolution order
- [ ] **GAME-07**: User receives prompts for required actions ("Choose attacker", "Choose target", etc.)
- [ ] **GAME-08**: User can make choices from selection dialogs (pick cards, choose modes, order cards)
- [ ] **GAME-09**: User can declare attackers and blockers in combat with visual assignment
- [ ] **GAME-10**: User can hover/click any card to see an enlarged detail view
- [ ] **GAME-11**: User can cast spells and activate abilities from hand and battlefield

### Game Setup

- [ ] **SETUP-01**: User can select a format (Commander, Standard, casual 60-card, Jumpstart)
- [ ] **SETUP-02**: User can select a deck from saved decks for the game
- [ ] **SETUP-03**: User can start a game against the AI
- [ ] **SETUP-04**: User can navigate from deck builder to game with the current deck pre-selected

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Gameplay Enhancements

- **GAME-12**: User can see scrollable game log of all actions
- **GAME-13**: User can use keyboard shortcuts for common actions (pass, confirm, undo)
- **GAME-14**: User can set auto-yield/auto-pass for specific phases
- **GAME-15**: User can select AI difficulty level in game setup
- **GAME-16**: User can play in goldfish/solitaire mode (no opponent)
- **GAME-17**: User can undo the last action (where Forge engine supports it)

### Deck Builder Enhancements

- **DECK-14**: User can import deck lists via text paste ("4 Lightning Bolt" format)
- **DECK-15**: User can see advanced statistics (removal count, ramp density, draw density)
- **DECK-16**: User can see card oracle text alongside the image

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multiplayer / networked play | Local-only tool, single user. Networking adds massive complexity |
| User accounts / authentication | No need for local tool |
| Mobile-responsive UI | Desktop browser target (min 1280px viewport) |
| Adventure / Quest mode | Different product, focus on constructed play |
| Draft / Sealed / Limited modes | Complex UI flow, requires separate research phase |
| Collection tracking / inventory | Users who want this use Moxfield/ManaBox |
| Price data integration | Gameplay tool, not purchasing tool |
| Card scanning / camera | Desktop web client, no camera workflow |
| Animated card effects | Enormous effort for visual polish that doesn't improve gameplay |
| Social features (sharing, comments) | Local-only tool with no server infrastructure |
| Deck import from Arena/MTGO formats | Proprietary formats, marginal value in v1 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| (populated by roadmapper) | | |

**Coverage:**
- v1 requirements: 30 total
- Mapped to phases: 0
- Unmapped: 30 ⚠️

---
*Requirements defined: 2026-03-16*
*Last updated: 2026-03-16 after initial definition*
