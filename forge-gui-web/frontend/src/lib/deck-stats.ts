import type { DeckCardEntry } from '../types/deck'

/**
 * Compute mana curve as array of 8 buckets: [CMC 0, 1, 2, 3, 4, 5, 6, 7+].
 * Excludes lands from the curve.
 */
export function computeManaCurve(cards: DeckCardEntry[]): number[] {
  const curve = new Array(8).fill(0)
  for (const entry of cards) {
    if (entry.typeLine.includes('Land')) continue
    const bucket = Math.min(entry.cmc, 7)
    curve[bucket] += entry.quantity
  }
  return curve
}

/**
 * Compute color distribution from card color identities.
 * Returns { W, U, B, R, G, C } counts. Cards with no colors count as Colorless.
 */
export function computeColorDistribution(cards: DeckCardEntry[]): Record<string, number> {
  const colors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0, C: 0 }
  for (const entry of cards) {
    if (entry.typeLine.includes('Land')) continue
    if (!entry.colors || entry.colors.length === 0) {
      colors.C += entry.quantity
    } else {
      for (const c of entry.colors) {
        if (colors[c] !== undefined) {
          colors[c] += entry.quantity
        }
      }
    }
  }
  return colors
}

/**
 * Compute card type breakdown. Returns map of type name to count.
 * Uses priority ordering: Creature > Planeswalker > Instant > Sorcery > Enchantment > Artifact > Land.
 * A "Legendary Enchantment Creature" counts as Creature (highest priority match).
 */
export function computeTypeBreakdown(cards: DeckCardEntry[]): Record<string, number> {
  const types: Record<string, number> = {}
  for (const entry of cards) {
    const primary = getPrimaryType(entry.typeLine)
    types[primary] = (types[primary] || 0) + entry.quantity
  }
  return types
}

/** Type priority order for grouping */
const TYPE_PRIORITY = ['Creature', 'Planeswalker', 'Battle', 'Instant', 'Sorcery', 'Enchantment', 'Artifact', 'Land']

/**
 * Extract primary card type from typeLine using priority ordering.
 * "Legendary Enchantment Creature - Elf Druid" -> "Creature"
 */
export function getPrimaryType(typeLine: string): string {
  if (!typeLine) return 'Other'
  const typeLineLower = typeLine.toLowerCase()
  for (const type of TYPE_PRIORITY) {
    if (typeLineLower.includes(type.toLowerCase())) {
      return type
    }
  }
  return 'Other'
}

/**
 * Compute total card count from entries.
 */
export function totalCards(cards: DeckCardEntry[]): number {
  return cards.reduce((sum, c) => sum + c.quantity, 0)
}

/**
 * Compute average CMC (excluding lands).
 */
export function averageCMC(cards: DeckCardEntry[]): number {
  let totalCost = 0
  let totalCount = 0
  for (const entry of cards) {
    if (entry.typeLine.includes('Land')) continue
    totalCost += entry.cmc * entry.quantity
    totalCount += entry.quantity
  }
  return totalCount === 0 ? 0 : Math.round((totalCost / totalCount) * 10) / 10
}

/**
 * Get deck's color identity (unique colors from all cards).
 */
export function deckColors(cards: DeckCardEntry[]): string[] {
  const colorOrder = ['W', 'U', 'B', 'R', 'G']
  const present = new Set<string>()
  for (const entry of cards) {
    if (entry.colors) {
      for (const c of entry.colors) present.add(c)
    }
  }
  return colorOrder.filter(c => present.has(c))
}
