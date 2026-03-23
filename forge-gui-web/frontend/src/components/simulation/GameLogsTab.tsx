import { useState, useEffect } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { getGameLogs, getGameLogDetail } from '@/api/simulation'
import type { GameLogSummary, GameLogDetail, GameLogEntry } from '@/lib/simulation-types'

interface GameLogsTabProps {
  simulationId?: string
}

const TYPE_COLORS: Record<string, string> = {
  TURN: 'text-yellow-500 font-semibold',
  LAND: 'text-green-500',
  STACK_ADD: 'text-blue-400',
  STACK_RESOLVE: 'text-blue-300',
  COMBAT: 'text-red-400',
  DAMAGE: 'text-red-500',
  LIFE: 'text-pink-400',
  ZONE_CHANGE: 'text-purple-400',
  MULLIGAN: 'text-orange-400',
  GAME_OUTCOME: 'text-yellow-400 font-semibold',
  PHASE: 'text-muted-foreground',
}

function outcomeColor(winner: string, playerDeck: string): string {
  return winner === playerDeck ? 'text-green-500' : 'text-red-500'
}

function GameLogEntryRow({ entry }: { entry: GameLogEntry }) {
  const colorClass = TYPE_COLORS[entry.type] ?? 'text-muted-foreground'
  return (
    <div className="flex gap-3 py-0.5 text-xs font-mono">
      <span className="text-muted-foreground w-6 text-right shrink-0">
        {entry.turn > 0 ? entry.turn : ''}
      </span>
      <span className={`w-24 shrink-0 ${colorClass}`}>{entry.type}</span>
      <span className="text-foreground">{entry.message}</span>
    </div>
  )
}

function GameLogRow({ log, playerDeck }: { log: GameLogSummary; playerDeck?: string }) {
  const [expanded, setExpanded] = useState(false)
  const [detail, setDetail] = useState<GameLogDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [typeFilter, setTypeFilter] = useState<string | null>(null)

  async function handleToggle() {
    if (expanded) {
      setExpanded(false)
      return
    }
    setExpanded(true)
    if (!detail) {
      setLoading(true)
      try {
        const data = await getGameLogDetail(log.id)
        setDetail(data)
      } catch {
        // Failed to load
      } finally {
        setLoading(false)
      }
    }
  }

  const won = log.winner === (playerDeck ?? log.playerDeck)
  const filteredEntries = detail?.entries.filter(
    e => !typeFilter || e.type === typeFilter
  )

  // Collect unique types for filter
  const entryTypes = detail
    ? [...new Set(detail.entries.map(e => e.type))]
    : []

  return (
    <div className="border border-border rounded-lg overflow-hidden">
      <button
        onClick={handleToggle}
        className="w-full flex items-center gap-3 px-3 py-2 text-sm hover:bg-muted/50 transition-colors"
      >
        {expanded ? <ChevronDown className="size-4 shrink-0" /> : <ChevronRight className="size-4 shrink-0" />}
        <span className={`font-medium ${won ? 'text-green-500' : 'text-red-500'}`}>
          {won ? 'W' : 'L'}
        </span>
        <span className="text-muted-foreground">T{log.turns}</span>
        <span className="truncate flex-1 text-left">vs {log.opponentDeck}</span>
        <span className="text-xs text-muted-foreground shrink-0">
          {log.onPlay ? 'Play' : 'Draw'}
        </span>
        {log.source === 'match' && (
          <span className="text-xs bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded shrink-0">
            PvAI
          </span>
        )}
      </button>

      {expanded && (
        <div className="border-t border-border bg-muted/20">
          {loading && (
            <p className="text-xs text-muted-foreground p-3">Loading game log...</p>
          )}
          {detail && filteredEntries && (
            <div className="flex flex-col">
              {/* Type filter bar */}
              <div className="flex flex-wrap gap-1 px-3 py-2 border-b border-border">
                <button
                  onClick={() => setTypeFilter(null)}
                  className={`text-xs px-2 py-0.5 rounded ${!typeFilter ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground hover:text-foreground'}`}
                >
                  All ({detail.entries.length})
                </button>
                {entryTypes.map(type => {
                  const count = detail.entries.filter(e => e.type === type).length
                  return (
                    <button
                      key={type}
                      onClick={() => setTypeFilter(typeFilter === type ? null : type)}
                      className={`text-xs px-2 py-0.5 rounded ${typeFilter === type ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground hover:text-foreground'}`}
                    >
                      {type} ({count})
                    </button>
                  )
                })}
              </div>

              {/* Log entries */}
              <div className="max-h-80 overflow-y-auto px-3 py-1">
                {filteredEntries.map((entry, i) => (
                  <GameLogEntryRow key={i} entry={entry} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export function GameLogsTab({ simulationId }: GameLogsTabProps) {
  const [logs, setLogs] = useState<GameLogSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getGameLogs(simulationId)
      .then(data => {
        if (!cancelled) setLogs(data)
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [simulationId])

  if (loading) {
    return <p className="text-sm text-muted-foreground py-4">Loading game logs...</p>
  }

  if (logs.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">No game logs available. Run a simulation to generate logs.</p>
  }

  const playerDeck = logs[0]?.playerDeck

  return (
    <div className="space-y-2 py-2">
      <p className="text-xs text-muted-foreground">
        {logs.length} game{logs.length !== 1 ? 's' : ''} logged
      </p>
      {logs.map(log => (
        <GameLogRow key={log.id} log={log} playerDeck={playerDeck} />
      ))}
    </div>
  )
}
