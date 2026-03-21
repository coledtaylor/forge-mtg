import { useState, useCallback, useRef, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { startSimulation, cancelSimulation, getSimulationHistory } from '../api/simulation'
import type { SimulationConfig, SimulationProgress, SimulationHistoryEntry } from '../lib/simulation-types'

export function useSimulation(deckName: string) {
  const queryClient = useQueryClient()
  const [progress, setProgress] = useState<SimulationProgress | null>(null)
  const [isRunning, setIsRunning] = useState(false)
  const eventSourceRef = useRef<EventSource | null>(null)
  const activeSimIdRef = useRef<string | null>(null)

  // Clean up EventSource
  const closeEventSource = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
      eventSourceRef.current = null
    }
    activeSimIdRef.current = null
  }, [])

  // Clean up on unmount
  useEffect(() => {
    return () => {
      closeEventSource()
    }
  }, [closeEventSource])

  const startSim = useCallback(async (config: SimulationConfig) => {
    // Close any existing SSE connection
    closeEventSource()
    setIsRunning(true)
    setProgress(null)

    const { id } = await startSimulation(config)
    activeSimIdRef.current = id

    // Open SSE connection for live progress
    const es = new EventSource(`/api/simulations/${encodeURIComponent(id)}/progress`)
    eventSourceRef.current = es

    es.addEventListener('progress', (event) => {
      try {
        const data = JSON.parse(event.data) as SimulationProgress
        setProgress({ ...data, status: 'running' })
      } catch {
        // Ignore malformed events
      }
    })

    es.addEventListener('complete', (event) => {
      try {
        const data = JSON.parse(event.data) as SimulationProgress
        setProgress({ ...data, status: data.cancelled ? 'cancelled' : 'complete' })
      } catch {
        // Ignore malformed events
      }
      setIsRunning(false)
      closeEventSource()
      // Refresh history after completion
      queryClient.invalidateQueries({ queryKey: ['simulation-history', deckName] })
    })

    es.addEventListener('error', (event) => {
      try {
        const data = JSON.parse((event as MessageEvent).data)
        if (data?.error) {
          setIsRunning(false)
          closeEventSource()
        }
      } catch {
        // SSE connection error -- may reconnect automatically
      }
    })

    es.onerror = () => {
      // Connection lost
      setIsRunning(false)
      closeEventSource()
    }

    return id
  }, [closeEventSource, deckName, queryClient])

  const cancelSim = useCallback(async () => {
    if (activeSimIdRef.current) {
      await cancelSimulation(activeSimIdRef.current)
    }
  }, [])

  const history = useQuery<SimulationHistoryEntry[]>({
    queryKey: ['simulation-history', deckName],
    queryFn: () => getSimulationHistory(deckName),
    enabled: !!deckName,
    staleTime: 30_000,
  })

  const refreshHistory = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['simulation-history', deckName] })
  }, [queryClient, deckName])

  return {
    startSim,
    cancelSim,
    progress,
    isRunning,
    history,
    refreshHistory,
  }
}
