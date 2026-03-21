import { fetchApi } from './client'
import type { JumpstartPack } from '../types/jumpstart'

export async function fetchJumpstartPacks(): Promise<JumpstartPack[]> {
  return fetchApi<JumpstartPack[]>('/api/jumpstart/packs')
}
