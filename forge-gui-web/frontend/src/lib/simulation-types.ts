export interface SimulationConfig {
  deckName: string
  gameCount: 10 | 50 | 100 | 500
  aiProfile?: 'Reckless' | 'Default'
  opponentDeckNames?: string[]
}

export interface MatchupStats {
  games: number
  wins: number
  winRate: number
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
  eloRating: number
  playstyle: Record<string, number> // aggro, midrange, control, combo -> 0.0-1.0
  cancelled: boolean
}

export interface SimulationHistoryEntry {
  id: string
  timestamp: string
  gamesCompleted: number
  gamesTotal: number
  winRate: number
  eloRating: number
}
