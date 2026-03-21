const K = 32
const START_ELO = 1500

/**
 * Compute an Elo rating from a series of game results.
 * Each result includes the opponent's Elo and whether the player won.
 * Mirrors the backend EloCalculator for client-side display.
 */
export function computeElo(results: { opponentElo: number; won: boolean }[]): number {
  let elo = START_ELO
  for (const { opponentElo, won } of results) {
    const expected = 1 / (1 + Math.pow(10, (opponentElo - elo) / 400))
    const actual = won ? 1 : 0
    elo = Math.round(elo + K * (actual - expected))
  }
  return elo
}

/**
 * Map an Elo rating to a human-readable tier label.
 */
export function eloTier(elo: number): string {
  if (elo < 1300) return 'Weak'
  if (elo < 1450) return 'Below Average'
  if (elo < 1550) return 'Average'
  if (elo < 1700) return 'Above Average'
  if (elo < 1850) return 'Strong'
  return 'Elite'
}
