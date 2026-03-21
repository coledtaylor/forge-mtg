import { useState, useMemo } from 'react'
import type { CardPerformance } from '@/lib/simulation-types'

interface PerformanceTabProps {
  cardPerformance: Record<string, CardPerformance>
  totalGames: number
}

type SortKey = 'name' | 'gamesDrawn' | 'drawRate' | 'winRate' | 'deadRate'
type SortDir = 'asc' | 'desc'

function winRateColor(rate: number): string {
  if (rate >= 60) return 'text-green-500'
  if (rate >= 40) return 'text-yellow-500'
  return 'text-red-500'
}

export function PerformanceTab({ cardPerformance, totalGames }: PerformanceTabProps) {
  const [sortKey, setSortKey] = useState<SortKey>('winRate')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  const rows = useMemo(() => {
    const entries = Object.entries(cardPerformance).map(([name, perf]) => ({
      name,
      gamesDrawn: perf.gamesDrawn,
      drawRate: totalGames > 0 ? (perf.gamesDrawn / totalGames) * 100 : 0,
      winRate: perf.winRateWhenDrawn,
      deadRate: perf.deadCardRate,
      insufficientData: perf.gamesDrawn < 3,
    }))

    entries.sort((a, b) => {
      let cmp: number
      switch (sortKey) {
        case 'name':
          cmp = a.name.localeCompare(b.name)
          break
        case 'gamesDrawn':
          cmp = a.gamesDrawn - b.gamesDrawn
          break
        case 'drawRate':
          cmp = a.drawRate - b.drawRate
          break
        case 'winRate':
          cmp = a.winRate - b.winRate
          break
        case 'deadRate':
          cmp = a.deadRate - b.deadRate
          break
        default:
          cmp = 0
      }
      return sortDir === 'desc' ? -cmp : cmp
    })

    return entries
  }, [cardPerformance, totalGames, sortKey, sortDir])

  function handleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'desc' ? 'asc' : 'desc'))
    } else {
      setSortKey(key)
      setSortDir('desc')
    }
  }

  if (rows.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">No per-card data available</p>
    )
  }

  // Summary stats (only from cards with sufficient data)
  const sufficient = rows.filter((r) => !r.insufficientData)
  const best = sufficient.length > 0
    ? sufficient.reduce((a, b) => (a.winRate > b.winRate ? a : b))
    : null
  const worst = sufficient.length > 0
    ? sufficient.reduce((a, b) => (a.winRate < b.winRate ? a : b))
    : null
  const mostDead = sufficient.length > 0
    ? sufficient.reduce((a, b) => (a.deadRate > b.deadRate ? a : b))
    : null

  const sortIndicator = (key: SortKey) => {
    if (sortKey !== key) return ''
    return sortDir === 'desc' ? ' \u2193' : ' \u2191'
  }

  return (
    <div className="space-y-4">
      {/* Summary */}
      {sufficient.length > 0 && (
        <div className="space-y-1 text-sm text-muted-foreground">
          {best && (
            <p>
              Best performer:{' '}
              <span className="text-foreground font-medium">{best.name}</span>{' '}
              ({best.winRate.toFixed(1)}% win rate when drawn)
            </p>
          )}
          {worst && (
            <p>
              Worst performer:{' '}
              <span className="text-foreground font-medium">{worst.name}</span>{' '}
              ({worst.winRate.toFixed(1)}%)
            </p>
          )}
          {mostDead && mostDead.deadRate > 0 && (
            <p>
              Most dead:{' '}
              <span className="text-foreground font-medium">{mostDead.name}</span>{' '}
              ({mostDead.deadRate.toFixed(1)}% dead card rate)
            </p>
          )}
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th
                className="pb-2 pr-4 font-medium cursor-pointer select-none hover:text-foreground"
                onClick={() => handleSort('name')}
              >
                Card Name{sortIndicator('name')}
              </th>
              <th
                className="pb-2 pr-4 font-medium text-right cursor-pointer select-none hover:text-foreground"
                onClick={() => handleSort('gamesDrawn')}
              >
                Games Drawn{sortIndicator('gamesDrawn')}
              </th>
              <th
                className="pb-2 pr-4 font-medium text-right cursor-pointer select-none hover:text-foreground"
                onClick={() => handleSort('drawRate')}
              >
                Draw Rate{sortIndicator('drawRate')}
              </th>
              <th
                className="pb-2 pr-4 font-medium text-right cursor-pointer select-none hover:text-foreground"
                onClick={() => handleSort('winRate')}
              >
                Win Rate When Drawn{sortIndicator('winRate')}
              </th>
              <th
                className="pb-2 font-medium text-right cursor-pointer select-none hover:text-foreground"
                onClick={() => handleSort('deadRate')}
              >
                Dead Card Rate{sortIndicator('deadRate')}
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                key={row.name}
                className={`border-b border-border/50 ${row.insufficientData ? 'opacity-50' : ''}`}
              >
                <td className="py-2 pr-4 font-medium">
                  {row.name}
                  {row.insufficientData && (
                    <span className="ml-2 text-xs text-muted-foreground italic">insufficient data</span>
                  )}
                </td>
                <td className="py-2 pr-4 text-right tabular-nums">{row.gamesDrawn}</td>
                <td className="py-2 pr-4 text-right tabular-nums">{row.drawRate.toFixed(1)}%</td>
                <td className={`py-2 pr-4 text-right tabular-nums font-medium ${winRateColor(row.winRate)}`}>
                  {row.winRate.toFixed(1)}%
                </td>
                <td className={`py-2 text-right tabular-nums font-medium ${row.deadRate > 30 ? 'text-red-500' : ''}`}>
                  {row.deadRate.toFixed(1)}%
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
