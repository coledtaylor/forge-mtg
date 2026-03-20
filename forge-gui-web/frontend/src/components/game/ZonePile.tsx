import { useState } from 'react'
import { Skull, CircleSlash, BookOpen, Crown } from 'lucide-react'
import { useGameStore } from '../../stores/gameStore'
import { GameCardImage } from './GameCardImage'
import { Badge } from '../ui/badge'
import { ZoneOverlay } from './ZoneOverlay'

interface ZonePileProps {
  playerId: number
  zone: 'Graveyard' | 'Exile' | 'Library' | 'Command'
  className?: string
}

const ZONE_ICONS = {
  Graveyard: Skull,
  Exile: CircleSlash,
  Library: BookOpen,
  Command: Crown,
} as const

export function ZonePile({ playerId, zone, className }: ZonePileProps) {
  const player = useGameStore((s) => s.players[playerId])
  const cards = useGameStore((s) => s.cards)
  const [expanded, setExpanded] = useState(false)

  const cardIds = player?.zones?.[zone] ?? []
  const count = cardIds.length
  const topCardId = cardIds.length > 0 ? cardIds[cardIds.length - 1] : null
  const topCard = topCardId !== null ? cards[topCardId] : null

  const Icon = ZONE_ICONS[zone]
  const canExpand = zone === 'Graveyard' || zone === 'Exile'
  const showTopCardFace = zone === 'Graveyard' || zone === 'Exile' || zone === 'Command'

  return (
    <>
      <div
        className={`relative w-[60px] h-[84px] transition-transform duration-150 hover:scale-105 ${
          canExpand ? 'cursor-pointer' : ''
        } ${className ?? ''}`}
        onClick={canExpand ? () => setExpanded(true) : undefined}
        title={`${zone}: ${count} cards`}
      >
        {count === 0 ? (
          /* Empty state */
          <div className="w-full h-full border border-dashed border-border rounded-md flex flex-col items-center justify-center gap-1">
            <Icon className="h-4 w-4 text-muted-foreground" />
            <span className="text-[10px] text-muted-foreground">0</span>
          </div>
        ) : (
          /* Has cards */
          <>
            {showTopCardFace ? (
              <GameCardImage name={topCard?.name ?? 'Unknown'} setCode={topCard?.setCode} collectorNumber={topCard?.collectorNumber} width={60} />
            ) : (
              /* Library: show card back / generic pile, not the top card (hidden info) */
              <div className="w-full h-full bg-card border border-border rounded-md flex flex-col items-center justify-center gap-1">
                <Icon className="h-4 w-4 text-muted-foreground" />
              </div>
            )}
            {/* Count badge */}
            <Badge
              variant="secondary"
              className="absolute -top-1 -right-1 h-4 min-w-4 px-1 text-[10px] font-semibold"
            >
              {count}
            </Badge>
          </>
        )}
      </div>

      {/* Zone overlay for graveyard/exile */}
      {canExpand && (
        <ZoneOverlay
          playerId={playerId}
          zone={zone as 'Graveyard' | 'Exile'}
          open={expanded}
          onClose={() => setExpanded(false)}
        />
      )}
    </>
  )
}
