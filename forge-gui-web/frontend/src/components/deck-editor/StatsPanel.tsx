import { useMemo } from 'react'
import { ManaCurveChart } from '../charts/ManaCurveChart'
import { ColorDistribution } from '../charts/ColorDistribution'
import { TypeBreakdown } from '../charts/TypeBreakdown'
import {
  computeManaCurve, computeColorDistribution, computeTypeBreakdown,
  totalCards, averageCMC, deckColors,
} from '../../lib/deck-stats'
import type { DeckCardEntry, ValidationResult } from '../../types/deck'

interface StatsPanelProps {
  cards: DeckCardEntry[]
  format: string
  validation: ValidationResult | null | undefined
  isValidating: boolean
}

export function StatsPanel({ cards, format, validation, isValidating }: StatsPanelProps) {
  const curve = useMemo(() => computeManaCurve(cards), [cards])
  const colorDist = useMemo(() => computeColorDistribution(cards), [cards])
  const typeBk = useMemo(() => computeTypeBreakdown(cards), [cards])
  const total = useMemo(() => totalCards(cards), [cards])
  const avgCmc = useMemo(() => averageCMC(cards), [cards])
  const colors = useMemo(() => deckColors(cards), [cards])
  const landCount = useMemo(() => {
    return cards.filter(c => c.typeLine.includes('Land')).reduce((sum, c) => sum + c.quantity, 0)
  }, [cards])
  const nonLandCount = total - landCount

  return (
    <div className="space-y-6 py-4">
      <section>
        <h3 className="text-[14px] font-semibold text-foreground mb-3">Mana Curve</h3>
        <ManaCurveChart curve={curve} />
      </section>

      <section>
        <h3 className="text-[14px] font-semibold text-foreground mb-3">Color Distribution</h3>
        <ColorDistribution distribution={colorDist} totalNonLand={nonLandCount} />
      </section>

      <section>
        <h3 className="text-[14px] font-semibold text-foreground mb-3">Card Types</h3>
        <TypeBreakdown breakdown={typeBk} />
      </section>

      <section>
        <h3 className="text-[14px] font-semibold text-foreground mb-3">Deck Summary</h3>
        <div className="space-y-2 text-[14px]">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Total</span>
            <span className="text-foreground">{total} cards</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Average CMC</span>
            <span className="text-foreground">{avgCmc}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Lands</span>
            <span className="text-foreground">{landCount} ({total > 0 ? Math.round((landCount / total) * 100) : 0}%)</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Colors</span>
            <span className="text-foreground">{colors.length > 0 ? colors.join(', ') : 'Colorless'}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Format</span>
            <span className="text-foreground">
              {format || 'Casual'}
              {format && validation && (
                validation.legal
                  ? <span className="ml-2 text-muted-foreground">&#10003; Legal</span>
                  : <span className="ml-2 text-destructive">Not Legal</span>
              )}
              {format && isValidating && <span className="ml-2 text-muted-foreground">Checking...</span>}
            </span>
          </div>
          {validation && !validation.legal && validation.conformanceProblem && (
            <div className="text-[12px] text-destructive mt-1">
              {validation.conformanceProblem}
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
