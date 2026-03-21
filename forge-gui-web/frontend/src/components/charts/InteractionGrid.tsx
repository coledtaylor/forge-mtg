import type { InteractionRange } from '../../lib/deck-analysis'

interface InteractionGridProps {
  range: InteractionRange
}

const ROWS: { label: string; key: keyof InteractionRange }[] = [
  { label: 'Creatures', key: 'creatures' },
  { label: 'Enchantments', key: 'enchantments' },
  { label: 'Artifacts', key: 'artifacts' },
  { label: 'Planeswalkers', key: 'planeswalkers' },
  { label: 'Graveyards', key: 'graveyards' },
  { label: 'Lands', key: 'lands' },
]

export function InteractionGrid({ range }: InteractionGridProps) {
  const rowHeight = 24
  const gap = 4
  const labelWidth = 110
  const countWidth = 40
  const indicatorX = labelWidth + countWidth + 8
  const totalWidth = indicatorX + 24
  const totalHeight = ROWS.length * (rowHeight + gap) - gap

  return (
    <svg width={totalWidth} height={totalHeight} className="block">
      {ROWS.map((row, i) => {
        const y = i * (rowHeight + gap)
        const count = range[row.key]
        const covered = count > 0
        return (
          <g key={row.key}>
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
            <text
              x={labelWidth + countWidth - 8}
              y={y + rowHeight / 2}
              textAnchor="end"
              dominantBaseline="central"
              className="fill-foreground"
              style={{ fontSize: '12px' }}
            >
              {count}
            </text>
            {covered ? (
              <text
                x={indicatorX}
                y={y + rowHeight / 2}
                dominantBaseline="central"
                className="fill-green-500"
                style={{ fontSize: '14px', fontWeight: 'bold' }}
              >
                &#10003;
              </text>
            ) : (
              <text
                x={indicatorX}
                y={y + rowHeight / 2}
                dominantBaseline="central"
                className="fill-red-500"
                style={{ fontSize: '14px', fontWeight: 'bold' }}
              >
                &#10007;
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
