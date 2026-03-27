import type { ManaProfile, SimulationProgress } from '@/lib/simulation-types'

interface ManaTabProps {
  data: SimulationProgress
}

// -------------------------------------------------------------------------
// Helpers — profile-aware
// -------------------------------------------------------------------------

function healthIndicatorWithProfile(
  screw: number,
  flood: number,
  profile: ManaProfile
): { icon: string; color: string; label: string } {
  // Compare actual rates (0-100%) against theoretical probabilities (0-1 -> 0-100%)
  const theoreticalScrew = profile.screwProbability * 100
  const theoreticalFlood = profile.floodProbability * 100
  const screwRatio = theoreticalScrew > 0 ? screw / theoreticalScrew : screw > 10 ? 2 : 1
  const floodRatio = theoreticalFlood > 0 ? flood / theoreticalFlood : flood > 10 ? 2 : 1
  if (screwRatio > 2.0 || floodRatio > 2.0)
    return { icon: '\u26a0', color: 'text-red-500', label: 'Poor' }
  if (screwRatio > 1.5 || floodRatio > 1.5)
    return { icon: '\u26a0', color: 'text-yellow-500', label: 'Fair' }
  return { icon: '\u2713', color: 'text-green-500', label: 'Good' }
}

function healthIndicatorFixed(screw: number, flood: number): { icon: string; color: string; label: string } {
  if (screw > 20 || flood > 20) return { icon: '\u26a0', color: 'text-red-500', label: 'Poor' }
  if (screw > 10 || flood > 10) return { icon: '\u26a0', color: 'text-yellow-500', label: 'Fair' }
  return { icon: '\u2713', color: 'text-green-500', label: 'Good' }
}

function landDeltaColor(actual: number, recommended: number): string {
  const delta = actual - recommended
  if (delta >= -1 && delta <= 2) return 'text-green-500'
  if (delta < -1) return 'text-red-500'
  return 'text-yellow-500'
}

function landDropColor(avgTurn: number, target: number): string {
  if (avgTurn === -1) return 'text-muted-foreground'
  if (avgTurn <= target) return 'text-green-500'
  if (avgTurn <= target + 1.5) return 'text-yellow-500'
  return 'text-red-500'
}

function landDropInterpretationFixed(third: number, fourth: number): { text: string; color: string } {
  if (third === -1 && fourth === -1) return { text: 'No data', color: 'text-muted-foreground' }
  const thirdBad = third > 5
  const fourthBad = fourth > 6
  if (thirdBad || fourthBad) return { text: 'Significantly behind -- consider more lands or fixing', color: 'text-red-500' }
  const thirdBehind = third > 3.5
  const fourthBehind = fourth > 4.5
  if (thirdBehind || fourthBehind) return { text: 'Slightly behind', color: 'text-yellow-500' }
  return { text: 'On curve', color: 'text-green-500' }
}

function landDropInterpretationWithProfile(
  third: number,
  fourth: number,
  keyTurn: number
): { text: string; color: string } {
  if (third === -1 && fourth === -1) return { text: 'No data', color: 'text-muted-foreground' }
  // Use keyTurn-based targets: 3rd land target = keyTurn-1, 4th land target = keyTurn
  const thirdTarget = Math.max(3, keyTurn - 1)
  const fourthTarget = keyTurn
  const thirdBad = third > 0 && third > thirdTarget + 2
  const fourthBad = fourth > 0 && fourth > fourthTarget + 2
  if (thirdBad || fourthBad)
    return { text: 'Significantly behind curve -- consider more lands or fixing', color: 'text-red-500' }
  const thirdBehind = third > 0 && third > thirdTarget + 0.5
  const fourthBehind = fourth > 0 && fourth > fourthTarget + 0.5
  if (thirdBehind || fourthBehind) return { text: 'Slightly behind curve', color: 'text-yellow-500' }
  return { text: 'On curve', color: 'text-green-500' }
}

