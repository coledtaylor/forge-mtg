import type { SimulationProgress } from '@/lib/simulation-types'
import { eloTier } from '@/lib/elo'
import { PlaystyleRadar } from './PlaystyleRadar'

interface OverviewTabProps {
  data: SimulationProgress
}

function eloColor(elo: number): string {
  if (elo < 1400) return 'text-red-500'
  if (elo < 1550) return 'text-yellow-500'
  return 'text-green-500'
}

interface StatCardProps {
  label: string
  value: string
  detail?: string
  tooltip?: string
}

function StatCard({ label, value, detail, tooltip }: StatCardProps) {
  return (
    <div className="rounded-lg border border-border p-4" title={tooltip}>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
      {detail && <p className="text-xs text-muted-foreground mt-1">{detail}</p>}
    </div>
  )
}

export function OverviewTab({ data }: OverviewTabProps) {
  const tier = eloTier(data.eloRating)
  const matchupCount = Object.keys(data.matchups).length

  return (
    <div className="space-y-4">
      {data.cancelled && (
        <div className="rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-4 py-2 text-sm text-yellow-600 dark:text-yellow-400">
          Results from {data.gamesCompleted}/{data.gamesTotal} games (cancelled early)
        </div>
      )}

      <p className="text-sm text-muted-foreground">
        {data.gamesCompleted} games against {matchupCount} opponent{matchupCount !== 1 ? 's' : ''}
      </p>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Left column: Elo + Radar */}
        <div className="flex flex-col items-center gap-4">
          <div className="text-center" title="Estimated Elo rating based on simulation results against the gauntlet">
            <p className={`text-5xl font-bold tabular-nums ${eloColor(data.eloRating)}`}>
              {data.eloRating}
            </p>
            <p className="text-sm text-muted-foreground mt-1">{tier}</p>
          </div>
          <PlaystyleRadar scores={data.playstyle} className="w-52 h-52" />
        </div>

        {/* Right column: Stat cards */}
        <div className="grid grid-cols-2 gap-3">
          <StatCard
            label="Win Rate"
            value={`${data.winRate.toFixed(1)}%`}
            detail={`${data.wins}-${data.losses}-${data.draws}`}
            tooltip="Percentage of games won across all matchups"
          />
          <StatCard
            label="Win Rate Going First"
            value={`${data.winRateOnPlay.toFixed(1)}%`}
            tooltip="Win rate when your deck goes first (on the play)"
          />
          <StatCard
            label="Win Rate Going Second"
            value={`${data.winRateOnDraw.toFixed(1)}%`}
            tooltip="Win rate when your deck goes second (on the draw)"
          />
          <StatCard
            label="Avg Turns to Win"
            value={`${data.avgTurns.toFixed(1)}`}
            detail={`Fastest: ${data.fastestWin}, Slowest: ${data.slowestWin}`}
            tooltip="Average game length in turns across all games (wins and losses)"
          />
          <StatCard
            label="Mulligans"
            value={`${data.keepRate.toFixed(0)}% keep`}
            detail={`Avg ${data.avgMulligans.toFixed(1)} mulligans`}
            tooltip="Percentage of games where the opening 7 cards were kept without mulliganing"
          />
          <StatCard
            label="First Spell Cast"
            value={`Turn ${data.avgFirstThreatTurn.toFixed(1)}`}
            tooltip="Average turn when your deck first casts a spell"
          />
        </div>
      </div>
    </div>
  )
}
