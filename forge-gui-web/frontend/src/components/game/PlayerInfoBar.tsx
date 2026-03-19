import { useEffect, useRef, useState } from 'react'
import { Layers } from 'lucide-react'
import { useGameStore } from '../../stores/gameStore'
import { ManaPool } from './ManaPool'
import { Badge } from '../ui/badge'
import type { PlayerDto } from '../../lib/gameTypes'

interface PlayerInfoBarProps {
  player: PlayerDto | undefined
  isOpponent?: boolean
}

export function PlayerInfoBar({ player, isOpponent }: PlayerInfoBarProps) {
  const activePlayerId = useGameStore((s) => s.activePlayerId)
  const isActive = player !== undefined && player.id === activePlayerId

  // Animated life change tracking
  const prevLifeRef = useRef<number | null>(null)
  const [lifeColor, setLifeColor] = useState<string>('')

  const life = player?.life ?? 0

  useEffect(() => {
    if (prevLifeRef.current === null) {
      prevLifeRef.current = life
      return
    }
    if (life < prevLifeRef.current) {
      setLifeColor('text-red-400')
    } else if (life > prevLifeRef.current) {
      setLifeColor('text-green-400')
    }
    prevLifeRef.current = life

    const timer = setTimeout(() => setLifeColor(''), 300)
    return () => clearTimeout(timer)
  }, [life])

  const handCount = player?.zones?.Hand?.length ?? 0

  return (
    <div className="h-9 bg-card flex items-center gap-4 px-4">
      {/* Player name */}
      <span
        className={`text-xs text-muted-foreground ${
          isActive ? 'border-b-2 border-primary' : ''
        }`}
      >
        {player?.name ?? (isOpponent ? 'Opponent' : 'Player')}
      </span>

      {/* Life total */}
      <span
        className={`text-xl font-semibold transition-colors duration-300 ${
          lifeColor || 'text-foreground'
        }`}
      >
        {life}
      </span>

      {/* Poison counters */}
      {(player?.poisonCounters ?? 0) > 0 && (
        <Badge className="bg-purple-600 text-white text-xs">
          {player!.poisonCounters} poison
        </Badge>
      )}

      {/* Hand count */}
      <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
        <Layers className="h-3 w-3" />
        {handCount}
      </span>

      {/* Mana pool (right-aligned) */}
      <span className="ml-auto">
        <ManaPool mana={player?.mana} />
      </span>
    </div>
  )
}
