import { useState, useMemo } from 'react'
import { Loader2 } from 'lucide-react'
import { useDecks } from '../../hooks/useDecks'
import { Card, CardContent } from '../ui/card'
import { Button } from '../ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select'
import { DeckPicker } from './DeckPicker'
import { AiSettings } from './AiSettings'

export interface GameStartConfig {
  deckName: string
  aiDeckName: string | null
  format: string
  aiDifficulty: string
}

interface GameLobbyProps {
  preSelectedDeck?: string
  preSelectedFormat?: string
  onStartGame: (
    gameId: string,
    deckName: string,
    format: string,
    gameConfig: GameStartConfig
  ) => void
  onBack: () => void
}

const FORMAT_OPTIONS = [
  { value: 'Commander', label: 'Commander' },
  { value: 'Standard', label: 'Standard' },
  { value: 'Casual 60-card', label: 'Casual 60-card' },
  { value: 'Jumpstart', label: 'Jumpstart' },
]

function matchesFormat(deckFormat: string, selectedFormat: string): boolean {
  const norm = selectedFormat.toLowerCase()
  const df = (deckFormat ?? '').toLowerCase()

  if (norm === 'casual 60-card') {
    return df === '' || df === 'constructed' || df === 'casual 60-card'
  }
  return df === norm
}

export function GameLobby({
  preSelectedDeck,
  preSelectedFormat,
  onStartGame,
  onBack,
}: GameLobbyProps) {
  const [selectedFormat, setSelectedFormat] = useState(preSelectedFormat ?? '')
  const [selectedDeck, setSelectedDeck] = useState<string | null>(
    preSelectedDeck ?? null
  )
  const [aiDifficulty, setAiDifficulty] = useState('Medium')
  const [aiDeckName, setAiDeckName] = useState<string | null>(null)
  const [isStarting, setIsStarting] = useState(false)

  const { data: allDecks, isLoading } = useDecks()

  const filteredDecks = useMemo(() => {
    if (!allDecks || !selectedFormat) return []
    return allDecks.filter((d) => matchesFormat(d.format, selectedFormat))
  }, [allDecks, selectedFormat])

  // Clear selected deck when format changes and selected deck is not in filtered list
  const handleFormatChange = (format: string) => {
    setSelectedFormat(format)
    if (selectedDeck && allDecks) {
      const stillAvailable = allDecks.some(
        (d) => d.name === selectedDeck && matchesFormat(d.format, format)
      )
      if (!stillAvailable) {
        setSelectedDeck(null)
      }
    }
  }

  const handleStartGame = () => {
    if (!selectedDeck || !selectedFormat) return
    setIsStarting(true)
    const gameId = crypto.randomUUID()
    onStartGame(gameId, selectedDeck, selectedFormat, {
      deckName: selectedDeck,
      aiDeckName,
      format: selectedFormat,
      aiDifficulty,
    })
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="max-w-[480px] mx-auto mt-12 px-4">
        <Card className="p-8">
          <CardContent className="space-y-4 p-0">
            <h2 className="text-[20px] font-semibold">Play a Game</h2>

            <div className="space-y-1">
              <span className="text-[12px] font-normal text-muted-foreground">
                Format
              </span>
              <Select value={selectedFormat} onValueChange={handleFormatChange}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select a format" />
                </SelectTrigger>
                <SelectContent>
                  {FORMAT_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {selectedFormat && (
              <>
                <DeckPicker
                  decks={filteredDecks}
                  selectedDeck={selectedDeck}
                  onSelect={setSelectedDeck}
                  isLoading={isLoading}
                  formatLabel={selectedFormat}
                  onCreateDeck={onBack}
                />

                <div className="pt-2">
                  <AiSettings
                    difficulty={aiDifficulty}
                    onDifficultyChange={setAiDifficulty}
                    aiDeckName={aiDeckName}
                    onAiDeckNameChange={setAiDeckName}
                    availableDecks={filteredDecks}
                  />
                </div>

                <Button
                  variant="default"
                  className="w-full mt-2"
                  disabled={!selectedDeck || isStarting}
                  onClick={handleStartGame}
                >
                  {isStarting ? (
                    <>
                      <Loader2 className="size-4 animate-spin" />
                      Starting...
                    </>
                  ) : (
                    'Start Game'
                  )}
                </Button>
              </>
            )}
          </CardContent>
        </Card>

        <button
          className="w-full mt-4 text-center text-sm text-muted-foreground hover:text-foreground transition-colors"
          onClick={onBack}
        >
          Back to Deck Builder
        </button>
      </div>
    </div>
  )
}
