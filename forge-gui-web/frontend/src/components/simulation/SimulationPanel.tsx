import { useState, useEffect, useCallback } from 'react'
import { Button } from '../ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../ui/tabs'
import { X, RotateCcw, ArrowLeft } from 'lucide-react'
import { useSimulation } from '../../hooks/useSimulation'
import { SimulationConfig } from './SimulationConfig'
import { SimulationProgress } from './SimulationProgress'
import { OverviewTab } from './OverviewTab'
import { MatchupsTab } from './MatchupsTab'
import { PerformanceTab } from './PerformanceTab'
import { ManaTab } from './ManaTab'
import { SimulationHistory } from './SimulationHistory'
import { GameLogsTab } from './GameLogsTab'
import { getSimulationStatus, deleteSimulationResult } from '../../api/simulation'
import type {
  SimulationConfig as SimulationConfigType,
  SimulationProgress as SimulationProgressType,
} from '../../lib/simulation-types'

type SimState = 'config' | 'running' | 'results'

interface SimulationPanelProps {
  deckName: string
  format: string
  onClose: () => void
}

export function SimulationPanel({ deckName, format, onClose }: SimulationPanelProps) {
  const { startSim, cancelSim, progress, isRunning, history, refreshHistory } = useSimulation(deckName)
  const [state, setState] = useState<SimState>('config')
  const [selectedHistoryId, setSelectedHistoryId] = useState<string | null>(null)
  const [historicalData, setHistoricalData] = useState<SimulationProgressType | null>(null)
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Transition to results when simulation finishes
  useEffect(() => {
    if (state === 'running' && progress && !isRunning) {
      if (progress.status === 'complete' || progress.status === 'cancelled') {
        setState('results')
      }
    }
  }, [state, progress, isRunning])

  async function handleStart(config: SimulationConfigType) {
    setSelectedHistoryId(null)
    setHistoricalData(null)
    setState('running')
    await startSim(config)
  }

  function handleRunAgain() {
    setSelectedHistoryId(null)
    setHistoricalData(null)
    setState('config')
  }

  const handleSelectHistory = useCallback(async (id: string) => {
    setLoadingHistory(true)
    try {
      const data = await getSimulationStatus(id)
      setSelectedHistoryId(id)
      setHistoricalData(data)
      setState('results')
    } catch {
      // Failed to load historical result
    } finally {
      setLoadingHistory(false)
    }
  }, [])

  const handleDeleteHistory = useCallback(async (id: string) => {
    try {
      await deleteSimulationResult(id)
      refreshHistory()
      // If viewing the deleted result, go back to latest
      if (selectedHistoryId === id) {
        setSelectedHistoryId(null)
        setHistoricalData(null)
        if (!progress) setState('config')
      }
    } catch {
      // Failed to delete
    }
  }, [refreshHistory, selectedHistoryId, progress])

  function handleBackToLatest() {
    setSelectedHistoryId(null)
    setHistoricalData(null)
    if (progress) {
      setState('results')
    } else {
      setState('config')
    }
  }

  // The data to display in the tabs
  const displayData = selectedHistoryId && historicalData ? historicalData : progress

  // Find the timestamp for the selected history entry
  const selectedEntry = selectedHistoryId && history.data
    ? history.data.find((e) => e.id === selectedHistoryId)
    : null

  return (
    <div className="flex flex-col h-full border border-border rounded-lg overflow-hidden bg-background">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-border shrink-0">
        <h2 className="text-sm font-semibold">Deck Simulation</h2>
        <Button variant="ghost" size="icon-xs" onClick={onClose} title="Close simulation">
          <X className="size-4" />
        </Button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {state === 'config' && (
          <div className="flex flex-col gap-4">
            <SimulationConfig
              deckName={deckName}
              format={format}
              onStart={handleStart}
            />

            {/* History section on config screen */}
            {history.data && history.data.length > 0 && (
              <div className="px-4 pb-4 space-y-2">
                <h3 className="text-sm font-semibold text-muted-foreground">Past Results</h3>
                <SimulationHistory
                  history={history.data}
                  onSelect={handleSelectHistory}
                  onDelete={handleDeleteHistory}
                />
              </div>
            )}

            {loadingHistory && (
              <div className="flex items-center justify-center p-4 text-sm text-muted-foreground">
                Loading result...
              </div>
            )}
          </div>
        )}

        {state === 'running' && progress && (
          <SimulationProgress
            progress={progress}
            onCancel={cancelSim}
          />
        )}

        {state === 'running' && !progress && (
          <div className="flex items-center justify-center p-8 text-sm text-muted-foreground">
            Starting simulation...
          </div>
        )}

        {state === 'results' && displayData && (
          <div className="flex flex-col gap-4 px-4 pb-4">
            {/* Historical result banner */}
            {selectedHistoryId && selectedEntry && (
              <div className="flex items-center gap-2 rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-3 py-2 mt-2">
                <p className="text-sm text-yellow-600 dark:text-yellow-400 flex-1">
                  Viewing saved result from {new Date(selectedEntry.timestamp).toLocaleString()}
                </p>
                <Button variant="ghost" size="sm" onClick={handleBackToLatest} className="gap-1.5 shrink-0">
                  <ArrowLeft className="size-3.5" />
                  Back to latest
                </Button>
              </div>
            )}

            <div className="flex items-center justify-end pt-2">
              <Button variant="outline" onClick={handleRunAgain} className="gap-2">
                <RotateCcw className="size-4" />
                Run Again
              </Button>
            </div>

            <Tabs defaultValue="overview">
              <TabsList>
                <TabsTrigger value="overview">Overview</TabsTrigger>
                <TabsTrigger value="matchups">Matchups</TabsTrigger>
                <TabsTrigger value="performance">Performance</TabsTrigger>
                <TabsTrigger value="mana">Mana</TabsTrigger>
                <TabsTrigger value="gamelogs">Game Logs</TabsTrigger>
                <TabsTrigger value="history">History</TabsTrigger>
              </TabsList>

              <TabsContent value="overview">
                <OverviewTab data={displayData} />
              </TabsContent>

              <TabsContent value="matchups">
                <MatchupsTab
                  matchups={displayData.matchups}
                  winRateOnPlay={displayData.winRateOnPlay}
                  winRateOnDraw={displayData.winRateOnDraw}
                />
              </TabsContent>

              <TabsContent value="performance">
                <PerformanceTab
                  cardPerformance={displayData.cardPerformance}
                  totalGames={displayData.gamesCompleted}
                />
              </TabsContent>

              <TabsContent value="mana">
                <ManaTab data={displayData} />
              </TabsContent>

              <TabsContent value="gamelogs">
                <GameLogsTab simulationId={selectedHistoryId ?? undefined} />
              </TabsContent>

              <TabsContent value="history">
                {history.data ? (
                  <SimulationHistory
                    history={history.data}
                    onSelect={handleSelectHistory}
                    onDelete={handleDeleteHistory}
                  />
                ) : (
                  <p className="text-sm text-muted-foreground py-4">Loading history...</p>
                )}
              </TabsContent>
            </Tabs>
          </div>
        )}
      </div>
    </div>
  )
}
