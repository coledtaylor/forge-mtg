import type { MatchupStats } from '@/lib/simulation-types'

interface MatchupsTabProps {
  matchups: Record<string, MatchupStats>
  winRateOnPlay: number
  winRateOnDraw: number
}

function winRateColor(rate: number): string {
  if (rate >= 60) return 'text-green-500'
  if (rate >= 40) return 'text-yellow-500'
  return 'text-red-500'
}

function barColor(rate: number): string {
  if (rate >= 60) return 'bg-green-500'
  if (rate >= 40) return 'bg-yellow-500'
  return 'bg-red-500'
}

export function MatchupsTab({ matchups, winRateOnPlay, winRateOnDraw }: MatchupsTabProps) {
  const entries = Object.entries(matchups)

  if (entries.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">No matchup data available</p>
    )
  }

  // Sort by win rate descending
  const sorted = entries
    .map(([opponent, stats]) => ({ opponent, ...stats, losses: stats.games - stats.wins }))
    .sort((a, b) => b.winRate - a.winRate)

  const totalGames = sorted.reduce((s, r) => s + r.games, 0)
  const totalWins = sorted.reduce((s, r) => s + r.wins, 0)
  const totalLosses = sorted.reduce((s, r) => s + r.losses, 0)
  const overallWinRate = totalGames > 0 ? (totalWins / totalGames) * 100 : 0

  const best = sorted[0]
  const worst = sorted[sorted.length - 1]

  return (
    <div className="space-y-4">
      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="pb-2 pr-4 font-medium">Opponent</th>
              <th className="pb-2 pr-4 font-medium text-right">Games</th>
              <th className="pb-2 pr-4 font-medium text-right">Wins</th>
              <th className="pb-2 pr-4 font-medium text-right">Losses</th>
              <th className="pb-2 pr-4 font-medium text-right">Win Rate</th>
              <th className="pb-2 font-medium w-32"></th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((row) => (
              <tr key={row.opponent} className="border-b border-border/50">
                <td className="py-2 pr-4 font-medium">{row.opponent}</td>
                <td className="py-2 pr-4 text-right tabular-nums">{row.games}</td>
                <td className="py-2 pr-4 text-right tabular-nums">{row.wins}</td>
                <td className="py-2 pr-4 text-right tabular-nums">{row.losses}</td>
                <td className={`py-2 pr-4 text-right tabular-nums font-medium ${winRateColor(row.winRate)}`}>
                  {row.winRate.toFixed(1)}%
                </td>
                <td className="py-2">
                  <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className={`h-full rounded-full transition-all ${barColor(row.winRate)}`}
                      style={{ width: `${Math.max(row.winRate, 2)}%` }}
                    />
                  </div>
                </td>
              </tr>
            ))}

            {/* Total row */}
            <tr className="font-medium">
              <td className="pt-3 pr-4">Total</td>
              <td className="pt-3 pr-4 text-right tabular-nums">{totalGames}</td>
              <td className="pt-3 pr-4 text-right tabular-nums">{totalWins}</td>
              <td className="pt-3 pr-4 text-right tabular-nums">{totalLosses}</td>
              <td className={`pt-3 pr-4 text-right tabular-nums ${winRateColor(overallWinRate)}`}>
                {overallWinRate.toFixed(1)}%
              </td>
              <td className="pt-3">
                <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
                  <div
                    className={`h-full rounded-full ${barColor(overallWinRate)}`}
                    style={{ width: `${Math.max(overallWinRate, 2)}%` }}
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Summary */}
      <div className="space-y-1 text-sm text-muted-foreground">
        <p>Best matchup: <span className="text-foreground font-medium">{best.opponent}</span> ({best.winRate.toFixed(1)}%)</p>
        <p>Worst matchup: <span className="text-foreground font-medium">{worst.opponent}</span> ({worst.winRate.toFixed(1)}%)</p>
        <p>Play vs Draw: <span className="text-foreground font-medium">{winRateOnPlay.toFixed(1)}%</span> on play, <span className="text-foreground font-medium">{winRateOnDraw.toFixed(1)}%</span> on draw</p>
      </div>
    </div>
  )
}
