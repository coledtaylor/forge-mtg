import { useQuery } from '@tanstack/react-query'
import { fetchJumpstartPacks } from '../api/jumpstart'

export function useJumpstartPacks() {
  return useQuery({
    queryKey: ['jumpstart-packs'],
    queryFn: fetchJumpstartPacks,
    staleTime: Infinity, // Pack definitions never change during session
  })
}
