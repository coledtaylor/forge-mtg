import type { DeckComposition, CardMatch } from '../../lib/deck-analysis'

interface CompositionBreakdownProps {
  composition: DeckComposition
}

interface GroupData {
  label: string
  total: number
  subcategories: { label: string; count: number }[]
}

function sumQuantities(matches: CardMatch[]): number {
  return matches.reduce((sum, m) => sum + m.quantity, 0)
}

function BarGroup({ label, total, subcategories }: GroupData) {
  const rows = subcategories.filter(s => s.count > 0)
  if (rows.length === 0) return null

  const max = Math.max(...rows.map(r => r.count), 1)
  const rowHeight = 24
  const gap = 4
  const labelWidth = 100
  const barMaxWidth = 180
  const countWidth = 40
  const totalWidth = labelWidth + barMaxWidth + countWidth
  const totalHeight = rows.length * (rowHeight + gap) - gap

  return (
    <div className="mb-4 last:mb-0">
      <div className="text-[13px] font-medium text-foreground mb-1">
        {label} ({total})
      </div>
      <svg width={totalWidth} height={totalHeight} className="block">
        {rows.map((row, i) => {
          const y = i * (rowHeight + gap)
          const barWidth = (row.count / max) * barMaxWidth
          return (
            <g key={row.label}>
              <text
                x={labelWidth - 8}
                y={y + rowHeight / 2}
                textAnchor="end"
                dominantBaseline="central"
                className="fill-muted-foreground"
                style={{ fontSize: '12px' }}
              >
                {row.label}
              </text>
              <rect
                x={labelWidth}
                y={y + 2}
                width={barWidth}
                height={rowHeight - 4}
                className="fill-primary"
                rx={2}
              />
              <text
                x={labelWidth + barWidth + 8}
                y={y + rowHeight / 2}
                dominantBaseline="central"
                className="fill-foreground"
                style={{ fontSize: '12px' }}
              >
                {row.count}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

export function CompositionBreakdown({ composition }: CompositionBreakdownProps) {
  const groups: GroupData[] = [
    {
      label: 'Removal',
      total: composition.removal.total,
      subcategories: [
        { label: 'Hard', count: sumQuantities(composition.removal.hard) },
        { label: 'Soft', count: sumQuantities(composition.removal.soft) },
        { label: 'Sweepers', count: sumQuantities(composition.removal.sweepers) },
      ],
    },
    {
      label: 'Ramp',
      total: composition.ramp.total,
      subcategories: [
        { label: 'Creatures', count: sumQuantities(composition.ramp.creatures) },
        { label: 'Artifacts', count: sumQuantities(composition.ramp.artifacts) },
        { label: 'Spells', count: sumQuantities(composition.ramp.spells) },
      ],
    },
    {
      label: 'Card Draw',
      total: composition.draw.total,
      subcategories: [
        { label: 'Draw', count: sumQuantities(composition.draw.draw) },
        { label: 'Cantrips', count: sumQuantities(composition.draw.cantrips) },
        { label: 'Filtering', count: sumQuantities(composition.draw.filtering) },
      ],
    },
  ]

  const nonEmpty = groups.filter(g => g.total > 0)
  if (nonEmpty.length === 0) return null

  return (
    <div>
      {nonEmpty.map(group => (
        <BarGroup key={group.label} {...group} />
      ))}
    </div>
  )
}
