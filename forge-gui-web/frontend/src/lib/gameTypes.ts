// TypeScript types matching Java DTOs from forge-gui-web backend

export interface GameStateDto {
  players: PlayerDto[]
  cards: CardDto[]
  stack: SpellAbilityDto[]
  combat: CombatDto | null
  phase: string | null
  turn: number
  activePlayerId: number
}

export interface PlayerDto {
  id: number
  name: string
  life: number
  poisonCounters: number
  mana: Record<string, number>
  zones: Record<string, number[]>
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
  zoneType: string | null
  tapped: boolean
  counters: Record<string, number> | null
  attachedToIds: number[] | null
  attachmentIds: number[] | null
  type: string | null
  oracleText: string | null
  setCode: string | null
  collectorNumber: string | null
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
  | 'GAME_STATE'
  | 'ZONE_UPDATE'
  | 'PHASE_UPDATE'
  | 'TURN_UPDATE'
  | 'COMBAT_UPDATE'
  | 'STACK_UPDATE'
  | 'PROMPT_CHOICE'
  | 'PROMPT_CONFIRM'
  | 'PROMPT_AMOUNT'
  | 'SHOW_CARDS'
  | 'MESSAGE'
  | 'BUTTON_UPDATE'
  | 'GAME_OVER'
  | 'ERROR'

export interface OutboundMessage {
  type: OutboundMessageType
  inputId: string | null
  sequenceNumber: number
  payload: unknown
}

export type InboundMessageType =
  | 'CHOICE_RESPONSE'
  | 'CONFIRM_RESPONSE'
  | 'AMOUNT_RESPONSE'
  | 'START_GAME'
  | 'BUTTON_OK'
  | 'BUTTON_CANCEL'
  | 'SELECT_CARD'

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
  attackerId?: number
  blockers?: CardDto[]
  damage?: number
  defenderId?: number
  overrideOrder?: boolean
  maySkip?: boolean
  sourceCardId?: number
  targets?: Record<string, number>
  amount?: number
  atLeastOne?: boolean
  amountLabel?: string
  defaultYes?: boolean
  yesButton?: string
  noButton?: string
  isNumeric?: boolean
  initialInput?: string
  defaultOption?: number
}

// Phase types sent by the engine via PHASE_UPDATE

export const PHASE_TYPES = [
  'UNTAP', 'UPKEEP', 'DRAW',
  'MAIN1',
  'COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS',
  'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END',
  'MAIN2',
  'END_OF_TURN', 'CLEANUP',
] as const

export const PHASE_STRIP_ITEMS = [
  { phases: ['UNTAP'], label: 'Untap' },
  { phases: ['UPKEEP'], label: 'Upkeep' },
  { phases: ['DRAW'], label: 'Draw' },
  { phases: ['MAIN1'], label: 'Main 1' },
  {
    phases: [
      'COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS',
      'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END',
    ],
    label: 'Combat',
  },
  { phases: ['MAIN2'], label: 'Main 2' },
  { phases: ['END_OF_TURN', 'CLEANUP'], label: 'End' },
] as const

export const MANA_COLORS = {
  White: { symbol: 'w', className: 'ms ms-w' },
  Blue: { symbol: 'u', className: 'ms ms-u' },
  Black: { symbol: 'b', className: 'ms ms-b' },
  Red: { symbol: 'r', className: 'ms ms-r' },
  Green: { symbol: 'g', className: 'ms ms-g' },
  Colorless: { symbol: 'c', className: 'ms ms-c' },
} as const
