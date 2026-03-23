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

  const hasWins = data.avgTurns > 0
  const avgTurnsDisplay = hasWins ? data.avgTurns.toFixed(1) : 'N/A'
  const fastestDisplay = data.fastestWin > 0 ? String(data.fastestWin) : 'N/A'
  const slowestDisplay = data.slowestWin > 0 ? String(data.slowestWin) : 'N/A'
  const firstSpellDisplay = data.avgFirstThreatTurn > 0 ? `Turn ${data.avgFirstThreatTurn.toFixed(1)}` : 'N/A'

  const record = `${data.wins}-${data.losses}-${data.draws}`
  const stalemateSuffix = data.stalemates > 0 ? ` (${data.stalemates} stalemate${data.stalemates !== 1 ? 's' : ''})` : ''

  return (
    <div className="space-y-4">
      {data.cancelled && (
        <div className="rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-4 py-2 text-sm text-yellow-600 dark:text-yellow-400">
          Results from {data.gamesCompleted}/{data.gamesTotal} games (cancelled early)
        </div>
      )}

      <p className="text-sm text-muted-foreground">
        {data.gamesCompleted} games against {matchupCount} opponent{matchupCount !== 1 ? 's' : ''}
        {data.stalemates > 0 && (
          <span className="text-yellow-500"> ({data.stalemates} stalemate{data.stalemates !== 1 ? 's' : ''} excluded)</span>
        )}
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
            detail={`${record}${stalemateSuffix}`}
            tooltip="Percentage of games won (stalemates excluded from calculation)"
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
            value={avgTurnsDisplay}
            detail={`Fastest: ${fastestDisplay}, Slowest: ${slowestDisplay}`}
            tooltip="Average number of turns across winning games only"
          />
          <StatCard
            label="Mulligans"
            value={`${data.keepRate.toFixed(0)}% keep`}
            detail={`Avg ${data.avgMulligans.toFixed(1)} mulligans`}
            tooltip="AI keeps hands with 2+ lands (not all lands), prefers lands ≈ half the hand with castable spells. Auto-mulligans 0-1 land or all-land hands. Won't mulligan below the profile threshold (Reckless: 3, Default: 4)."
          />
          <StatCard
            label="First Spell Cast"
            value={firstSpellDisplay}
            tooltip="Average turn when the deck first puts a spell on the stack"
          />
        </div>
      </div>
    </div>
  )
}
