import { useState, useCallback } from 'react'
import { useGameStore, type PromptState } from '../../stores/gameStore'
import type { GameWebSocket } from '../../lib/gameWebSocket'
import { Button } from '../ui/button'

interface ChoiceDialogProps {
  prompt: PromptState
  wsRef: React.MutableRefObject<GameWebSocket | null>
}

export function ChoiceDialog({ prompt, wsRef }: ChoiceDialogProps) {
  const setPrompt = useGameStore((s) => s.setPrompt)
  const choices = prompt.payload.choices ?? prompt.payload.options ?? []
  const isMultiSelect = (prompt.payload.max ?? 1) > 1
  const [selectedIndices, setSelectedIndices] = useState<number[]>([])

  const handleSingleSelect = useCallback(
    (index: number) => {
      wsRef.current?.sendChoiceResponse(prompt.inputId, [index])
      setPrompt(null)
    },
    [prompt.inputId, wsRef, setPrompt]
  )

  const toggleMultiSelect = useCallback(
    (index: number) => {
      setSelectedIndices((prev) =>
        prev.includes(index)
          ? prev.filter((i) => i !== index)
          : [...prev, index]
      )
    },
    []
  )

  const handleConfirmMulti = useCallback(() => {
    wsRef.current?.sendChoiceResponse(prompt.inputId, selectedIndices)
    setPrompt(null)
  }, [prompt.inputId, wsRef, selectedIndices, setPrompt])

  const handleSkip = useCallback(() => {
    wsRef.current?.sendChoiceResponse(prompt.inputId, [])
    setPrompt(null)
  }, [prompt.inputId, wsRef, setPrompt])

  // Short choice list (<=5): render as button row
  if (!isMultiSelect && choices.length <= 5) {
    return (
      <>
        <span className="text-sm text-foreground truncate shrink-0">
          {prompt.payload.message}
        </span>
        <div className="flex items-center gap-2 overflow-x-auto shrink-0">
          {choices.map((choice, i) => (
            <Button
              key={i}
              variant="outline"
              size="sm"
              onClick={() => handleSingleSelect(i)}
            >
              {choice}
            </Button>
          ))}
          {prompt.payload.isOptional && (
            <Button variant="ghost" size="sm" onClick={handleSkip}>
              Skip
            </Button>
          )}
        </div>
      </>
    )
  }

  // Long choice list (>5) or multi-select: scrollable list
  return (
    <div className="flex items-center gap-4 w-full min-w-0">
      <span className="text-sm text-foreground truncate shrink-0">
        {prompt.payload.message}
      </span>
      <div className="flex items-center gap-1 overflow-x-auto flex-1 min-w-0">
        {choices.map((choice, i) => (
          <Button
            key={i}
            variant={
              isMultiSelect && selectedIndices.includes(i)
                ? 'default'
                : 'outline'
            }
            size="sm"
            onClick={() =>
              isMultiSelect ? toggleMultiSelect(i) : handleSingleSelect(i)
            }
            className="shrink-0"
          >
            {choice}
          </Button>
        ))}
      </div>
      <div className="flex items-center gap-2 shrink-0">
        {isMultiSelect && (
          <Button
            variant="default"
            size="sm"
            onClick={handleConfirmMulti}
            disabled={
              selectedIndices.length < (prompt.payload.min ?? 0)
            }
          >
            Confirm
          </Button>
        )}
        {prompt.payload.isOptional && (
          <Button variant="ghost" size="sm" onClick={handleSkip}>
            Skip
          </Button>
        )}
      </div>
    </div>
  )
}
