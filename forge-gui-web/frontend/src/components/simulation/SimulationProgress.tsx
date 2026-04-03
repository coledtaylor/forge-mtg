import { Button } from '../ui/button'
import { tierColor } from '../../lib/wilson'
import { XCircle, Loader2 } from 'lucide-react'
import type { SimulationProgress as SimulationProgressType } from '../../lib/simulation-types'

interface SimulationProgressProps {
  progress: SimulationProgressType
  onCancel: () => void
  isCancelling?: boolean
}

export function SimulationProgress({ progress, onCancel, isCancelling }: SimulationProgressProps) {
  const {
    status,
    gamesCompleted,
    gamesTotal,
    wins,
    losses,
    draws,
    winRate,
    powerScore,
    tier,
    avgTurns,
    matchups,
  } = progress

  const percentage = gamesTotal > 0 ? Math.round((gamesCompleted / gamesTotal) * 100) : 0
  const isFinished = status === 'complete' || status === 'cancelled'

  const matchupEntries = Object.entries(matchups || {}).sort(
    (a, b) => b[1].winRate - a[1].winRate
  )

  return (
    <div className="flex flex-col gap-4 p-4">
      {!isFinished && (
        <div>
          <div className="flex items-center justify-between text-sm mb-1">
            <span className="text-muted-foreground">
              {gamesCompleted}/{gamesTotal} games complete
            </span>
            <span className="font-medium">{percentage}%</span>
          </div>
          <div className="h-2 rounded-full bg-muted overflow-hidden">
            <div
              className="h-full rounded-full bg-primary transition-[width] duration-300 ease-out"
              style={{ width: `${percentage}%` }}
            />
          </div>
        </div>
      )}

      {isFinished && (
        <div className="rounded-md border border-border bg-muted/30 px-3 py-2 text-sm">
          {status === 'cancelled' ? (
            <span className="text-muted-foreground">
              Cancelled after {gamesCompleted}/{gamesTotal} games
            </span>
          ) : (
            <span className="text-muted-foreground">
              Simulation complete -- {gamesCompleted} games played
            </span>
          )}
        </div>
      )}

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
          <div className="text-2xl font-bold">{winRate.toFixed(1)}%</div>
          <div className="text-xs text-muted-foreground">Win Rate</div>
        </div>
        <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
          <div className={`text-2xl font-bold ${tierColor(tier)}`}>{powerScore}</div>
          <div className={`text-xs font-medium ${tierColor(tier)}`}>{tier}</div>
        </div>
        <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
          <div className="text-lg font-medium">
            {wins}W / {losses}L / {draws}D
          </div>
          <div className="text-xs text-muted-foreground">Record</div>
        </div>
        <div className="rounded-md border border-border bg-muted/30 p-3 text-center">
          <div className="text-lg font-medium">{avgTurns > 0 ? avgTurns.toFixed(1) : '--'}</div>
          <div className="text-xs text-muted-foreground">Avg Turns</div>
        </div>
      </div>

      {matchupEntries.length > 0 && (
        <div>
          <h3 className="text-sm font-medium text-muted-foreground mb-2">Matchups</h3>
          <div className="rounded-md border border-border overflow-hidden">
            <table className="w-full text-sm">
              <tbody>
                {matchupEntries.map(([opponent, stats]) => (
                  <tr key={opponent} className="border-b border-border last:border-0">
                    <td className="px-3 py-1.5 truncate max-w-[180px]">{opponent}</td>
                    <td className="px-3 py-1.5 text-right text-muted-foreground">
                      {stats.games}g
                    </td>
                    <td className="px-3 py-1.5 text-right font-medium w-16">
                      {stats.winRate.toFixed(0)}%
                    </td>
                    <td className={`px-3 py-1.5 text-right font-medium w-12 ${tierColor(stats.tier)}`}>
                      {stats.powerScore}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!isFinished && (
        isCancelling ? (
          <div className="flex items-center justify-center gap-2 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            <Loader2 className="size-4 animate-spin" />
            Cancelling after current game...
          </div>
        ) : (
          <Button variant="destructive" onClick={onCancel} className="w-full gap-2">
            <XCircle className="size-4" />
            Cancel Simulation
          </Button>
        )
      )}
    </div>
  )
}
