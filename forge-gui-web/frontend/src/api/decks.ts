import { fetchApi } from './client'
import type { DeckSummary, DeckDetail } from '../types/deck'

export async function listDecks(): Promise<DeckSummary[]> {
  return fetchApi<DeckSummary[]>('/api/decks')
}

export async function createDeck(name: string): Promise<DeckDetail> {
  return fetchApi<DeckDetail>('/api/decks', {
    method: 'POST',
    body: JSON.stringify({ name }),
  })
}

export async function getDeck(name: string): Promise<DeckDetail> {
  return fetchApi<DeckDetail>(`/api/decks/${encodeURIComponent(name)}`)
}

export async function deleteDeck(name: string): Promise<void> {
  return fetchApi<void>(`/api/decks/${encodeURIComponent(name)}`, { method: 'DELETE' })
}
