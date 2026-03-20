import { create } from 'zustand'
import { immer } from 'zustand/middleware/immer'
import type {
  GameStateDto,
  PlayerDto,
  CardDto,
  SpellAbilityDto,
  CombatDto,
  ZoneUpdateDto,
  ButtonPayload,
  PromptPayload,
  OutboundMessageType,
  GameLogEntry,
} from '../lib/gameTypes'

export interface PromptState {
  type: OutboundMessageType
  inputId: string
  payload: PromptPayload
}

export interface TargetingState {
  validTargetIds: number[]
  selectedTargetIds: number[]
  min: number
  max: number
  promptInputId: string
}

interface GameState {
  players: Record<number, PlayerDto>
  cards: Record<number, CardDto>
  stack: SpellAbilityDto[]
  combat: CombatDto | null
  phase: string | null
  turn: number
  activePlayerId: number
  humanPlayerId: number | null
  prompt: PromptState | null
  buttons: ButtonPayload | null
  gameLog: GameLogEntry[]
  hasPriority: boolean
  targetingState: TargetingState | null
  gameOver: { winner: string; message: string } | null
  connected: boolean
  error: string | null
}

interface GameActions {
  applyGameState: (dto: GameStateDto) => void
  applyZoneUpdate: (updates: ZoneUpdateDto[]) => void
  applyPhaseUpdate: (phase: string) => void
  applyTurnUpdate: (payload: { turn: number; activePlayerId: number }) => void
  applyCombatUpdate: (combat: CombatDto | null) => void
  applyStackUpdate: (stack: SpellAbilityDto[]) => void
  applyButtonUpdate: (buttons: ButtonPayload) => void
  setPrompt: (prompt: PromptState | null) => void
  setGameOver: (payload: { winner: string; message: string }) => void
  setConnected: (connected: boolean) => void
  addLogEntries: (entries: GameLogEntry[]) => void
  clearGameLog: () => void
  setTargetingState: (state: TargetingState | null) => void
  toggleTargetSelection: (cardId: number) => void
  clearButtons: () => void
  setError: (error: string | null) => void
  reset: () => void
}

const initialState: GameState = {
  players: {},
  cards: {},
  stack: [],
  combat: null,
  phase: null,
  turn: 0,
  activePlayerId: -1,
  humanPlayerId: null,
  prompt: null,
  buttons: null,
  gameLog: [],
  hasPriority: false,
  targetingState: null,
  gameOver: null,
  connected: false,
  error: null,
}

export const useGameStore = create<GameState & GameActions>()(
  immer((set) => ({
    ...initialState,

    applyGameState: (dto: GameStateDto) =>
      set((state) => {
        // Full snapshot -- replace everything
        const players: Record<number, PlayerDto> = {}
        for (const player of dto.players) {
          players[player.id] = player
        }
        state.players = players

        const cards: Record<number, CardDto> = {}
        for (const card of dto.cards) {
          cards[card.id] = card
        }
        state.cards = cards

        state.stack = dto.stack
        state.combat = dto.combat
        state.phase = dto.phase
        state.turn = dto.turn
        state.activePlayerId = dto.activePlayerId
      }),

    applyZoneUpdate: (updates: ZoneUpdateDto[]) =>
      set((state) => {
        // ZoneUpdateDto is notification-only (Pitfall 1).
        // Re-derive zone membership from cards' zoneType field.
        for (const update of updates) {
          const player = state.players[update.playerId]
          if (!player) continue

          for (const zoneName of update.updatedZones) {
            // Rebuild this zone's card ID list from the cards collection
            const cardIds: number[] = []
            for (const cardId in state.cards) {
              const card = state.cards[cardId]
              if (card.zoneType === zoneName && card.controllerId === update.playerId) {
                cardIds.push(card.id)
              }
            }
            player.zones[zoneName] = cardIds
          }
        }
      }),

    applyPhaseUpdate: (phase: string) =>
      set((state) => {
        state.phase = phase
      }),

    applyTurnUpdate: (payload: { turn: number; activePlayerId: number }) =>
      set((state) => {
        state.turn = payload.turn
        state.activePlayerId = payload.activePlayerId
      }),

    applyCombatUpdate: (combat: CombatDto | null) =>
      set((state) => {
        state.combat = combat
      }),

    applyStackUpdate: (stack: SpellAbilityDto[]) =>
      set((state) => {
        state.stack = stack
      }),

    applyButtonUpdate: (buttons: ButtonPayload) =>
      set((state) => {
        state.buttons = buttons
        state.hasPriority = true // BUTTON_UPDATE means player has priority
        // First BUTTON_UPDATE identifies the human player
        if (state.humanPlayerId === null) {
          state.humanPlayerId = buttons.playerId
        }
      }),

    setPrompt: (prompt: PromptState | null) =>
      set((state) => {
        state.prompt = prompt
      }),

    setGameOver: (payload: { winner: string; message: string }) =>
      set((state) => {
        state.gameOver = payload
      }),

    setConnected: (connected: boolean) =>
      set((state) => {
        state.connected = connected
      }),

    addLogEntries: (entries: GameLogEntry[]) =>
      set((state) => {
        state.gameLog.push(...entries)
      }),

    clearGameLog: () =>
      set((state) => {
        state.gameLog = []
      }),

    setTargetingState: (targetingState: TargetingState | null) =>
      set((state) => {
        state.targetingState = targetingState
      }),

    toggleTargetSelection: (cardId: number) =>
      set((state) => {
        if (!state.targetingState) return
        const idx = state.targetingState.selectedTargetIds.indexOf(cardId)
        if (idx >= 0) {
          state.targetingState.selectedTargetIds.splice(idx, 1)
        } else if (state.targetingState.selectedTargetIds.length < state.targetingState.max) {
          state.targetingState.selectedTargetIds.push(cardId)
        }
      }),

    clearButtons: () =>
      set((state) => {
        state.buttons = null
        state.hasPriority = false
      }),

    setError: (error: string | null) =>
      set((state) => {
        state.error = error
      }),

    reset: () => set(() => ({ ...initialState })),
  }))
)
