import { useState, useCallback } from 'react'
import type { CardSearchResult } from '../types/card'
import type { DeckCardEntry } from '../types/deck'

type HoverCard = CardSearchResult | DeckCardEntry | null

interface MousePos {
  x: number
  y: number
}

export function useCardHover() {
  const [hoverCard, setHoverCard] = useState<HoverCard>(null)
  const [mousePos, setMousePos] = useState<MousePos>({ x: 0, y: 0 })

  const onCardMouseEnter = useCallback((card: HoverCard, e: React.MouseEvent) => {
    setHoverCard(card)
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onCardMouseMove = useCallback((e: React.MouseEvent) => {
    setMousePos({ x: e.clientX, y: e.clientY })
  }, [])

  const onCardMouseLeave = useCallback(() => {
    setHoverCard(null)
  }, [])

  return { hoverCard, mousePos, onCardMouseEnter, onCardMouseMove, onCardMouseLeave }
}
