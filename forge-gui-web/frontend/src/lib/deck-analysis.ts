import type { DeckCardEntry } from '../types/deck'

// --- Types ---

export interface CardMatch {
  name: string
  quantity: number
}

export interface DeckComposition {
  removal: { hard: CardMatch[]; soft: CardMatch[]; sweepers: CardMatch[]; total: number }
  ramp: { creatures: CardMatch[]; artifacts: CardMatch[]; spells: CardMatch[]; total: number }
  draw: { draw: CardMatch[]; cantrips: CardMatch[]; filtering: CardMatch[]; total: number }
}

export interface InteractionRange {
  creatures: number
  enchantments: number
  artifacts: number
  planeswalkers: number
  graveyards: number
  lands: number
}

export interface ConsistencyMetrics {
  fourOfRatio: number
  tutorCount: number
  threatRedundancy: number
}

export interface WinConditionAnalysis {
  altWinCons: CardMatch[]
  bigThreats: CardMatch[]
  planeswalkers: CardMatch[]
  total: number
}

// --- Regex Patterns ---

// Hard removal (targeted destroy/exile of creatures, permanents, planeswalkers)
const HARD_REMOVAL = [
  /destroy target (?:.*?)(?:creature|permanent|planeswalker)/i,
  /exile target (?:.*?)(?:creature|permanent|planeswalker)/i,
]

// Sweepers (mass removal)
const SWEEPERS = [
  /destroy all (?:.*?)creature/i,
  /exile all (?:.*?)creature/i,
  /deals? \d+ damage to each creature/i,
  /all creatures get -\d+\/-\d+/i,
]

// Soft removal (conditional/temporary removal)
const SOFT_REMOVAL = [
  /deals? \d+ damage to (?:target|any target)/i,
  /gets? -\d+\/-\d+/i,
  /target (?:.*?) fights? /i,
  /fight target/i,
  /return target (?:.*?) to its owner's hand/i,
]

// Ramp patterns
const MANA_PRODUCERS = /\{T\}:? add \{/i
const LAND_SEARCH = /search your library for (?:a|an|up to \w+)(?:\s\w+)* (?:basic )?land/i
const RITUAL = /add \{[WUBRGC]\}\{[WUBRGC]\}/i

// Draw patterns
const DRAW_MULTIPLE = /draw (?:two|three|four|five|six|seven|\d+) cards?/i
const CANTRIP = /draw a card/i
const FILTERING = /\b(?:scry|surveil)\b/i
const LOOK_TOP = /look at the top \d+ cards?/i

// Interaction range patterns
const CREATURE_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)creature/i
const ENCHANTMENT_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)(?:enchantment|permanent|nonland permanent)/i
const ARTIFACT_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)(?:artifact|permanent|nonland permanent)/i
const PLANESWALKER_ANSWER = /(?:destroy|exile|damage)(?:.*?)(?:target|each)(?:.*?)planeswalker/i
const GRAVEYARD_ANSWER = /exile (?:.*?)(?:from (?:a |target )?graveyard|all cards from (?:.*?)graveyard)/i
const LAND_ANSWER = /(?:destroy|exile)(?:.*?)(?:target|all)(?:.*?)land/i

// Win condition patterns
const ALT_WIN = /you win the game/i
const TUTOR = /search your library/i

// --- Helper ---

function sumQuantities(matches: CardMatch[]): number {
  return matches.reduce((sum, m) => sum + m.quantity, 0)
}

function matchEntry(card: DeckCardEntry): CardMatch {
  return { name: card.name, quantity: card.quantity }
}

// --- Analysis Functions ---

/**
 * Classify cards into removal, ramp, and card draw categories.
 * Cards CAN appear in multiple categories (e.g., Cryptic Command = removal AND draw).
 */
