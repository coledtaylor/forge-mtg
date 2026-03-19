export interface CardSearchResult {
  name: string
  manaCost: string
  typeLine: string
  oracleText: string
  power: number
  toughness: number
  colors: string[]
  cmc: number
  setCode: string
  collectorNumber: string
}

export interface CardSearchResponse {
  cards: CardSearchResult[]
  total: number
  page: number
  limit: number
  totalPages: number
}

export interface CardSearchParams {
  q?: string
  color?: string
  type?: string
  cmc?: number
  cmcOp?: 'eq' | 'lt' | 'gt' | 'lte' | 'gte'
  format?: string
  page?: number
  limit?: number
}
