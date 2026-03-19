import { parseManaCost, manaSymbolClass } from '../lib/mana'

interface ManaCostProps {
  cost: string
}

export function ManaCost({ cost }: ManaCostProps) {
  const symbols = parseManaCost(cost)
  if (symbols.length === 0) return null

  return (
    <span className="inline-flex items-center gap-0.5">
      {symbols.map((s, i) => (
        <i key={i} className={manaSymbolClass(s)} />
      ))}
    </span>
  )
}
