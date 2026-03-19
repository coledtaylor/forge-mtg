import { useState, useEffect, useCallback, useRef } from 'react'
import { useDeck, useUpdateDeck } from './useDecks'
import type { DeckDetail, DeckCardEntry, UpdateDeckPayload } from '../types/deck'
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

export function useDeckEditor(deckName: string) {
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
    flushSave,
  }
}
