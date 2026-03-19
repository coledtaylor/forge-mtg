export interface DeckSummary {
  name: string
  cardCount: number
  colors: string[]
  path: string
}

export interface DeckCardEntry {
  name: string
  quantity: number
  setCode: string
  collectorNumber: string
}

export interface DeckDetail {
  name: string
  main: DeckCardEntry[]
  sideboard: DeckCardEntry[]
  commander: DeckCardEntry[]
}