function smartSuggestion(
  screw: number,
  flood: number,
  profile: ManaProfile
): { text: string; color: string } | null {
  const landDelta = profile.landCount - profile.recommendedLands
  const theoreticalScrew = profile.screwProbability * 100

  if (landDelta < -1) {
    return {
      text: `Add ${Math.round(-landDelta)} land${Math.round(-landDelta) !== 1 ? 's' : ''} to match the recommended ${profile.recommendedLands.toFixed(0)} for your curve.`,
      color: 'text-red-400',
    }
  }
  if (landDelta > 2) {
    return {
      text: `Cut ${Math.round(landDelta - 2)} land${Math.round(landDelta - 2) !== 1 ? 's' : ''} -- you're running ${profile.landCount} vs recommended ${profile.recommendedLands.toFixed(0)}.`,
      color: 'text-yellow-400',
    }
  }
  if (theoreticalScrew > 0 && screw > theoreticalScrew * 1.5) {
    return {
      text: 'Mana screw rate exceeds expected -- consider improving color consistency or land quality.',
      color: 'text-yellow-400',
    }
  }
  return { text: 'Mana base looks good for your curve.', color: 'text-green-400' }
}

// -------------------------------------------------------------------------
// Sub-components
// -------------------------------------------------------------------------

interface ResourceCardProps {
  label: string
  value: string
  alert?: boolean
  tooltip?: string
}

