import { useState, useMemo } from 'react'
import { Button } from '../ui/button'
import { useDecks } from '../../hooks/useDecks'
import { ChevronDown, ChevronRight, Play } from 'lucide-react'
import type { SimulationConfig as SimulationConfigType } from '../../lib/simulation-types'

const GAME_COUNTS = [10, 50, 100, 500] as const
type GameCount = (typeof GAME_COUNTS)[number]

const SPEED_OPTIONS = [
  { label: 'Quick', value: 'Default' as const },
  { label: 'Thorough', value: 'Reckless' as const },
]
type AiProfile = 'Reckless' | 'Default'

interface SimulationConfigProps {
  deckName: string
  format: string
  onStart: (config: SimulationConfigType) => void
}

export function SimulationConfig({ deckName, format, onStart }: SimulationConfigProps) {
  const [aiProfile, setAiProfile] = useState<AiProfile>('Reckless')
  const [gameCount, setGameCount] = useState<GameCount>(50)
  const [gauntletExpanded, setGauntletExpanded] = useState(false)
  const [selectedOpponents, setSelectedOpponents] = useState<Set<string>>(new Set())
  const { data: decks } = useDecks()

  const availableOpponents = useMemo(() => {
    if (!decks) return []
    return decks
      .filter((d) => d.format === format && d.name !== deckName)
      .sort((a, b) => a.name.localeCompare(b.name))
  }, [decks, format, deckName])

  const opponentCount = gauntletExpanded && selectedOpponents.size > 0
    ? selectedOpponents.size
    : availableOpponents.length

  function handleSelectAll() {
    setSelectedOpponents(new Set(availableOpponents.map((d) => d.name)))
  }

  function handleDeselectAll() {
    setSelectedOpponents(new Set())
  }

  function toggleOpponent(name: string) {
    setSelectedOpponents((prev) => {
      const next = new Set(prev)
      if (next.has(name)) {
        next.delete(name)
      } else {
        next.add(name)
      }
      return next
    })
  }

  function handleStart() {
    const config: SimulationConfigType = {
      deckName,
      gameCount,
      aiProfile,
    }
    if (gauntletExpanded && selectedOpponents.size > 0) {
      config.opponentDeckNames = Array.from(selectedOpponents)
    }
    onStart(config)
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <div>
        <h3 className="text-sm font-medium text-muted-foreground mb-2">Speed</h3>
        <div className="flex gap-1">
          {SPEED_OPTIONS.map((option) => (
            <button
              key={option.value}
              onClick={() => setAiProfile(option.value)}
              className={`flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                aiProfile === option.value
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
        <p className="text-xs text-muted-foreground/70 mt-1">
          Quick runs faster but with less precise AI play.
        </p>
      </div>

      <div>
        <h3 className="text-sm font-medium text-muted-foreground mb-2">Number of Games</h3>
        <div className="flex gap-1">
          {GAME_COUNTS.map((count) => (
            <button
              key={count}
              onClick={() => setGameCount(count)}
              className={`flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                gameCount === count
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              {count}
            </button>
          ))}
        </div>
      </div>

      <div>
        <button
          onClick={() => setGauntletExpanded(!gauntletExpanded)}
          className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          {gauntletExpanded ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" />}
          Configure Gauntlet
        </button>

        {gauntletExpanded && (
          <div className="mt-2 rounded-md border border-border bg-muted/30 p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-muted-foreground">
                {availableOpponents.length} deck{availableOpponents.length !== 1 ? 's' : ''} available
              </span>
              <div className="flex gap-1">
                <Button variant="ghost" size="xs" onClick={handleSelectAll}>
                  Select All
                </Button>
                <Button variant="ghost" size="xs" onClick={handleDeselectAll}>
                  Deselect All
                </Button>
              </div>
            </div>

            <div className="max-h-48 overflow-y-auto space-y-0.5">
              {availableOpponents.length === 0 ? (
                <p className="text-xs text-muted-foreground py-2 text-center">
                  No other {format} decks found
                </p>
              ) : (
                availableOpponents.map((deck) => (
                  <label
                    key={deck.name}
                    className="flex items-center gap-2 rounded px-2 py-1 text-sm hover:bg-muted/50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedOpponents.has(deck.name)}
                      onChange={() => toggleOpponent(deck.name)}
                      className="rounded border-border"
                    />
                    <span className="truncate">{deck.name}</span>
                  </label>
                ))
              )}
            </div>
          </div>
        )}
      </div>

      <p className="text-xs text-muted-foreground">
        Testing against {opponentCount} opponent deck{opponentCount !== 1 ? 's' : ''}
      </p>

      <Button onClick={handleStart} className="w-full gap-2">
        <Play className="size-4" />
        Start Simulation
      </Button>
    </div>
  )
}
