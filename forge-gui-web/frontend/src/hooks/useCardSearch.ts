import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { searchCards } from '../api/cards'
import type { CardSearchParams } from '../types/card'

export function useCardSearch(params: CardSearchParams, enabled = true) {
  return useQuery({
    queryKey: ['cards', params],
    queryFn: () => searchCards(params),
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    enabled,
  })
}
