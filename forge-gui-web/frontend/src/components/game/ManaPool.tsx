import { MANA_COLORS } from '../../lib/gameTypes'

interface ManaPoolProps {
  mana: Record<string, number> | undefined
}

export function ManaPool({ mana }: ManaPoolProps) {
  if (!mana) return null

  const entries = (Object.keys(MANA_COLORS) as (keyof typeof MANA_COLORS)[])
    .filter((color) => (mana[color] ?? 0) > 0)

  if (entries.length === 0) return null

  return (
    <span className="inline-flex items-center gap-1">
      {entries.map((color) => (
        <span key={color} className="inline-flex items-center gap-0.5">
          <i className={MANA_COLORS[color].className} />
          <span className="text-xs text-foreground">{mana[color]}</span>
        </span>
      ))}
    </span>
  )
}
