export interface DeckSummary {
  name: string
  cardCount: number
  colors: string[]
  path: string
  format: string
}

export interface DeckCardEntry {
  name: string
  quantity: number
  setCode: string
  collectorNumber: string
  manaCost: string
  typeLine: string
  cmc: number
  colors: string[]
}

export interface DeckDetail {
  name: string
  main: DeckCardEntry[]
  sideboard: DeckCardEntry[]
  commander: DeckCardEntry[]
}

export interface UpdateDeckPayload {
  main: Record<string, number>
  sideboard: Record<string, number>
  commander: Record<string, number>
}

export interface CreateDeckPayload {
  name: string
  format: string
}

export interface ValidationResult {
  legal: boolean
  illegalCards: { name: string; section: string; reason: string }[]
  conformanceProblem: string
}
