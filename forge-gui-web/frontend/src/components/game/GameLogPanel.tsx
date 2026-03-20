import { useEffect, useRef } from 'react'
import { useGameStore } from '../../stores/gameStore'
import type { GameLogEntry } from '../../lib/gameTypes'
import {
  Swords, Heart, Zap, Hash, Clock, PlusCircle, CheckCircle,
  ArrowRight, Shuffle, Info, Leaf,
} from 'lucide-react'

// Color and icon mapping for log entry types
const entryConfig: Record<string, { color: string; icon: React.ComponentType<{ className?: string }> }> = {
  TURN:           { color: 'text-primary',           icon: Hash },
  PHASE:          { color: 'text-muted-foreground',  icon: Clock },
  STACK_ADD:      { color: 'text-blue-400',          icon: PlusCircle },
  STACK_RESOLVE:  { color: 'text-green-400',         icon: CheckCircle },
  DAMAGE:         { color: 'text-red-400',           icon: Zap },
  LIFE:           { color: 'text-yellow-400',        icon: Heart },
  LAND:           { color: 'text-green-400',         icon: Leaf },
  COMBAT:         { color: 'text-orange-400',        icon: Swords },
  ZONE_CHANGE:    { color: 'text-muted-foreground',  icon: ArrowRight },
  MULLIGAN:       { color: 'text-purple-400',        icon: Shuffle },
  INFORMATION:    { color: 'text-muted-foreground',  icon: Info },
}

const defaultConfig = { color: 'text-muted-foreground', icon: Info }

interface GameLogPanelProps {
  className?: string
}

export function GameLogPanel({ className }: GameLogPanelProps) {
  const gameLog = useGameStore((s) => s.gameLog)
  const scrollRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom when new entries arrive
  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: 'smooth',
    })
  }, [gameLog.length])

  if (gameLog.length === 0) {
    return (
      <div className={`flex items-center justify-center text-xs text-muted-foreground ${className ?? ''}`}>
        Game log will appear here
      </div>
    )
  }

  return (
    <div ref={scrollRef} className={`flex-1 overflow-y-auto px-3 pb-2 flex flex-col gap-0.5 ${className ?? ''}`}>
      {gameLog.map((entry: GameLogEntry, i: number) => {
        // TURN entries: bold separator
        if (entry.type === 'TURN') {
          return (
            <div key={i} className="text-xs font-semibold text-primary border-t border-border pt-1 mt-1">
              {entry.message}
            </div>
          )
        }

        // PHASE entries: subtle phase marker
        if (entry.type === 'PHASE') {
          return (
            <div key={i} className="text-[10px] text-muted-foreground uppercase tracking-wide mt-0.5">
              {entry.message}
            </div>
          )
        }

        // Other entries: icon + message
        const config = entryConfig[entry.type] ?? defaultConfig
        const Icon = config.icon
        return (
          <div key={i} className={`flex items-start gap-1.5 text-xs ${config.color}`}>
            <Icon className="size-3 shrink-0 mt-0.5" />
            <span>{entry.message}</span>
          </div>
        )
      })}
    </div>
  )
}
