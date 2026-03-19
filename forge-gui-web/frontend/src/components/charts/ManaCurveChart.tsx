interface ManaCurveChartProps {
  curve: number[]
  mini?: boolean
}

export function ManaCurveChart({ curve, mini = false }: ManaCurveChartProps) {
  const max = Math.max(...curve, 1)
  const barWidth = mini ? 16 : 32
  const gap = mini ? 2 : 4
  const chartHeight = mini ? 50 : 120
  const labelHeight = mini ? 14 : 20
  const totalHeight = chartHeight + labelHeight
  const totalWidth = curve.length * (barWidth + gap) - gap

  return (
    <svg width={totalWidth} height={totalHeight} className="block">
      {curve.map((count, i) => {
        const barHeight = max > 0 ? (count / max) * chartHeight : 0
        const x = i * (barWidth + gap)
        return (
          <g key={i}>
            <rect
              x={x}
              y={chartHeight - barHeight}
              width={barWidth}
              height={barHeight}
              className="fill-primary"
              rx={2}
            />
            <text
              x={x + barWidth / 2}
              y={chartHeight + labelHeight - 2}
              textAnchor="middle"
              className="fill-muted-foreground"
              style={{ fontSize: mini ? '10px' : '12px' }}
            >
              {i === 7 ? '7+' : i}
            </text>
            {!mini && count > 0 && (
              <text
                x={x + barWidth / 2}
                y={chartHeight - barHeight - 4}
                textAnchor="middle"
                className="fill-foreground"
                style={{ fontSize: '12px' }}
              >
                {count}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}
