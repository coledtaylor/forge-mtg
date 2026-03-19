const MTG_COLORS: Record<string, { hex: string; label: string; stroke?: string }> = {
  W: { hex: '#F9FAF4', label: 'White', stroke: '#d4d4d4' },
  U: { hex: '#0E68AB', label: 'Blue' },
  B: { hex: '#150B00', label: 'Black', stroke: 'var(--border)' },
  R: { hex: '#D3202A', label: 'Red' },
  G: { hex: '#00733E', label: 'Green' },
  C: { hex: '#A0A0A0', label: 'Colorless' },
}

interface ColorDistributionProps {
  distribution: Record<string, number>
  totalNonLand: number
}

export function ColorDistribution({ distribution, totalNonLand }: ColorDistributionProps) {
  const size = 160
  const outerR = size / 2
  const innerR = 40
  const cx = outerR
  const cy = outerR

  // Filter to non-zero segments
  const segments = Object.entries(distribution)
    .filter(([, count]) => count > 0)
    .map(([color, count]) => ({ color, count }))

  const total = segments.reduce((sum, s) => sum + s.count, 0)

  if (total === 0) {
    return (
      <div className="flex flex-col items-center gap-3">
        <svg width={size} height={size}>
          <circle cx={cx} cy={cy} r={outerR - 2} fill="none" stroke="var(--border)" strokeWidth={outerR - innerR} />
          <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central" className="fill-muted-foreground" style={{ fontSize: '14px' }}>
            No cards
          </text>
        </svg>
      </div>
    )
  }

  // Build arc paths
  let startAngle = -Math.PI / 2
  const arcs = segments.map((seg) => {
    const proportion = seg.count / total
    const endAngle = startAngle + proportion * 2 * Math.PI
    const largeArc = proportion > 0.5 ? 1 : 0

    const x1Outer = cx + outerR * Math.cos(startAngle)
    const y1Outer = cy + outerR * Math.sin(startAngle)
    const x2Outer = cx + outerR * Math.cos(endAngle)
    const y2Outer = cy + outerR * Math.sin(endAngle)
    const x1Inner = cx + innerR * Math.cos(endAngle)
    const y1Inner = cy + innerR * Math.sin(endAngle)
    const x2Inner = cx + innerR * Math.cos(startAngle)
    const y2Inner = cy + innerR * Math.sin(startAngle)

    const d = [
      `M ${x1Outer} ${y1Outer}`,
      `A ${outerR} ${outerR} 0 ${largeArc} 1 ${x2Outer} ${y2Outer}`,
      `L ${x1Inner} ${y1Inner}`,
      `A ${innerR} ${innerR} 0 ${largeArc} 0 ${x2Inner} ${y2Inner}`,
      'Z',
    ].join(' ')

    const colorInfo = MTG_COLORS[seg.color] || MTG_COLORS.C
    const result = { d, fill: colorInfo.hex, stroke: colorInfo.stroke, color: seg.color }
    startAngle = endAngle
    return result
  })

  return (
    <div className="flex flex-col items-center gap-3">
      <svg width={size} height={size}>
        {arcs.map((arc, i) => (
          <path
            key={i}
            d={arc.d}
            fill={arc.fill}
            stroke={arc.stroke || 'none'}
            strokeWidth={arc.stroke ? 1 : 0}
          />
        ))}
        <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central"
          className="fill-foreground" style={{ fontSize: '20px', fontWeight: 600 }}>
          {totalNonLand}
        </text>
      </svg>
      <div className="flex items-center gap-4 flex-wrap justify-center">
        {segments.map((seg) => {
          const info = MTG_COLORS[seg.color] || MTG_COLORS.C
          return (
            <div key={seg.color} className="flex items-center gap-1.5">
              <span
                className="inline-block w-3 h-3 rounded-full"
                style={{ backgroundColor: info.hex, border: info.stroke ? `1px solid ${info.stroke}` : undefined }}
              />
              <span className="text-[12px] text-muted-foreground">{info.label} ({seg.count})</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
