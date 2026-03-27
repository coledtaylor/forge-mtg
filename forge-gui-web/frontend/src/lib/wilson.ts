const Z = 1.96
const Z2 = Z * Z

/**
 * Compute the Wilson score interval for a win/loss record.
 * Returns a lower-bound Power Score (0-100) with tier label and confidence bounds.
 * Mirrors the backend WilsonCalculator.
 */
export function computeWilson(
  wins: number,
  total: number
): { powerScore: number; confidenceLower: number; confidenceUpper: number; tier: string } {
  if (total === 0) {
    return { powerScore: 0, confidenceLower: 0, confidenceUpper: 0, tier: 'D Weak' }
  }

  const p = wins / total
  const n = total

  const center = p + Z2 / (2 * n)
  const margin = Z * Math.sqrt((p * (1 - p)) / n + Z2 / (4 * n * n))
  const denominator = 1 + Z2 / n

  const lower = Math.max(0, (center - margin) / denominator)
  const upper = Math.min(1, (center + margin) / denominator)

  const powerScore = Math.round(lower * 100)
  const tier = tierFor(powerScore)

  return { powerScore, confidenceLower: lower, confidenceUpper: upper, tier }
}

/**
 * Map a power score (0-100) to a tier label string.
 */
function tierFor(powerScore: number): string {
  if (powerScore >= 75) return 'S+ Elite'
  if (powerScore >= 65) return 'S Strong'
  if (powerScore >= 55) return 'A Above Average'
  if (powerScore >= 45) return 'B Average'
  if (powerScore >= 30) return 'C Below Average'
  return 'D Weak'
}

/**
 * Return a Tailwind text color class for a tier label.
 */
export function tierColor(tier: string): string {
  if (tier === 'S+ Elite') return 'text-purple-500'
  if (tier === 'S Strong') return 'text-green-500'
  if (tier === 'A Above Average') return 'text-blue-500'
  if (tier === 'B Average') return 'text-yellow-500'
  if (tier === 'C Below Average') return 'text-orange-500'
  return 'text-red-500'
}

/**
 * Return a Tailwind text color class for a numeric power score (0-100).
 */
export function powerScoreColor(score: number): string {
  if (score >= 75) return 'text-purple-500'
  if (score >= 65) return 'text-green-500'
  if (score >= 55) return 'text-blue-500'
  if (score >= 45) return 'text-yellow-500'
  if (score >= 30) return 'text-orange-500'
  return 'text-red-500'
}
