import type { SimulationProgress } from '@/lib/simulation-types'

interface ManaTabProps {
  data: SimulationProgress
}

function healthIndicator(screw: number, flood: number): { icon: string; color: string; label: string } {
  if (screw > 0.2 || flood > 0.2) return { icon: '\u26a0', color: 'text-red-500', label: 'Poor' }
  if (screw > 0.1 || flood > 0.1) return { icon: '\u26a0', color: 'text-yellow-500', label: 'Fair' }
  return { icon: '\u2713', color: 'text-green-500', label: 'Good' }
}

function landDropColor(avgTurn: number, target: number): string {
  if (avgTurn === -1) return 'text-muted-foreground'
  if (avgTurn <= target) return 'text-green-500'
  if (avgTurn <= target + 1.5) return 'text-yellow-500'
  return 'text-red-500'
}

function landDropInterpretation(third: number, fourth: number): { text: string; color: string } {
  if (third === -1 && fourth === -1) return { text: 'No data', color: 'text-muted-foreground' }
  const thirdBad = third > 5
  const fourthBad = fourth > 6
  if (thirdBad || fourthBad) return { text: 'Significantly behind -- consider more lands or fixing', color: 'text-red-500' }
  const thirdBehind = third > 3.5
  const fourthBehind = fourth > 4.5
  if (thirdBehind || fourthBehind) return { text: 'Slightly behind', color: 'text-yellow-500' }
  return { text: 'On curve', color: 'text-green-500' }
}

interface ResourceCardProps {
  label: string
  value: string
  alert?: boolean
}

function ResourceCard({ label, value, alert }: ResourceCardProps) {
  return (
    <div className="rounded-lg border border-border p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${alert ? 'text-red-500' : ''}`}>{value}</p>
    </div>
  )
}

export function ManaTab({ data }: ManaTabProps) {
  const health = healthIndicator(data.manaScrew, data.manaFlood)
  const screwPct = (data.manaScrew * 100).toFixed(1)
  const floodPct = (data.manaFlood * 100).toFixed(1)

  const thirdLand = data.avgThirdLandTurn
  const fourthLand = data.avgFourthLandTurn
  const landInterp = landDropInterpretation(thirdLand, fourthLand)

  return (
    <div className="space-y-6">
      {/* Mana Consistency */}
      <section className="space-y-3">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold">Mana Consistency</h3>
          <span className={`text-sm font-medium ${health.color}`}>
            {health.icon} {health.label}
          </span>
        </div>

        <div className="space-y-2">
          {/* Mana Screw */}
          <div className="space-y-1">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Mana Screw Rate</span>
              <span className="tabular-nums font-medium">{screwPct}%</span>
            </div>
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-red-500 transition-all"
                style={{ width: `${Math.min(data.manaScrew * 100, 100)}%` }}
              />
            </div>
          </div>

          {/* Mana Flood */}
          <div className="space-y-1">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Mana Flood Rate</span>
              <span className="tabular-nums font-medium">{floodPct}%</span>
            </div>
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-blue-500 transition-all"
                style={{ width: `${Math.min(data.manaFlood * 100, 100)}%` }}
              />
            </div>
          </div>

          {/* Interpretation */}
          <div className="text-xs text-muted-foreground space-y-0.5">
            {data.manaScrew > 0.2 && (
              <p className="text-red-400">Consider adding more lands</p>
            )}
            {data.manaFlood > 0.15 && (
              <p className="text-blue-400">Consider cutting some lands</p>
            )}
          </div>
        </div>
      </section>

      {/* Land Drop Timing */}
      <section className="space-y-3">
        <h3 className="text-sm font-semibold">Land Drop Timing</h3>

        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-lg border border-border p-4">
            <p className="text-sm text-muted-foreground">Avg 3rd Land</p>
            <p className={`text-2xl font-bold mt-1 tabular-nums ${landDropColor(thirdLand, 3.5)}`}>
              {thirdLand === -1 ? 'N/A' : `Turn ${thirdLand.toFixed(1)}`}
            </p>
          </div>
          <div className="rounded-lg border border-border p-4">
            <p className="text-sm text-muted-foreground">Avg 4th Land</p>
            <p className={`text-2xl font-bold mt-1 tabular-nums ${landDropColor(fourthLand, 4.5)}`}>
              {fourthLand === -1 ? 'N/A' : `Turn ${fourthLand.toFixed(1)}`}
            </p>
          </div>
        </div>

        <p className={`text-xs ${landInterp.color}`}>{landInterp.text}</p>
      </section>

      {/* Resources */}
      <section className="space-y-3">
        <h3 className="text-sm font-semibold">Resources</h3>

        <div className="grid grid-cols-2 gap-3">
          <ResourceCard
            label="Avg Cards Drawn"
            value={`${data.avgCardsDrawn.toFixed(1)}`}
          />
          <ResourceCard
            label="Empty Hand Turns"
            value={`${data.avgEmptyHandTurns.toFixed(1)}`}
            alert={data.avgEmptyHandTurns > 2}
          />
          <ResourceCard
            label="Avg Life at Win"
            value={`${data.avgLifeAtWin.toFixed(1)}`}
          />
          <ResourceCard
            label="Avg Life at Loss"
            value={`${data.avgLifeAtLoss.toFixed(1)}`}
          />
        </div>
      </section>
    </div>
  )
}
