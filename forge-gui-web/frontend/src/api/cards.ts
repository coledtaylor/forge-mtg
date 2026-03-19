import { fetchApi } from './client'
import type { CardSearchParams, CardSearchResponse } from '../types/card'

export async function searchCards(params: CardSearchParams): Promise<CardSearchResponse> {
  const searchParams = new URLSearchParams()
  if (params.q) searchParams.set('q', params.q)
  if (params.color) searchParams.set('color', params.color)
  if (params.type) searchParams.set('type', params.type)
  if (params.cmc !== undefined) searchParams.set('cmc', String(params.cmc))
  if (params.cmcOp) searchParams.set('cmcOp', params.cmcOp)
  if (params.format) searchParams.set('format', params.format)
  if (params.page) searchParams.set('page', String(params.page))
  if (params.limit) searchParams.set('limit', String(params.limit))
  return fetchApi<CardSearchResponse>(`/api/cards?${searchParams.toString()}`)
}
