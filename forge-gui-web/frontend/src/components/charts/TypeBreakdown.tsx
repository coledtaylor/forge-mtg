interface TypeBreakdownProps {
  breakdown: Record<string, number>
}

const TYPE_ORDER = ['Creature', 'Planeswalker', 'Instant', 'Sorcery', 'Enchantment', 'Artifact', 'Land']

export function TypeBreakdown({ breakdown }: TypeBreakdownProps) {
  const entries = TYPE_ORDER
    .filter(type => (breakdown[type] || 0) > 0)
    .map(type => ({ type, count: breakdown[type] || 0 }))

  if (entries.length === 0) return null

  const max = Math.max(...entries.map(e => e.count), 1)
  const rowHeight = 24
  const gap = 4
  const labelWidth = 100
  const barMaxWidth = 180
  const countWidth = 40
  const totalWidth = labelWidth + barMaxWidth + countWidth
  const totalHeight = entries.length * (rowHeight + gap) - gap

  return (
    <svg width={totalWidth} height={totalHeight} className="block">
      {entries.map((entry, i) => {
        const y = i * (rowHeight + gap)
        const barWidth = (entry.count / max) * barMaxWidth
        return (
          <g key={entry.type}>
            <text
              x={labelWidth - 8}
              y={y + rowHeight / 2}
              textAnchor="end"
              dominantBaseline="central"
              className="fill-muted-foreground"
              style={{ fontSize: '12px' }}
            >
              {entry.type}s
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
              {entry.count}
            </text>
          </g>
        )
      })}
    </svg>
  )
}
