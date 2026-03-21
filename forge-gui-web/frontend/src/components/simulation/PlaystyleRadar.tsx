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

const CENTER = 100
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
  const dataPoints = AXES.map(({ key, angle }) => {
    const value = Math.max(0, Math.min(1, scores[key] ?? 0))
    return polarToXY(angle, value)
  })

  const dataPolygon = dataPoints.map(([x, y]) => `${x},${y}`).join(' ')

  return (
    <svg viewBox="0 0 200 200" className={className}>
      {/* Grid rings */}
      {GRID_LEVELS.map((level) => (
        <polygon
          key={level}
          points={gridPolygon(level)}
          fill="none"
          stroke="currentColor"
          strokeOpacity={0.15}
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
            stroke="currentColor"
            strokeOpacity={0.15}
            strokeWidth={1}
          />
        )
      })}

      {/* Data polygon */}
      <polygon
        points={dataPolygon}
        fill="hsl(var(--primary))"
        fillOpacity={0.3}
        stroke="hsl(var(--primary))"
        strokeWidth={2}
      />

      {/* Data points */}
      {dataPoints.map(([x, y], i) => (
        <circle
          key={AXES[i].key}
          cx={x}
          cy={y}
          r={3}
          fill="hsl(var(--primary))"
        />
      ))}

      {/* Axis labels */}
      {AXES.map(({ key, label, angle }) => {
        const value = Math.round((scores[key] ?? 0) * 100)
        const labelDistance = 1.22
        const [x, y] = polarToXY(angle, labelDistance)

        // Adjust text anchor based on position
        let textAnchor: 'start' | 'middle' | 'end' = 'middle'
        if (Math.abs(Math.cos(angle)) > 0.5) {
          textAnchor = Math.cos(angle) > 0 ? 'start' : 'end'
        }

        // Adjust vertical alignment
        let dy = '0.35em'
        if (Math.abs(Math.sin(angle)) > 0.5) {
          dy = Math.sin(angle) > 0 ? '1em' : '-0.3em'
        }

        return (
          <text
            key={key}
            x={x}
            y={y}
            textAnchor={textAnchor}
            dy={dy}
            className="fill-muted-foreground"
            style={{ fontSize: '11px' }}
          >
            {label} {value}%
          </text>
        )
      })}
    </svg>
  )
}
