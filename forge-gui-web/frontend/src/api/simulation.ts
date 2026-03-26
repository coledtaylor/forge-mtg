import { fetchApi } from './client'
import type { SimulationConfig, SimulationProgress, SimulationHistoryEntry, GameLogSummary, GameLogDetail } from '../lib/simulation-types'

export async function startSimulation(config: SimulationConfig): Promise<{ id: string }> {
  return fetchApi<{ id: string }>('/api/simulations/start', {
    method: 'POST',
    body: JSON.stringify(config),
  })
}

export async function getSimulationStatus(id: string): Promise<SimulationProgress> {
  return fetchApi<SimulationProgress>(`/api/simulations/${encodeURIComponent(id)}/status`)
}

export async function cancelSimulation(id: string): Promise<void> {
  return fetchApi<void>(`/api/simulations/${encodeURIComponent(id)}/cancel`, {
    method: 'POST',
  })
}

export async function getSimulationHistory(deckName: string): Promise<SimulationHistoryEntry[]> {
  return fetchApi<SimulationHistoryEntry[]>(
    `/api/simulations/history/${encodeURIComponent(deckName)}`
  )
}

export async function deleteSimulationResult(id: string): Promise<void> {
  return fetchApi<void>(`/api/simulations/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}

export async function recalculateSimulation(id: string): Promise<SimulationProgress> {
  return fetchApi<SimulationProgress>(`/api/simulations/${encodeURIComponent(id)}/recalculate`, {
    method: 'POST',
  })
}

export async function getGameLogs(simulationId?: string): Promise<GameLogSummary[]> {
  const params = simulationId ? `?simulationId=${encodeURIComponent(simulationId)}` : ''
  return fetchApi<GameLogSummary[]>(`/api/gamelogs${params}`)
}

export async function getGameLogDetail(id: string): Promise<GameLogDetail> {
  return fetchApi<GameLogDetail>(`/api/gamelogs/${encodeURIComponent(id)}`)
}

export async function deleteGameLog(id: string): Promise<void> {
  return fetchApi<void>(`/api/gamelogs/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}
