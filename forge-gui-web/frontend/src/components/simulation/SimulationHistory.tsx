import { Trash2 } from 'lucide-react'
import { Button } from '../ui/button'
import { tierColor, powerScoreColor } from '@/lib/wilson'
import type { SimulationHistoryEntry } from '@/lib/simulation-types'

interface SimulationHistoryProps {
  history: SimulationHistoryEntry[]
  onSelect: (id: string) => void
  onDelete: (id: string) => void
}

function winRateColor(rate: number): string {
  if (rate >= 60) return 'text-green-500'
  if (rate >= 40) return 'text-yellow-500'
  return 'text-red-500'
}

function formatTimestamp(iso: string): string {
  const date = new Date(iso)
  const now = Date.now()
  const diffMs = now - date.getTime()
  const diffMin = Math.floor(diffMs / 60_000)
  const diffHr = Math.floor(diffMs / 3_600_000)
  const diffDay = Math.floor(diffMs / 86_400_000)

  if (diffMin < 1) return 'Just now'
  if (diffMin < 60) return `${diffMin} minute${diffMin !== 1 ? 's' : ''} ago`
  if (diffHr < 24) return `${diffHr} hour${diffHr !== 1 ? 's' : ''} ago`
  if (diffDay < 7) return `${diffDay} day${diffDay !== 1 ? 's' : ''} ago`

  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function SimulationHistory({ history, onSelect, onDelete }: SimulationHistoryProps) {
  if (history.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">No simulation history for this deck</p>
    )
  }

  // Sort by timestamp descending (most recent first)
  const sorted = [...history].sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
  )

  return (
    <div className="space-y-1">
      {sorted.map((entry) => {
        return (
          <div
            key={entry.id}
            className="flex items-center gap-3 rounded-lg border border-border px-3 py-2 cursor-pointer hover:bg-muted/50 transition-colors group"
            onClick={() => onSelect(entry.id)}
          >
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">
                {entry.gamesCompleted}/{entry.gamesTotal} games
              </p>
              <p className="text-xs text-muted-foreground">{formatTimestamp(entry.timestamp)}</p>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <span className={`text-sm font-medium tabular-nums ${winRateColor(entry.winRate ?? 0)}`}>
                {(entry.winRate ?? 0).toFixed(1)}%
              </span>
              <span className={`text-sm tabular-nums ${powerScoreColor(entry.powerScore ?? 0)}`} title={entry.tier ?? ''}>
                {entry.powerScore ?? '—'}
              </span>
              <span className={`text-xs ${tierColor(entry.tier ?? 'F')}`}>{entry.tier ?? '—'}</span>
              <Button
                variant="ghost"
                size="icon-xs"
                className="opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete(entry.id)
                }}
                title="Delete result"
              >
                <Trash2 className="size-3.5" />
              </Button>
            </div>
          </div>
        )
      })}
    </div>
  )
}
