import { fetchApi } from './client'
import type { DeckSummary, DeckDetail, UpdateDeckPayload, CreateDeckPayload, ValidationResult, ParseToken } from '../types/deck'

export async function listDecks(): Promise<DeckSummary[]> {
  return fetchApi<DeckSummary[]>('/api/decks')
}

export async function createDeck(payload: CreateDeckPayload): Promise<DeckDetail> {
  return fetchApi<DeckDetail>('/api/decks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function getDeck(name: string): Promise<DeckDetail> {
  return fetchApi<DeckDetail>(`/api/decks/${encodeURIComponent(name)}`)
}

export async function deleteDeck(name: string): Promise<void> {
  return fetchApi<void>(`/api/decks/${encodeURIComponent(name)}`, { method: 'DELETE' })
}

export async function updateDeck(name: string, payload: UpdateDeckPayload): Promise<DeckDetail> {
  return fetchApi<DeckDetail>(`/api/decks/${encodeURIComponent(name)}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function validateDeck(name: string, format: string): Promise<ValidationResult> {
  return fetchApi<ValidationResult>(
    `/api/decks/${encodeURIComponent(name)}/validate?format=${encodeURIComponent(format)}`
  )
}

export async function parseDeckText(text: string): Promise<ParseToken[]> {
  return fetchApi<ParseToken[]>('/api/decks/parse', {
    method: 'POST',
    body: JSON.stringify({ text }),
  })
}

export async function exportDeck(name: string, format: string): Promise<string> {
  const result = await fetchApi<{ text: string }>(
    `/api/decks/${encodeURIComponent(name)}/export?format=${encodeURIComponent(format)}`
  )
  return result.text
}
