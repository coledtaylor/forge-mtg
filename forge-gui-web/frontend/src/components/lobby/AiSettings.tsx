import { useState } from 'react'
import { ChevronRight, ChevronDown } from 'lucide-react'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select'
import type { DeckSummary } from '../../types/deck'

interface AiSettingsProps {
  difficulty: string
  onDifficultyChange: (val: string) => void
  aiDeckName: string | null
  onAiDeckNameChange: (val: string | null) => void
  availableDecks: DeckSummary[]
}

export function AiSettings({
  difficulty,
  onDifficultyChange,
  aiDeckName,
  onAiDeckNameChange,
  availableDecks,
}: AiSettingsProps) {
  const [expanded, setExpanded] = useState(false)

  const summaryText = difficulty === 'Goldfish'
    ? 'Goldfish (solitaire)'
    : `${difficulty} difficulty \u00b7 ${aiDeckName ?? 'Random'} deck`

  return (
    <div className="rounded-lg border border-border">
      <div
        className="flex items-center gap-2 px-3 py-2.5 cursor-pointer select-none hover:bg-muted transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        {expanded ? (
          <ChevronDown className="size-4 text-muted-foreground" />
        ) : (
          <ChevronRight className="size-4 text-muted-foreground" />
        )}
        <span className="text-[12px] font-normal">AI Settings</span>
        <span className="ml-auto text-[12px] text-muted-foreground truncate">
          {summaryText}
        </span>
      </div>

      <div
        className={`overflow-hidden transition-[max-height] duration-200 ease-out ${
          expanded ? 'max-h-[200px]' : 'max-h-0'
        }`}
      >
        <div className="flex gap-4 px-3 pb-3 pt-1">
          <div className="flex-1 space-y-1">
            <span className="text-[12px] text-muted-foreground">Difficulty</span>
            <Select value={difficulty} onValueChange={onDifficultyChange}>
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="Easy">Easy</SelectItem>
                <SelectItem value="Medium">Medium</SelectItem>
                <SelectItem value="Hard">Hard</SelectItem>
                <SelectItem value="Goldfish">Goldfish (Solitaire)</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {difficulty !== 'Goldfish' && <div className="flex-1 space-y-1">
            <span className="text-[12px] text-muted-foreground">AI Deck</span>
            <Select
              value={aiDeckName ?? '__random__'}
              onValueChange={(val) =>
                onAiDeckNameChange(val === '__random__' ? null : val)
              }
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="__random__">Random</SelectItem>
                {availableDecks.map((deck) => (
                  <SelectItem key={deck.name} value={deck.name}>
                    {deck.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>}
        </div>
      </div>
    </div>
  )
}