export function analyzeDeckComposition(cards: DeckCardEntry[]): DeckComposition {
  const result: DeckComposition = {
    removal: { hard: [], soft: [], sweepers: [], total: 0 },
    ramp: { creatures: [], artifacts: [], spells: [], total: 0 },
    draw: { draw: [], cantrips: [], filtering: [], total: 0 },
  }

  for (const card of cards) {
    if (!card.oracleText) continue
    const text = card.oracleText
    const typeLine = card.typeLine.toLowerCase()

    // Skip lands for ramp classification (ramp = acceleration beyond land drops)
    const isLand = typeLine.includes('land')

    // Removal classification (a card goes into the highest-priority matching subcategory)
    if (SWEEPERS.some((r) => r.test(text))) {
      result.removal.sweepers.push(matchEntry(card))
    } else if (HARD_REMOVAL.some((r) => r.test(text))) {
      result.removal.hard.push(matchEntry(card))
    } else if (SOFT_REMOVAL.some((r) => r.test(text))) {
      result.removal.soft.push(matchEntry(card))
    }

    // Ramp classification (exclude lands)
    if (!isLand && (MANA_PRODUCERS.test(text) || LAND_SEARCH.test(text) || RITUAL.test(text))) {
      if (typeLine.includes('creature')) {
        result.ramp.creatures.push(matchEntry(card))
      } else if (typeLine.includes('artifact')) {
        result.ramp.artifacts.push(matchEntry(card))
      } else {
        result.ramp.spells.push(matchEntry(card))
      }
    }

    // Draw classification (a card goes into the highest-value matching subcategory)
    if (DRAW_MULTIPLE.test(text)) {
      result.draw.draw.push(matchEntry(card))
    } else if (CANTRIP.test(text)) {
      result.draw.cantrips.push(matchEntry(card))
    } else if (FILTERING.test(text) || LOOK_TOP.test(text)) {
      result.draw.filtering.push(matchEntry(card))
    }
  }

  // Compute totals as sum of quantities across subcategories
  result.removal.total =
    sumQuantities(result.removal.hard) +
    sumQuantities(result.removal.soft) +
    sumQuantities(result.removal.sweepers)
  result.ramp.total =
    sumQuantities(result.ramp.creatures) +
    sumQuantities(result.ramp.artifacts) +
    sumQuantities(result.ramp.spells)
  result.draw.total =
    sumQuantities(result.draw.draw) +
    sumQuantities(result.draw.cantrips) +
    sumQuantities(result.draw.filtering)

  return result
}

/**
 * Count cards that answer each permanent type.
 * A card answering multiple types counts in each (e.g., Vindicate = creature + enchantment + artifact + planeswalker + land).
 */
export function analyzeInteractionRange(cards: DeckCardEntry[]): InteractionRange {
  const range: InteractionRange = {
    creatures: 0,
    enchantments: 0,
    artifacts: 0,
    planeswalkers: 0,
    graveyards: 0,
    lands: 0,
  }

  for (const card of cards) {
    if (!card.oracleText) continue
    const text = card.oracleText

    if (CREATURE_ANSWER.test(text)) range.creatures += card.quantity
    if (ENCHANTMENT_ANSWER.test(text)) range.enchantments += card.quantity
    if (ARTIFACT_ANSWER.test(text)) range.artifacts += card.quantity
    if (PLANESWALKER_ANSWER.test(text)) range.planeswalkers += card.quantity
    if (GRAVEYARD_ANSWER.test(text)) range.graveyards += card.quantity
    if (LAND_ANSWER.test(text)) range.lands += card.quantity
  }

  return range
}

/**
 * Compute consistency metrics: 4-of ratio, tutor count, and threat redundancy.
 */
export function analyzeConsistency(cards: DeckCardEntry[]): ConsistencyMetrics {
  if (cards.length === 0) {
    return { fourOfRatio: 0, tutorCount: 0, threatRedundancy: 0 }
  }

  const fourOfs = cards.filter((c) => c.quantity >= 4).length
  const fourOfRatio = fourOfs / cards.length

  let tutorCount = 0
  for (const card of cards) {
    if (card.oracleText && TUTOR.test(card.oracleText)) {
      tutorCount += card.quantity
    }
  }

  // Threat redundancy = number of distinct win condition cards
  const winCons = analyzeWinConditions(cards)
  const threatRedundancy = winCons.total

  return { fourOfRatio, tutorCount, threatRedundancy }
}

/**
 * Identify win conditions: alternate win cons, big threats (CMC 5+ creatures with power 4+),
 * and planeswalkers.
 */
export function analyzeWinConditions(cards: DeckCardEntry[]): WinConditionAnalysis {
  const altWinCons: CardMatch[] = []
  const bigThreats: CardMatch[] = []
  const planeswalkers: CardMatch[] = []
  const seen = new Set<string>()

  for (const card of cards) {
    const typeLine = card.typeLine.toLowerCase()

    // Alternate win conditions
    if (card.oracleText && ALT_WIN.test(card.oracleText)) {
      altWinCons.push(matchEntry(card))
      seen.add(card.name)
    }

    // Big threats: CMC 5+ creatures with power 4+
    if (typeLine.includes('creature') && card.cmc >= 5 && card.power >= 4) {
      bigThreats.push(matchEntry(card))
      seen.add(card.name)
    }

    // Planeswalkers
    if (typeLine.includes('planeswalker')) {
      planeswalkers.push(matchEntry(card))
      seen.add(card.name)
    }
  }

  // Total = unique cards across all subcategories (deduplicated)
  const total = seen.size

  return { altWinCons, bigThreats, planeswalkers, total }
}
