import { useEffect, useRef } from 'react'
import { GameWebSocket } from '../lib/gameWebSocket'
import { useGameStore } from '../stores/gameStore'

export function useGameWebSocket(gameId: string | null) {
  const wsRef = useRef<GameWebSocket | null>(null)

  useEffect(() => {
    if (!gameId) return

    // Guard against StrictMode double-mount (Pitfall 3)
    if (wsRef.current !== null) return

    const ws = new GameWebSocket(gameId)
    wsRef.current = ws
    ws.connect()

    return () => {
      ws.disconnect()
      wsRef.current = null
      useGameStore.getState().reset()
    }
  }, [gameId])

  return wsRef
}
