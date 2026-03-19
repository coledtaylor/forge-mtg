# Phase 4: Game Board - Research

**Researched:** 2026-03-19
**Domain:** Real-time game UI with WebSocket state management, complex interactive board layout
**Confidence:** HIGH

## Summary

Phase 4 builds the full gameplay UI -- the most complex frontend work in the project. The engine bridge (Phase 1) is complete and provides a well-defined WebSocket protocol with typed DTOs (GameStateDto, CardDto, PlayerDto, CombatDto, SpellAbilityDto, ZoneUpdateDto). The frontend receives a full GAME_STATE snapshot on connect, then incremental ZONE_UPDATE, PHASE_UPDATE, TURN_UPDATE, COMBAT_UPDATE, STACK_UPDATE, BUTTON_UPDATE, and prompt messages. The client responds with BUTTON_OK/BUTTON_CANCEL for priority and CHOICE_RESPONSE/CONFIRM_RESPONSE/AMOUNT_RESPONSE for engine prompts, each keyed by an `inputId`.

The primary challenge is state management: the game board needs a single reactive store that merges full snapshots with incremental updates, tracks which cards are in which zones, handles combat overlays, and manages the prompt/response lifecycle. Zustand with Immer is the right fit -- lightweight, works outside React for WebSocket handlers, and Immer simplifies nested state updates on card/player objects.

The secondary challenge is the Arena-style board layout with many interactive zones, hover previews, combat assignment UI, and targeting highlights. This is pure component composition with Tailwind -- no additional libraries needed. The existing CardHoverPreview, CardImage, ManaCost, and mana-font assets from Phases 2-3 are directly reusable.

**Primary recommendation:** Build a Zustand game store fed by a WebSocket manager class. Layer React components on top that select slices of game state. Handle all prompts through a unified action bar driven by store state.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Arena-style layout: opponent at top, player at bottom, battlefield center
- Distinct lanes per player: lands row (bottom) and creatures/other permanents row (top)
- Subtle visual dividers between zones -- polished, structured visual hierarchy
- Player info bars at top edge (opponent) and bottom edge (you) -- thin horizontal bars with name, life total, hand count, mana pool
- Phase strip in center divider: horizontal bar showing Untap -> Upkeep -> Draw -> Main 1 -> Combat -> Main 2 -> End, current phase highlighted, turn ownership indicated
- Vertical stack display on the right side -- spells in resolution order, top spell at top, each with card thumbnail + name
- Fanned overlap with hover-to-raise for hand -- cards overlap slightly in an arc at the bottom
- Double-click a card in hand to cast/play it; single click selects/previews
- Tapped cards rotate 90 degrees clockwise
- Attachments stacked behind the card they're on, offset slightly up/right -- each supports hover-to-inspect
- Counters shown as small colored badge overlays on the card
- Separate rows: lands in bottom lane, creatures/other permanents in upper lane per player
- Graveyard and exile as pile icon showing top card + count badge, click to expand into scrollable overlay
- Hover preview floating near cursor -- reuses CardHoverPreview + useCardHover from Phase 3
- Bottom action bar for all prompts -- fixed bar above the hand area, non-modal
- Arena-style priority: always pause when player has priority, explicitly pass with "Pass" button
- Auto-pass ONLY when player has zero playable cards and zero activatable abilities
- Valid targets highlight/glow on the board; click highlighted card to select; invalid cards dimmed
- Engine handles mana payment automatically (auto-taps lands)
- Combat: eligible creatures highlight, click to toggle attacker (shift forward + red glow), confirm with OK
- Blockers: click blocker then click attacker, line/arrow connects them, click blocker again to reassign
- Combat damage: creatures flash red, P/T updates real-time, creatures that die fade out, life total animation
- Mana pool in player info bar: colored mana symbols with quantity, only shows colors with available mana

