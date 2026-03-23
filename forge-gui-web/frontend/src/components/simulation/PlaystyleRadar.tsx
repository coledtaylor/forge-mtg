interface PlaystyleRadarProps {
  scores: Record<string, number>
  className?: string
}

const AXES = [
  { key: 'aggro', label: 'Aggro', angle: -Math.PI / 2 },       // top
  { key: 'midrange', label: 'Midrange', angle: 0 },              // right
  { key: 'control', label: 'Control', angle: Math.PI / 2 },      // bottom
  { key: 'combo', label: 'Combo', angle: Math.PI },               // left
]

const SIZE = 260
const CENTER = SIZE / 2
const RADIUS = 80
const GRID_LEVELS = [0.25, 0.5, 0.75, 1.0]

function polarToXY(angle: number, fraction: number): [number, number] {
  return [
    CENTER + RADIUS * fraction * Math.cos(angle),
    CENTER + RADIUS * fraction * Math.sin(angle),
  ]
}

function gridPolygon(fraction: number): string {
  return AXES.map(({ angle }) => {
    const [x, y] = polarToXY(angle, fraction)
    return `${x},${y}`
  }).join(' ')
}

export function PlaystyleRadar({ scores, className = 'w-48 h-48' }: PlaystyleRadarProps) {
  // Ensure minimum visibility even with very low scores
  const dataPoints = AXES.map(({ key, angle }) => {
    const raw = Math.max(0, Math.min(1, scores[key] ?? 0))
    const value = Math.max(0.08, raw) // minimum 8% so polygon is always visible
    return polarToXY(angle, value)
  })

  const dataPolygon = dataPoints.map(([x, y]) => `${x},${y}`).join(' ')

  return (
    <svg viewBox={`0 0 ${SIZE} ${SIZE}`} overflow="visible" className={className}>
      {/* Grid rings */}
      {GRID_LEVELS.map((level) => (
        <polygon
          key={level}
          points={gridPolygon(level)}
          fill="none"
          stroke="var(--muted-foreground)"
          strokeOpacity={0.3}
          strokeWidth={1}
        />
      ))}

      {/* Axis lines */}
      {AXES.map(({ key, angle }) => {
        const [x, y] = polarToXY(angle, 1)
        return (
          <line
            key={key}
            x1={CENTER}
            y1={CENTER}
            x2={x}
            y2={y}
            stroke="var(--muted-foreground)"
            strokeOpacity={0.3}
            strokeWidth={1}
          />
        )
      })}

      {/* Data polygon */}
      <polygon
        points={dataPolygon}
        fill="var(--primary)"
        fillOpacity={0.3}
        stroke="var(--primary)"
        strokeWidth={2}
      />

      {/* Data points */}
      {dataPoints.map(([x, y], i) => (
        <circle
          key={AXES[i].key}
          cx={x}
          cy={y}
          r={3}
          fill="var(--primary)"
        />
      ))}

      {/* Axis labels */}
      {AXES.map(({ key, label, angle }) => {
        const pct = Math.round((scores[key] ?? 0) * 100)

        // Position labels well outside the chart area
        const labelRadius = RADIUS + 40
        const lx = CENTER + labelRadius * Math.cos(angle)
        const ly = CENTER + labelRadius * Math.sin(angle)

        // Text anchor based on quadrant
        let textAnchor: 'start' | 'middle' | 'end' = 'middle'
        if (Math.cos(angle) > 0.3) textAnchor = 'start'
        else if (Math.cos(angle) < -0.3) textAnchor = 'end'

        return (
          <text
            key={key}
            x={lx}
            y={ly}
            textAnchor={textAnchor}
            dominantBaseline="central"
            fill="var(--muted-foreground)"
            style={{ fontSize: '11px' }}
          >
            {label} {pct}%
          </text>
        )
      })}
    </svg>
  )
}
