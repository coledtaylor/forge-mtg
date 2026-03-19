import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listDecks, createDeck, deleteDeck } from '../api/decks'

export function useDecks() {
  return useQuery({
    queryKey: ['decks'],
    queryFn: listDecks,
  })
}

export function useCreateDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => createDeck(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['decks'] }),
  })
}

export function useDeleteDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => deleteDeck(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['decks'] }),
  })
}