### Claude's Discretion
- WebSocket reconnection/error handling
- Card sizing and responsive scaling within zones
- Animation timing and easing curves
- How to handle very large battlefields (many permanents)
- Game over screen design
- Sound effects (if any -- likely skip for v1)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| GAME-01 | User can see the battlefield with all zones rendered (hand, battlefield, graveyard, exile, library, stack) | Zustand game store with zone-indexed card maps; board layout with dedicated zone components |
| GAME-02 | User can see cards rendered with tap/untap state, counters, and attachments | CardDto provides `tapped`, `counters`, `attachmentIds`; GameCard component renders all states |
| GAME-03 | User can see phase/turn indicator showing current game phase | PHASE_UPDATE message with PhaseType enum names; PhaseStrip component |
| GAME-04 | User can see both players' life totals | PlayerDto.life field; PlayerInfoBar component with animated life changes |
| GAME-05 | User can see mana pool with available mana | PlayerDto.mana map (White/Blue/Black/Red/Green/Colorless); ManaPool component with mana-font |
| GAME-06 | User can see the stack with spells/abilities in resolution order | STACK_UPDATE with SpellAbilityDto list; StackPanel component |
| GAME-07 | User receives prompts for required actions | PROMPT_CHOICE/PROMPT_CONFIRM/PROMPT_AMOUNT + BUTTON_UPDATE messages; ActionBar component |
| GAME-08 | User can make choices from selection dialogs | CHOICE_RESPONSE with index-based selection; ChoiceDialog within ActionBar |
| GAME-09 | User can declare attackers and blockers in combat | COMBAT_UPDATE with CombatDto; CombatOverlay with attacker/blocker selection modes |
| GAME-10 | User can hover/click any card to see enlarged detail view | Reuse CardHoverPreview + useCardHover from Phase 3 |
| GAME-11 | User can cast spells and activate abilities from hand and battlefield | Double-click triggers BUTTON_OK (play from hand) or getAbilityToPlay prompt; targeting via PROMPT_CHOICE |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| zustand | 5.0.12 | Game state management | Lightweight, works outside React (WebSocket handlers can update store directly), subscriptions for selective re-renders |
| immer | 11.1.4 | Immutable state updates | Zustand immer middleware; nested card/player state updates are painful without it |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| react | 19.2.4 | UI framework | Already installed |
| tailwindcss | 4.2.2 | Styling | Already installed, all existing components use it |
| lucide-react | 0.577.0 | Icons | Already installed, use for zone icons (sword for combat, skull for graveyard, etc.) |
| mana-font | 1.18.0 | Mana symbols | Already installed, used in ManaCost component |
| tw-animate-css | 1.4.0 | CSS animations | Already installed, use for life total changes, damage flashes |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| zustand | useReducer + Context | Context causes unnecessary re-renders; zustand slice subscriptions are surgical |
| zustand | Redux Toolkit | Overkill for this scope; zustand is 1KB vs RTK's 30KB+ |
| Custom WebSocket | socket.io-client | Forge uses raw WebSocket, not Socket.IO; adding socket.io would require server changes |

**Installation:**
```bash
npm install zustand immer
```

No other new dependencies needed -- everything else is already in package.json.

## Architecture Patterns

### Recommended Project Structure
```
src/
├── components/
│   ├── game/                    # All game board components
│   │   ├── GameBoard.tsx        # Top-level game view, layout shell
│   │   ├── PlayerInfoBar.tsx    # Name, life, hand count, mana pool
│   │   ├── ManaPool.tsx         # Colored mana symbols with quantities
│   │   ├── PhaseStrip.tsx       # Phase/turn indicator bar
│   │   ├── HandZone.tsx         # Fan of cards at bottom
│   │   ├── BattlefieldZone.tsx  # Per-player battlefield (lands row + creatures row)
│   │   ├── GameCard.tsx         # Single card on battlefield (tap, counters, attachments)
│   │   ├── HandCard.tsx         # Single card in hand (hover-to-raise, double-click to play)
│   │   ├── StackPanel.tsx       # Vertical stack display on right side
│   │   ├── ZonePile.tsx         # Graveyard/exile/library pile icon with count
│   │   ├── ZoneOverlay.tsx      # Scrollable card list when clicking a pile
│   │   ├── ActionBar.tsx        # Prompt text + action buttons (OK/Cancel/Pass)
│   │   ├── ChoiceDialog.tsx     # Multi-select choice within action bar
│   │   ├── CombatOverlay.tsx    # Attacker/blocker assignment arrows
│   │   └── GameOverScreen.tsx   # End-of-game display
│   └── ... (existing components)
├── stores/
│   └── gameStore.ts             # Zustand store for all game state
├── hooks/
│   ├── useCardHover.ts          # Existing -- reuse
│   └── useGameWebSocket.ts      # WebSocket connection + message dispatch
├── lib/
│   ├── gameWebSocket.ts         # WebSocket manager class (connect/send/reconnect)
│   └── gameTypes.ts             # TypeScript types matching Java DTOs
└── types/
    └── game.ts                  # Game-specific type definitions
```

