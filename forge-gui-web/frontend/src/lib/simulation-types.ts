export interface ManaProfile {
  landCount: number
  deckSize: number
  avgCmc: number
  recommendedLands: number
  keyTurn: number
  landsNeededByKeyTurn: number
  landExcessThreshold: number
  screwProbability: number
  floodProbability: number
  archetype: string
}

export interface SimulationConfig {
  deckName: string
  gameCount: 10 | 50 | 100 | 500
  aiProfile?: 'auto' | 'Reckless' | 'Default' | 'Cautious' | 'Experimental' | 'Aggro' | 'Burn' | 'Midrange' | 'Control' | 'Combo'
  opponentDeckNames?: string[]
  parallelGames?: number
}

export interface SystemInfo {
  availableProcessors: number
  safeMax: number
  defaultParallelGames: number
}

export interface MatchupStats {
  games: number
  wins: number
  winRate: number
  powerScore: number
  confidenceLower: number
  confidenceUpper: number
  tier: string
}

export interface CardPerformance {
  gamesDrawn: number
  winRateWhenDrawn: number
  deadCardRate: number
}

export interface SimulationProgress {
  status: 'running' | 'complete' | 'cancelled'
  gamesCompleted: number
  gamesTotal: number
  totalGames: number
  wins: number
  losses: number
  stalemates: number
  draws: number
  winRate: number
  winRateOnPlay: number
  winRateOnDraw: number
  matchups: Record<string, MatchupStats>
  avgTurns: number
  fastestWin: number
  slowestWin: number
  avgFirstThreatTurn: number
  keepRate: number
  avgMulligans: number
  winRateAfterMulligan: number
  avgThirdLandTurn: number // average turn for 3rd land drop (-1 if no data)
  avgFourthLandTurn: number // average turn for 4th land drop (-1 if no data)
  manaScrew: number
  manaFlood: number
  avgCardsDrawn: number
  avgEmptyHandTurns: number
  avgLifeAtWin: number
  avgLifeAtLoss: number
  cardPerformance: Record<string, CardPerformance>
  powerScore: number
  confidenceLower: number
  confidenceUpper: number
  tier: string
  playstyle: Record<string, number> // aggro, midrange, control, combo -> 0.0-1.0
  cancelled: boolean
  manaProfile?: ManaProfile
}

export interface SimulationHistoryEntry {
  id: string
  timestamp: string
  gamesCompleted: number
  gamesTotal: number
  winRate: number
  powerScore: number
  tier: string
}

export interface GameLogEntry {
  turn: number
  type: string
  message: string
}

export interface GameLogSummary {
  id: string
  timestamp: string
  source: 'simulation' | 'match'
  simulationId?: string
  playerDeck: string
  opponentDeck: string
  winner: string
  turns: number
  onPlay: boolean
}

export interface GameLogDetail extends GameLogSummary {
  entries: GameLogEntry[]
}
