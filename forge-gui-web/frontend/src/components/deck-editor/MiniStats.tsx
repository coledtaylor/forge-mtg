import { useMemo } from 'react'
import { ManaCurveChart } from '../charts/ManaCurveChart'
import { computeManaCurve, totalCards, deckColors } from '../../lib/deck-stats'
import type { DeckCardEntry } from '../../types/deck'

const MTG_COLOR_HEX: Record<string, string> = {
  W: '#F9FAF4', U: '#0E68AB', B: '#150B00', R: '#D3202A', G: '#00733E',
}

interface MiniStatsProps {
  cards: DeckCardEntry[]
}

export function MiniStats({ cards }: MiniStatsProps) {
  const curve = useMemo(() => computeManaCurve(cards), [cards])
  const total = useMemo(() => totalCards(cards), [cards])
  const colors = useMemo(() => deckColors(cards), [cards])

  return (
    <div className="flex items-center gap-4 px-4 py-2 border-t border-border" style={{ height: '80px' }}>
      <ManaCurveChart curve={curve} mini />
      <div className="flex flex-col gap-1">
        <span className="text-[20px] font-semibold text-foreground">{total}</span>
        <div className="flex items-center gap-1">
          {colors.map(c => (
            <span
              key={c}
              className="inline-block w-3 h-3 rounded-full"
              style={{
                backgroundColor: MTG_COLOR_HEX[c] || '#A0A0A0',
                border: c === 'B' ? '1px solid #555' : c === 'W' ? '1px solid #d4d4d4' : undefined,
              }}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