### Pattern 1: Zustand Store with Immer Middleware
**What:** Single store holds all game state (players, cards, combat, phase, prompts). WebSocket handler writes directly to store. React components subscribe to specific slices.
**When to use:** All game state management.
**Example:**
```typescript
import { create } from 'zustand'
import { immer } from 'zustand/middleware/immer'

interface GameState {
  players: Map<number, PlayerState>
  cards: Map<number, CardState>
  stack: StackItem[]
  combat: CombatState | null
  phase: string | null
  turn: number
  activePlayerId: number
  prompt: PromptState | null
  buttons: ButtonState | null
  gameOver: boolean
}

interface GameActions {
  applyGameState: (dto: GameStateDto) => void
  applyZoneUpdate: (updates: ZoneUpdateDto[]) => void
  applyPhaseUpdate: (phase: string) => void
  applyCombatUpdate: (combat: CombatDto | null) => void
  applyStackUpdate: (stack: SpellAbilityDto[]) => void
  applyButtonUpdate: (buttons: ButtonPayload) => void
  setPrompt: (prompt: PromptState | null) => void
  reset: () => void
}

export const useGameStore = create<GameState & GameActions>()(
  immer((set) => ({
    players: new Map(),
    cards: new Map(),
    stack: [],
    combat: null,
    phase: null,
    turn: 0,
    activePlayerId: -1,
    prompt: null,
    buttons: null,
    gameOver: false,

    applyGameState: (dto) => set((state) => {
      // Full snapshot -- replace everything
      state.cards.clear()
      for (const card of dto.cards) {
        state.cards.set(card.id, card)
      }
      state.players.clear()
      for (const player of dto.players) {
        state.players.set(player.id, player)
      }
      state.stack = dto.stack
      state.combat = dto.combat
      state.phase = dto.phase
      state.turn = dto.turn
      state.activePlayerId = dto.activePlayerId
    }),
    // ... other actions
  }))
)
```

