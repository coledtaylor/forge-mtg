/**
 * Parse a mana cost string into an array of symbol identifiers.
 * Handles both brace format "{W}{U}{2}" and space-separated "W U 2".
 * Forge's ManaCost.toString() returns brace format like "{2}{W}{U}".
 */
export function parseManaCost(manaCost: string): string[] {
  if (!manaCost) return []

  // Try brace format first: {W}, {2}, {W/U}, {2/W}, {X}, {C}
  const braceRegex = /\{([^}]+)\}/g
  const symbols: string[] = []
  let match
  while ((match = braceRegex.exec(manaCost)) !== null) {
    symbols.push(match[1])
  }
  if (symbols.length > 0) return symbols

  // Fallback: space-separated format
  return manaCost.trim().split(/\s+/).filter(Boolean)
}

/**
 * Convert a parsed mana symbol to the CSS class name for mana-font.
 * mana-font uses classes like: ms-w, ms-u, ms-b, ms-r, ms-g, ms-c, ms-1, ms-2, ms-x
 * Hybrid mana like "W/U" becomes "ms-wu" (slash removed, lowercased).
 */
export function manaSymbolClass(symbol: string): string {
  return `ms ms-${symbol.replace(/\//g, '').toLowerCase()} ms-cost`
}
