import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listDecks, createDeck, getDeck, updateDeck, validateDeck, deleteDeck } from '../api/decks'
import type { UpdateDeckPayload, CreateDeckPayload } from '../types/deck'

export function useDecks() {
  return useQuery({
    queryKey: ['decks'],
    queryFn: listDecks,
  })
}

export function useDeck(name: string) {
  return useQuery({
    queryKey: ['deck', name],
    queryFn: () => getDeck(name),
    enabled: !!name,
  })
}

export function useCreateDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateDeckPayload) => createDeck(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['decks'] }),
  })
}

export function useUpdateDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ name, payload }: { name: string; payload: UpdateDeckPayload }) =>
      updateDeck(name, payload),
    onSuccess: (_, { name }) => {
      queryClient.invalidateQueries({ queryKey: ['deck', name] })
      queryClient.invalidateQueries({ queryKey: ['decks'] })
    },
  })
}

export function useValidateDeck(name: string, format: string) {
  return useQuery({
    queryKey: ['deck-validate', name, format],
    queryFn: () => validateDeck(name, format),
    enabled: !!name && !!format,
    staleTime: 30_000,
  })
}

export function useDeleteDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => deleteDeck(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['decks'] }),
  })
}