function ResourceCard({ label, value, alert, tooltip }: ResourceCardProps) {
  return (
    <div className="rounded-lg border border-border p-4" title={tooltip}>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${alert ? 'text-red-500' : ''}`}>{value}</p>
    </div>
  )
}

// -------------------------------------------------------------------------
// Main component
// -------------------------------------------------------------------------

export function ManaTab({ data }: ManaTabProps) {
  const profile = data.manaProfile
  const screwPct = data.manaScrew.toFixed(1)
  const floodPct = data.manaFlood.toFixed(1)
  const thirdLand = data.avgThirdLandTurn
  const fourthLand = data.avgFourthLandTurn

  const health = profile
    ? healthIndicatorWithProfile(data.manaScrew, data.manaFlood, profile)
    : healthIndicatorFixed(data.manaScrew, data.manaFlood)

  const landInterp = profile
    ? landDropInterpretationWithProfile(thirdLand, fourthLand, profile.keyTurn)
    : landDropInterpretationFixed(thirdLand, fourthLand)

  const thirdTarget = profile ? Math.max(3, profile.keyTurn - 1) : 3.5
  const fourthTarget = profile ? profile.keyTurn : 4.5

  return (
    <div className="space-y-6">

      {/* Profile header — only when profile data is available */}
      {profile && (
        <section className="rounded-lg border border-border p-4 space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div>
              <p className="text-xs text-muted-foreground uppercase tracking-wide">Deck Archetype</p>
              <p className="text-sm font-semibold capitalize">{profile.archetype}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted-foreground uppercase tracking-wide">Key Turn</p>
              <p className="text-sm font-semibold">Turn {profile.keyTurn}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted-foreground uppercase tracking-wide">Avg CMC</p>
              <p className="text-sm font-semibold">{profile.avgCmc.toFixed(2)}</p>
            </div>
          </div>

          {/* Recommended vs Actual Lands */}
          <div>
            <p className="text-xs text-muted-foreground mb-1">
              Lands: Actual vs Recommended (Karsten)
            </p>
            <div className="flex items-baseline gap-2">
              <span className={`text-2xl font-bold tabular-nums ${landDeltaColor(profile.landCount, profile.recommendedLands)}`}>
                {profile.landCount}
              </span>
              <span className="text-muted-foreground text-sm">/ {profile.recommendedLands.toFixed(0)} recommended</span>
            </div>
            {Math.abs(profile.landCount - profile.recommendedLands) > 0.5 && (
              <p className={`text-xs mt-1 ${landDeltaColor(profile.landCount, profile.recommendedLands)}`}>
                {profile.landCount < profile.recommendedLands
                  ? `${(profile.recommendedLands - profile.landCount).toFixed(0)} below recommendation`
                  : `${(profile.landCount - profile.recommendedLands).toFixed(0)} above recommendation`}
              </p>
            )}
          </div>
        </section>
      )}

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
          <div
            className="space-y-1"
            title={
              profile
                ? `Games where you missed your key mana by turn ${profile.keyTurn}. Theoretical baseline: ${(profile.screwProbability * 100).toFixed(1)}%`
                : 'Percentage of games (lasting 5+ turns) where mana was missed'
            }
          >
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Mana Screw Rate</span>
              <span className="tabular-nums font-medium">
                {screwPct}%
                {profile && (
                  <span className="text-muted-foreground text-xs ml-1">
                    (expected {(profile.screwProbability * 100).toFixed(1)}%)
                  </span>
                )}
              </span>
            </div>
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-red-500 transition-all"
                style={{ width: `${Math.min(data.manaScrew, 100)}%` }}
              />
            </div>
          </div>

          {/* Mana Flood */}
          <div
            className="space-y-1"
            title={
              profile
                ? `Games where excess lands (${profile.landExcessThreshold}+) were drawn. Theoretical baseline: ${(profile.floodProbability * 100).toFixed(1)}%`
                : 'Percentage of games ending in a loss with an excess of land draws'
            }
          >
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Mana Flood Rate</span>
              <span className="tabular-nums font-medium">
                {floodPct}%
                {profile && (
                  <span className="text-muted-foreground text-xs ml-1">
                    (expected {(profile.floodProbability * 100).toFixed(1)}%)
                  </span>
                )}
              </span>
            </div>
            <div className="flex h-3 w-full overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-blue-500 transition-all"
                style={{ width: `${Math.min(data.manaFlood, 100)}%` }}
              />
            </div>
          </div>

          {/* Smart suggestions (profile-aware) or fixed fallback */}
          <div className="text-xs space-y-0.5">
            {profile ? (
              (() => {
                const s = smartSuggestion(data.manaScrew, data.manaFlood, profile)
                return s ? <p className={s.color}>{s.text}</p> : null
              })()
            ) : (
              <>
                {data.manaScrew > 20 && (
                  <p className="text-red-400">Consider adding more lands</p>
                )}
                {data.manaFlood > 15 && (
                  <p className="text-blue-400">Consider cutting some lands</p>
                )}
              </>
            )}
          </div>
        </div>
      </section>

      {/* Land Drop Timing */}
      <section className="space-y-3">
        <h3 className="text-sm font-semibold">Land Drop Timing</h3>

        <div className="grid grid-cols-2 gap-3">
          <div
            className="rounded-lg border border-border p-4"
            title={`Average turn number when your 3rd land hits the battlefield (target: turn ${Math.round(thirdTarget)})`}
          >
            <p className="text-sm text-muted-foreground">Avg 3rd Land</p>
            <p className={`text-2xl font-bold mt-1 tabular-nums ${landDropColor(thirdLand, thirdTarget)}`}>
              {thirdLand === -1 ? 'N/A' : `Turn ${thirdLand.toFixed(1)}`}
            </p>
          </div>
          <div
            className="rounded-lg border border-border p-4"
            title={`Average turn number when your 4th land hits the battlefield (target: turn ${Math.round(fourthTarget)})`}
          >
            <p className="text-sm text-muted-foreground">Avg 4th Land</p>
            <p className={`text-2xl font-bold mt-1 tabular-nums ${landDropColor(fourthLand, fourthTarget)}`}>
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
            label="Cards Used From Library"
            value={`${data.avgCardsDrawn.toFixed(1)}`}
            tooltip="Average number of cards that left your library per game (draws, searches, mill, etc.)"
          />
          <ResourceCard
            label="Turns With Empty Hand"
            value={`${data.avgEmptyHandTurns.toFixed(1)}`}
            alert={data.avgEmptyHandTurns > 2}
            tooltip="Average turns per game where you had no cards in hand (not yet tracked -- shows 0)"
          />
          <ResourceCard
            label="Your Life When You Win"
            value={`${data.avgLifeAtWin.toFixed(1)}`}
            tooltip="Average life total remaining when you win a game"
          />
          <ResourceCard
            label="Your Life When You Lose"
            value={`${data.avgLifeAtLoss.toFixed(1)}`}
            tooltip="Average life total when you lose a game (0 means death by damage)"
          />
        </div>
      </section>
    </div>
  )
}
