import type { DeckCardEntry } from '../types/deck'
import { getPrimaryType } from './deck-stats'

/** Ordered list of type groups as displayed in the UI */
export const TYPE_GROUP_ORDER = [
  'Creature',
  'Planeswalker',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Land',
  'Other',
] as const

export interface CardTypeGroup {
  type: string
  cards: DeckCardEntry[]
  count: number
}

/**
 * Group deck cards by their primary type in display order.
 * Each group has a type label, card list, and total count (sum of quantities).
 * Empty groups are excluded.
 */
export function groupByType(cards: DeckCardEntry[]): CardTypeGroup[] {
  const grouped = new Map<string, DeckCardEntry[]>()

  for (const card of cards) {
    const type = getPrimaryType(card.typeLine)
    const list = grouped.get(type) || []
    list.push(card)
    grouped.set(type, list)
  }

  return TYPE_GROUP_ORDER
    .filter(type => grouped.has(type))
    .map(type => {
      const groupCards = grouped.get(type)!
      return {
        type,
        cards: groupCards.sort((a, b) => a.name.localeCompare(b.name)),
        count: groupCards.reduce((sum, c) => sum + c.quantity, 0),
      }
    })
}
