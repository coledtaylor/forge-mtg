import { useState, useEffect } from 'react'
import { Button } from '../ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../ui/tabs'
import { X, RotateCcw } from 'lucide-react'
import { useSimulation } from '../../hooks/useSimulation'
import { SimulationConfig } from './SimulationConfig'
import { SimulationProgress } from './SimulationProgress'
import { OverviewTab } from './OverviewTab'
import { MatchupsTab } from './MatchupsTab'
import type { SimulationConfig as SimulationConfigType } from '../../lib/simulation-types'

type SimState = 'config' | 'running' | 'results'

interface SimulationPanelProps {
  deckName: string
  format: string
  onClose: () => void
}

export function SimulationPanel({ deckName, format, onClose }: SimulationPanelProps) {
  const { startSim, cancelSim, progress, isRunning } = useSimulation(deckName)
  const [state, setState] = useState<SimState>('config')

  // Transition to results when simulation finishes
  useEffect(() => {
    if (state === 'running' && progress && !isRunning) {
      if (progress.status === 'complete' || progress.status === 'cancelled') {
        setState('results')
      }
    }
  }, [state, progress, isRunning])

  async function handleStart(config: SimulationConfigType) {
    setState('running')
    await startSim(config)
  }

  function handleRunAgain() {
    setState('config')
  }

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
          <SimulationConfig
            deckName={deckName}
            format={format}
            onStart={handleStart}
          />
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

        {state === 'results' && progress && (
          <div className="flex flex-col gap-4 px-4 pb-4">
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
              </TabsList>

              <TabsContent value="overview">
                <OverviewTab data={progress} />
              </TabsContent>

              <TabsContent value="matchups">
                <MatchupsTab
                  matchups={progress.matchups}
                  winRateOnPlay={progress.winRateOnPlay}
                  winRateOnDraw={progress.winRateOnDraw}
                />
              </TabsContent>

              <TabsContent value="performance">
                <p className="py-4 text-sm text-muted-foreground">Per-card performance stats</p>
              </TabsContent>

              <TabsContent value="mana">
                <p className="py-4 text-sm text-muted-foreground">Mana statistics</p>
              </TabsContent>
            </Tabs>
          </div>
        )}
      </div>
    </div>
  )
}