### Pattern 2: WebSocket Manager (Class, not Hook)
**What:** Plain TypeScript class that manages WebSocket lifecycle and dispatches messages to the Zustand store. Not a React hook -- the class exists outside the React tree.
**When to use:** WebSocket connection management.
**Example:**
```typescript
class GameWebSocket {
  private ws: WebSocket | null = null
  private gameId: string

  constructor(gameId: string) {
    this.gameId = gameId
  }

  connect() {
    this.ws = new WebSocket(`ws://${location.host}/ws/game/${this.gameId}`)

    this.ws.onmessage = (event) => {
      const msg = JSON.parse(event.data) as OutboundMessage
      const store = useGameStore.getState()

      switch (msg.type) {
        case 'GAME_STATE':
          store.applyGameState(msg.payload)
          break
        case 'ZONE_UPDATE':
          store.applyZoneUpdate(msg.payload)
          break
        case 'PHASE_UPDATE':
          store.applyPhaseUpdate(msg.payload.phase)
          break
        case 'PROMPT_CHOICE':
        case 'PROMPT_CONFIRM':
        case 'PROMPT_AMOUNT':
          store.setPrompt({ type: msg.type, inputId: msg.inputId, payload: msg.payload })
          break
        case 'BUTTON_UPDATE':
          store.applyButtonUpdate(msg.payload)
          break
        // ... etc
      }
    }
  }

  send(msg: InboundMessage) {
    this.ws?.send(JSON.stringify(msg))
  }

  sendButtonOk() {
    this.send({ type: 'BUTTON_OK', inputId: null, payload: null })
  }

  sendChoiceResponse(inputId: string, payload: unknown) {
    this.send({ type: 'CHOICE_RESPONSE', inputId, payload })
  }
}
```

### Pattern 3: Zone-Based Card Rendering
**What:** Cards are stored flat in the store by ID. Zone components derive their card list by filtering: player zones map (from PlayerDto) contains card IDs per zone. Components select their slice.
**When to use:** Rendering any zone.
**Example:**
```typescript
// In HandZone.tsx
const handCardIds = useGameStore((s) => s.players.get(humanPlayerId)?.zones.Hand ?? [])
const cards = useGameStore((s) => handCardIds.map(id => s.cards.get(id)).filter(Boolean))
```

### Pattern 4: Prompt Response Lifecycle
**What:** Engine sends prompt with inputId -> store.setPrompt() -> ActionBar renders buttons/choices -> user clicks -> WebSocket sends response with same inputId -> store.setPrompt(null).
**When to use:** All engine prompts (choices, confirms, amounts, button presses).

### Anti-Patterns to Avoid
- **Storing game state in React component state:** The WebSocket handler runs outside React. Zustand store is accessible from both React and plain JS.
- **Creating WebSocket in useEffect:** Race conditions with StrictMode double-mount. Use a class instantiated once, ref'd in a hook.
- **Re-rendering entire board on every message:** Use Zustand selectors to subscribe to specific slices. A phase update should not re-render the hand.
- **Building card lookup from arrays on every render:** Store cards in a Map<number, CardState> for O(1) lookup by ID.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| State management with WebSocket | Custom pub/sub or Context | Zustand + immer | Selective re-renders, works outside React, immer simplifies deep updates |
| Card image rendering | Custom image loader | Existing CardImage component | Already handles Scryfall URLs, loading states, error fallback |
| Hover preview | New tooltip system | Existing CardHoverPreview + useCardHover | Already positioned correctly, handles viewport edge detection |
| Mana symbol rendering | SVG icons or custom sprites | Existing ManaCost component + mana-font CSS | Already parsed and rendered correctly |
| CSS animations | JavaScript animation library (framer-motion, etc.) | Tailwind transition classes + tw-animate-css | Already installed, sufficient for the animations needed (fades, rotations, color flashes) |
| WebSocket reconnection | Custom retry logic from scratch | Exponential backoff with 3 retries in GameWebSocket class | Simple enough to not need a library; reconnecting-websocket npm package would work but adds a dependency for 20 lines of code |

**Key insight:** Phase 4 is about composition, not new infrastructure. The engine bridge protocol is complete, the card rendering pipeline exists, and the styling system is established. The new work is layout, state management, and interaction handlers.

## Common Pitfalls

### Pitfall 1: ZONE_UPDATE is Notification-Only, Not a Diff
**What goes wrong:** ZoneUpdateDto only tells you WHICH zones changed for WHICH player. It does NOT include the new card list for that zone. You must request a fresh GAME_STATE or already have the data.
**Why it happens:** The engine calls `updateZones()` as a notification that something changed. The actual card data comes from `updateCards()` or a full `GAME_STATE` re-send.
**How to avoid:** On ZONE_UPDATE, the frontend should either: (a) rely on the fact that `updateCards()` is typically called alongside `updateZones()`, delivering fresh CardDto data, or (b) re-derive zone membership from the cards' `zoneType` field. The store should handle both full snapshots (GAME_STATE) and incremental card updates.
**Warning signs:** Cards appearing in wrong zones or disappearing after zone changes.

### Pitfall 2: Dual Input System (Buttons vs. Prompts)
**What goes wrong:** Mixing up BUTTON_OK/BUTTON_CANCEL with CHOICE_RESPONSE/CONFIRM_RESPONSE. They serve different engine subsystems.
**Why it happens:** The Forge engine has TWO input paths: (1) InputQueue for mulligan/priority decisions (uses `selectButtonOk()`/`selectButtonCancel()` on the game controller), and (2) CompletableFuture sendAndWait for choices/targeting (uses `inputBridge.complete(inputId, responseJson)`).
**How to avoid:** BUTTON_UPDATE messages from the server tell the client what buttons to show (label1, label2, enable1, enable2). The client sends BUTTON_OK or BUTTON_CANCEL (no inputId needed). PROMPT_CHOICE/PROMPT_CONFIRM/PROMPT_AMOUNT messages include an `inputId` -- the response MUST include that same `inputId`.
**Warning signs:** Game hangs waiting for input; response sent but engine doesn't receive it.

### Pitfall 3: StrictMode Double-Mount WebSocket
**What goes wrong:** React 19 StrictMode mounts components twice in development, creating two WebSocket connections.
**Why it happens:** useEffect cleanup runs, but the second mount creates a new connection before the first one closes.
**How to avoid:** Use a ref to track the WebSocket instance. Or better: instantiate the GameWebSocket class outside React, pass it to components via context or store.
**Warning signs:** Duplicate messages, "game session already exists" errors.

### Pitfall 4: Map Serialization in Zustand/Immer
**What goes wrong:** JavaScript `Map` objects don't serialize to JSON and Immer's `enableMapSet()` must be called.
**Why it happens:** Immer doesn't support Map/Set by default.
**How to avoid:** Call `enableMapSet()` from immer at app startup, OR use plain objects (`Record<number, CardState>`) instead of Maps. Plain objects are simpler and JSON-compatible. Recommendation: use `Record<number, CardState>` for cards and `Record<number, PlayerState>` for players.
**Warning signs:** State updates silently failing, Maps becoming empty after immer produces.

### Pitfall 5: CardDto Has No setCode/collectorNumber
**What goes wrong:** Attempting to use CardImage component directly with CardDto fails because CardDto has `name` but no `setCode` or `collectorNumber` (which Scryfall needs).
**Why it happens:** The engine CardView doesn't expose Scryfall-specific identifiers. CardDto has `name` which is enough for a Scryfall name-based lookup, but not the exact image URL pattern CardImage uses.
**How to avoid:** Use Scryfall's card name search API (`https://api.scryfall.com/cards/named?fuzzy={name}&format=image&version=normal`) instead of the set/collector-number URL. Or add a Scryfall lookup cache that maps card names to image URLs. Alternatively, consider adding setCode/collectorNumber to CardDto on the Java side if the engine has that data.
**Warning signs:** Blank card images on the game board despite working in the deck builder.

