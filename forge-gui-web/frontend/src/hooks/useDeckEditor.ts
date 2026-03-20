import { useState, useEffect, useCallback, useRef } from 'react'
import { useDeck, useUpdateDeck } from './useDecks'
import type { DeckDetail, DeckCardEntry, UpdateDeckPayload, ParseToken } from '../types/deck'
import type { CardSearchResult } from '../types/card'

function toCardMap(entries: DeckCardEntry[]): Record<string, number> {
  const map: Record<string, number> = {}
  for (const e of entries) {
    map[e.name] = e.quantity
  }
  return map
}

function cardEntryFromSearch(card: CardSearchResult, quantity: number): DeckCardEntry {
  return {
    name: card.name,
    quantity,
    setCode: card.setCode,
    collectorNumber: card.collectorNumber,
    manaCost: card.manaCost,
    typeLine: card.typeLine,
    cmc: card.cmc,
    colors: card.colors,
  }
}

export function useDeckEditor(deckName: string, format?: string) {
  const { data: serverDeck, isLoading } = useDeck(deckName)
  const updateMutation = useUpdateDeck()
  const [localDeck, setLocalDeck] = useState<DeckDetail | null>(null)
  const [isDirty, setIsDirty] = useState(false)
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Initialize from server
  useEffect(() => {
    if (serverDeck && !localDeck) {
      setLocalDeck(serverDeck)
    }
  }, [serverDeck, localDeck])

  const scheduleSave = useCallback((deck: DeckDetail) => {
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current)
    setIsDirty(true)
    saveTimerRef.current = setTimeout(() => {
      const payload: UpdateDeckPayload = {
        main: toCardMap(deck.main),
        sideboard: toCardMap(deck.sideboard),
        commander: toCardMap(deck.commander),
      }
      updateMutation.mutate({
        name: deckName,
        payload,
      }, {
        onSuccess: () => setIsDirty(false),
      })
    }, 1000)
  }, [deckName, updateMutation])

  const flushSave = useCallback(() => {
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current)
      saveTimerRef.current = null
    }
    if (localDeck && isDirty) {
      const payload: UpdateDeckPayload = {
        main: toCardMap(localDeck.main),
        sideboard: toCardMap(localDeck.sideboard),
        commander: toCardMap(localDeck.commander),
      }
      updateMutation.mutate({
        name: deckName,
        payload,
      }, {
        onSuccess: () => setIsDirty(false),
      })
    }
  }, [localDeck, isDirty, deckName, updateMutation])

  const addCard = useCallback((card: CardSearchResult, section: 'main' | 'sideboard' = 'main') => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const sectionCards = [...prev[section]]
      const existing = sectionCards.findIndex(c => c.name === card.name)
      if (existing >= 0) {
        sectionCards[existing] = { ...sectionCards[existing], quantity: sectionCards[existing].quantity + 1 }
      } else {
        sectionCards.push(cardEntryFromSearch(card, 1))
      }
      const updated = { ...prev, [section]: sectionCards }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  const removeCard = useCallback((cardName: string, section: 'main' | 'sideboard' = 'main') => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const sectionCards = prev[section]
        .map(c => c.name === cardName ? { ...c, quantity: c.quantity - 1 } : c)
        .filter(c => c.quantity > 0)
      const updated = { ...prev, [section]: sectionCards }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  const setQuantity = useCallback((cardName: string, quantity: number, section: 'main' | 'sideboard' = 'main') => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const sectionCards = quantity <= 0
        ? prev[section].filter(c => c.name !== cardName)
        : prev[section].map(c => c.name === cardName ? { ...c, quantity } : c)
      const updated = { ...prev, [section]: sectionCards }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  const setCommander = useCallback((card: DeckCardEntry) => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const updated = { ...prev, commander: [{ ...card, quantity: 1 }] }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  const removeCommander = useCallback(() => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const updated = { ...prev, commander: [] }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  const importCards = useCallback((tokens: ParseToken[], mode: 'replace' | 'add', rawText: string) => {
    setLocalDeck(prev => {
      if (!prev) return prev

      const isCommanderFormat = format?.toLowerCase() === 'commander'

      // Collect valid card tokens for import
      const cardTokens: ParseToken[] = []
      for (const token of tokens) {
        if (!token.cardName) continue
        const tokenType = token.type
        if (tokenType === 'UNKNOWN_CARD' || tokenType === 'UNSUPPORTED_CARD' ||
            tokenType === 'COMMENT' || tokenType === 'DECK_SECTION_NAME' ||
            tokenType === 'UNKNOWN_TEXT' || tokenType === 'DECK_NAME') continue
        cardTokens.push(token)
      }

      // Check if the raw text has an explicit section header like "Commander" or "//Commander"
      const hasExplicitSectionHeader = tokens.some(
        t => t.type === 'DECK_SECTION_NAME' && t.text === 'Commander'
      )

      // Detect commander from raw text for commander-format decks
      // Moxfield format: commander card(s) before blank line, main deck after
      // If no blank line, use first card as commander
      let commanderCardNames: Set<string> | null = null
      if (isCommanderFormat && !hasExplicitSectionHeader) {
        // Parse raw text to find blank line separator (backend strips blank lines from tokens)
        const lines = rawText.split(/\r?\n/)
        const preBlankLines: string[] = []
        let foundBlankLine = false
        let foundAnyCard = false

        for (const line of lines) {
          if (line.trim() === '') {
            if (foundAnyCard) {
              foundBlankLine = true
              break
            }
          } else {
            foundAnyCard = true
            if (!foundBlankLine) {
              preBlankLines.push(line.trim())
            }
          }
        }

        if (foundBlankLine && preBlankLines.length > 0) {
          // Extract card names from lines before the blank line
          // Match patterns like "1 Card Name" or "1x Card Name"
          commanderCardNames = new Set<string>()
          for (const line of preBlankLines) {
            const match = line.match(/^\d+x?\s+(.+)$/i)
            if (match) {
              const rawName = match[1].trim()
              // Find matching token by checking if any card token's name matches
              const matchingToken = cardTokens.find(t =>
                t.cardName && rawName.toLowerCase().startsWith(t.cardName.toLowerCase().substring(0, Math.min(10, t.cardName.length)))
              )
              if (matchingToken?.cardName) {
                commanderCardNames.add(matchingToken.cardName)
              }
            }
          }
          // If we couldn't match names, fall back to first card token
          if (commanderCardNames.size === 0 && cardTokens.length > 0) {
            commanderCardNames.add(cardTokens[0].cardName!)
          }
        } else if (cardTokens.length > 0) {
          // No blank line separator — use first card as commander
          commanderCardNames = new Set<string>([cardTokens[0].cardName!])
        }
      }

      // Build maps from parsed tokens
      // Ignore the backend's auto-assigned Commander section — use our own detection
      const importMain: Record<string, number> = {}
      const importSideboard: Record<string, number> = {}
      const importCommander: Record<string, number> = {}

      for (const token of cardTokens) {
        if (commanderCardNames && commanderCardNames.has(token.cardName!)) {
          importCommander[token.cardName!] = (importCommander[token.cardName!] || 0) + token.quantity
        } else if (hasExplicitSectionHeader && token.section === 'Commander') {
          importCommander[token.cardName!] = (importCommander[token.cardName!] || 0) + token.quantity
        } else if (token.section === 'Sideboard') {
          importSideboard[token.cardName!] = (importSideboard[token.cardName!] || 0) + token.quantity
        } else {
          importMain[token.cardName!] = (importMain[token.cardName!] || 0) + token.quantity
        }
      }

      function mergeCards(existing: DeckCardEntry[], additions: Record<string, number>): DeckCardEntry[] {
        const remaining = { ...additions }
        const result = existing.map(card => {
          if (remaining[card.name]) {
            const added = remaining[card.name]
            delete remaining[card.name]
            return { ...card, quantity: card.quantity + added }
          }
          return card
        })
        for (const [name, qty] of Object.entries(remaining)) {
          result.push({
            name, quantity: qty, setCode: '', collectorNumber: '',
            manaCost: '', typeLine: '', cmc: 0, colors: [],
          })
        }
        return result
      }

      let newMain: DeckCardEntry[]
      let newSideboard: DeckCardEntry[]
      let newCommander: DeckCardEntry[]

      if (mode === 'replace') {
        newMain = Object.entries(importMain).map(([name, qty]) => ({
          name, quantity: qty, setCode: '', collectorNumber: '',
          manaCost: '', typeLine: '', cmc: 0, colors: [],
        }))
        newSideboard = Object.entries(importSideboard).map(([name, qty]) => ({
          name, quantity: qty, setCode: '', collectorNumber: '',
          manaCost: '', typeLine: '', cmc: 0, colors: [],
        }))
        newCommander = Object.entries(importCommander).map(([name, qty]) => ({
          name, quantity: qty, setCode: '', collectorNumber: '',
          manaCost: '', typeLine: '', cmc: 0, colors: [],
        }))
      } else {
        newMain = mergeCards(prev.main, importMain)
        newSideboard = mergeCards(prev.sideboard, importSideboard)
        newCommander = mergeCards(prev.commander, importCommander)
      }

      const updated = { ...prev, main: newMain, sideboard: newSideboard, commander: newCommander }

      // For imports, save immediately (no debounce) and reset localDeck on success
      // so the useEffect re-syncs from server with full card metadata
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current)
      setIsDirty(true)
      const payload: UpdateDeckPayload = {
        main: toCardMap(updated.main),
        sideboard: toCardMap(updated.sideboard),
        commander: toCardMap(updated.commander),
      }
      updateMutation.mutate({
        name: deckName,
        payload,
      }, {
        onSuccess: () => {
          setIsDirty(false)
          setLocalDeck(null) // Reset to re-fetch from server with full metadata
        },
      })

      return updated
    })
  }, [format, deckName, updateMutation])

  const addBasicLand = useCallback((landName: string) => {
    setLocalDeck(prev => {
      if (!prev) return prev
      const sectionCards = [...prev.main]
      const existing = sectionCards.findIndex(c => c.name === landName)
      if (existing >= 0) {
        sectionCards[existing] = { ...sectionCards[existing], quantity: sectionCards[existing].quantity + 1 }
      } else {
        sectionCards.push({
          name: landName,
          quantity: 1,
          setCode: '',
          collectorNumber: '',
          manaCost: '',
          typeLine: 'Basic Land',
          cmc: 0,
          colors: [],
        })
      }
      const updated = { ...prev, main: sectionCards }
      scheduleSave(updated)
      return updated
    })
  }, [scheduleSave])

  return {
    deck: localDeck,
    isLoading,
    isDirty,
    isSaving: updateMutation.isPending,
    saveError: updateMutation.isError,
    addCard,
    removeCard,
    setQuantity,
    setCommander,
    removeCommander,
    addBasicLand,
    importCards,
    flushSave,
  }
}