### Pitfall 6: Stale Selectors After Incremental Updates
**What goes wrong:** React component renders stale data because the Zustand selector reference didn't change.
**Why it happens:** Zustand uses shallow equality by default. If you return a derived array from a selector, a new array reference is created every time, causing unnecessary re-renders. If you memoize too aggressively, you miss updates.
**How to avoid:** Use `useShallow` from `zustand/react/shallow` for selectors that return objects/arrays. For card lists, select the card ID array (stable reference if zone didn't change), then map to card objects in the component.
**Warning signs:** UI not updating after zone changes, or excessive re-renders on every message.

## Code Examples

### TypeScript Types Matching Java DTOs
```typescript
// lib/gameTypes.ts

export interface GameStateDto {
  players: PlayerDto[]
  cards: CardDto[]
  stack: SpellAbilityDto[]
  combat: CombatDto | null
  phase: string | null  // PhaseType.name() e.g. "MAIN1", "COMBAT_DECLARE_ATTACKERS"
  turn: number
  activePlayerId: number
}

export interface PlayerDto {
  id: number
  name: string
  life: number
  poisonCounters: number
  mana: Record<string, number>  // Keys: "White", "Blue", "Black", "Red", "Green", "Colorless"
  zones: Record<string, number[]>  // Keys: ZoneType.name() e.g. "Hand", "Battlefield", "Graveyard"
}

export interface CardDto {
  id: number
  name: string
  manaCost: string | null
  power: number
  toughness: number
  colors: string[]
  ownerId: number
  controllerId: number
  zoneType: string | null  // "Hand", "Battlefield", "Graveyard", "Exile", "Library", "Stack"
  tapped: boolean
  counters: Record<string, number> | null  // e.g. { "P1P1": 3, "LOYALTY": 4 }
  attachedToIds: number[] | null
  attachmentIds: number[] | null
  type: string | null  // Full type line e.g. "Creature - Human Wizard"
  oracleText: string | null
}

export interface CombatDto {
  attackers: AttackerInfo[]
}

export interface AttackerInfo {
  cardId: number
  defendingPlayerId: number
  blockerCardIds: number[]
}

export interface SpellAbilityDto {
  id: number
  name: string | null
  description: string | null
  sourceCardId: number
  activatingPlayerId: number
}

export interface ZoneUpdateDto {
  playerId: number
  updatedZones: string[]
}

// WebSocket message envelopes
export type OutboundMessageType =
  | 'GAME_STATE' | 'ZONE_UPDATE' | 'PHASE_UPDATE' | 'TURN_UPDATE'
  | 'COMBAT_UPDATE' | 'STACK_UPDATE' | 'PROMPT_CHOICE' | 'PROMPT_CONFIRM'
  | 'PROMPT_AMOUNT' | 'SHOW_CARDS' | 'MESSAGE' | 'BUTTON_UPDATE'
  | 'GAME_OVER' | 'ERROR'

export interface OutboundMessage {
  type: OutboundMessageType
  inputId: string | null
  sequenceNumber: number
  payload: unknown
}

export type InboundMessageType =
  | 'CHOICE_RESPONSE' | 'CONFIRM_RESPONSE' | 'AMOUNT_RESPONSE'
  | 'START_GAME' | 'BUTTON_OK' | 'BUTTON_CANCEL'

export interface InboundMessage {
  type: InboundMessageType
  inputId: string | null
  payload: unknown
}

export interface ButtonPayload {
  playerId: number
  label1: string
  label2: string
  enable1: boolean
  enable2: boolean
  focus1: boolean
}

export interface PromptPayload {
  message: string
  min?: number
  max?: number
  choices?: string[]
  selected?: number[]
  title?: string
  options?: string[]
  isOptional?: boolean
  cardId?: number
  abilities?: string[]
  // Combat damage assignment
  attackerId?: number
  blockers?: CardDto[]
  damage?: number
  defenderId?: number
  overrideOrder?: boolean
  maySkip?: boolean
  // Amount assignment
  sourceCardId?: number
  targets?: Record<string, number>
  amount?: number
  atLeastOne?: boolean
  amountLabel?: string
  // Other flags
  defaultYes?: boolean
  yesButton?: string
  noButton?: string
  isNumeric?: boolean
  initialInput?: string
  defaultOption?: number
}
```

### PhaseType Values for Phase Strip
```typescript
// The engine sends these exact strings via PHASE_UPDATE
export const PHASE_TYPES = [
  'UNTAP', 'UPKEEP', 'DRAW',
  'MAIN1',
  'COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS',
  'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END',
  'MAIN2',
  'END_OF_TURN', 'CLEANUP'
] as const

// Simplified display groups for the phase strip
export const PHASE_STRIP_ITEMS = [
  { phases: ['UNTAP'], label: 'Untap' },
  { phases: ['UPKEEP'], label: 'Upkeep' },
  { phases: ['DRAW'], label: 'Draw' },
  { phases: ['MAIN1'], label: 'Main 1' },
  { phases: ['COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS',
             'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END'], label: 'Combat' },
  { phases: ['MAIN2'], label: 'Main 2' },
  { phases: ['END_OF_TURN', 'CLEANUP'], label: 'End' },
]
```

### Mana Color Keys from PlayerDto
```typescript
// PlayerDto.mana uses MagicColor.Constant values as keys
// From forge-core MagicColor.java:
export const MANA_COLORS = {
  White: { symbol: 'w', className: 'ms ms-w' },
  Blue:  { symbol: 'u', className: 'ms ms-u' },
  Black: { symbol: 'b', className: 'ms ms-b' },
  Red:   { symbol: 'r', className: 'ms ms-r' },
  Green: { symbol: 'g', className: 'ms ms-g' },
  Colorless: { symbol: 'c', className: 'ms ms-c' },
} as const
```

### Board Layout Skeleton
```typescript
// GameBoard.tsx -- overall layout structure
// Uses CSS Grid for the Arena-style layout
<div className="h-screen w-screen grid grid-rows-[auto_1fr_auto_1fr_auto_auto] grid-cols-[1fr_220px]">
  {/* Row 1: Opponent info bar */}
  <PlayerInfoBar player={opponent} className="col-span-2" />

  {/* Row 2: Opponent battlefield */}
  <BattlefieldZone playerId={opponent.id} flipped />

  {/* Row 3: Phase strip + center divider */}
  <PhaseStrip phase={phase} turn={turn} activePlayerId={activePlayerId} className="col-span-2" />

  {/* Row 4: Player battlefield */}
  <BattlefieldZone playerId={human.id} />

  {/* Row 5: Action bar */}
  <ActionBar className="col-span-2" />

  {/* Row 6: Player hand */}
  <HandZone playerId={human.id} className="col-span-2" />

  {/* Right sidebar: Stack (spans rows 2-4) */}
  <StackPanel className="row-start-2 row-span-3 col-start-2" />

  {/* Player info bar at very bottom */}
  <PlayerInfoBar player={human} className="col-span-2" />
</div>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Redux + Redux-Saga for async | Zustand + direct store updates | 2023+ | No boilerplate, no middleware for WebSocket handling |
| socket.io everywhere | Native WebSocket API | Ongoing | Forge uses raw WebSocket; native API is sufficient |
| CSS-in-JS (styled-components) | Tailwind CSS | Project standard | Already established in Phases 2-3 |
| React Context for global state | Zustand for cross-component state | 2022+ | Avoids provider hell and unnecessary re-renders |

**Deprecated/outdated:**
- `enableMapSet()` import from `immer` is at `immer` top level: `import { enableMapSet } from 'immer'` -- but recommendation is to use plain objects instead

## Open Questions

1. **Card images on the game board**
   - What we know: CardDto has `name` but no `setCode`/`collectorNumber`. The deck builder's CardImage uses Scryfall set+collector URLs.
   - What's unclear: Whether the engine's CardView has access to the PaperCard's set/collector info that could be added to CardDto.
   - Recommendation: Use Scryfall's name-based image API (`/cards/named?fuzzy=X&format=image`) for the game board. Less precise than set/collector but works with what CardDto provides. Cache responses. If image quality/specificity matters, extend CardDto in a future pass.

2. **How updateCards() and updateZones() coordinate**
   - What we know: The engine calls both when cards move. `updateCards()` sends fresh CardDto list. `updateZones()` sends which zones changed.
   - What's unclear: The exact ordering guarantee -- does updateCards always arrive before/after updateZones?
   - Recommendation: On ZONE_UPDATE, mark the affected zones as stale. On card updates (via ZONE_UPDATE with type "card_update" or GAME_STATE), merge new card data. Use cards' `zoneType` field as the source of truth for which zone they belong to, not the zone notification.

3. **Identifying the human player**
   - What we know: GameStateDto has players array with IDs, but no explicit "you are player X" field.
   - What's unclear: Whether the first player in the array is always the human, or if there's a separate identification mechanism.
   - Recommendation: The human player is the one created via `GamePlayerUtil.getGuiPlayer()` -- their name will be predictable. Store the human player ID when processing the first GAME_STATE. Could also be passed as part of the START_GAME response or derived from the BUTTON_UPDATE playerId (buttons are only sent for the human player).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None currently installed |
| Config file | None -- see Wave 0 |
| Quick run command | `npx vitest run --reporter=verbose` |
| Full suite command | `npx vitest run` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GAME-01 | All zones rendered from game state | unit | `npx vitest run src/stores/gameStore.test.ts -t "applyGameState"` | No -- Wave 0 |
| GAME-02 | Card tap/counter/attachment rendering | unit | `npx vitest run src/components/game/GameCard.test.ts` | No -- Wave 0 |
| GAME-03 | Phase indicator updates on PHASE_UPDATE | unit | `npx vitest run src/stores/gameStore.test.ts -t "phase"` | No -- Wave 0 |
| GAME-04 | Life totals display from PlayerDto | unit | `npx vitest run src/stores/gameStore.test.ts -t "life"` | No -- Wave 0 |
| GAME-05 | Mana pool from PlayerDto.mana | unit | `npx vitest run src/stores/gameStore.test.ts -t "mana"` | No -- Wave 0 |
| GAME-06 | Stack renders from STACK_UPDATE | unit | `npx vitest run src/stores/gameStore.test.ts -t "stack"` | No -- Wave 0 |
| GAME-07 | Prompt state set from PROMPT_CHOICE | unit | `npx vitest run src/stores/gameStore.test.ts -t "prompt"` | No -- Wave 0 |
| GAME-08 | Choice response with correct inputId | unit | `npx vitest run src/lib/gameWebSocket.test.ts -t "choice"` | No -- Wave 0 |
| GAME-09 | Combat state from COMBAT_UPDATE | unit | `npx vitest run src/stores/gameStore.test.ts -t "combat"` | No -- Wave 0 |
| GAME-10 | Hover preview reuses existing hook | manual-only | Visual verification -- hover interaction | N/A |
| GAME-11 | Cast spell sends BUTTON_OK | unit | `npx vitest run src/lib/gameWebSocket.test.ts -t "button"` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `npx vitest run --reporter=verbose`
- **Per wave merge:** `npx vitest run`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] Install vitest: `npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom`
- [ ] Create `vitest.config.ts` with jsdom environment
- [ ] `src/stores/gameStore.test.ts` -- covers GAME-01 through GAME-09 (store logic)
- [ ] `src/lib/gameWebSocket.test.ts` -- covers GAME-08, GAME-11 (message sending)

## Sources

### Primary (HIGH confidence)
- Direct code inspection of `WebGuiGame.java` -- all 20+ IGuiGame method implementations, message types, payload structures
- Direct code inspection of `WebInputBridge.java` -- sendAndWait/complete pattern with inputId
- Direct code inspection of `WebServer.java` -- WebSocket message routing, START_GAME handler, BUTTON_OK/CANCEL handling
- Direct code inspection of all DTOs: GameStateDto, CardDto, PlayerDto, CombatDto, SpellAbilityDto, ZoneUpdateDto
- Direct code inspection of `MessageType.java` -- complete enum of all message types
- Direct code inspection of `PhaseType.java` -- all phase enum values
- Direct code inspection of `ZoneType.java` -- all zone enum values
- Direct code inspection of frontend package.json, App.tsx, existing components
- npm registry: zustand 5.0.12, immer 11.1.4 (verified 2026-03-19)

### Secondary (MEDIUM confidence)
- Zustand + immer middleware pattern: well-documented in zustand GitHub README
- Scryfall name-based image API: documented at scryfall.com/docs/api

### Tertiary (LOW confidence)
- Whether CardView exposes set/collector information that could be added to CardDto (needs investigation in forge-game source)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- zustand + immer are minimal additions; everything else already installed
- Architecture: HIGH -- protocol fully inspected, all message types/payloads documented from source
- Pitfalls: HIGH -- identified from direct code inspection of the dual input system, DTO shapes, and React 19 behavior

**Research date:** 2026-03-19
**Valid until:** 2026-04-19 (stable domain, no library upgrades expected)
