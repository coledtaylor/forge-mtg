import { useRef, useEffect } from 'react'
import { useGameStore, type PromptState } from '../../stores/gameStore'
import type { GameWebSocket } from '../../lib/gameWebSocket'
import { Button } from '../ui/button'
import { ChoiceDialog } from './ChoiceDialog'

interface ActionBarProps {
  wsRef: React.MutableRefObject<GameWebSocket | null>
  className?: string
}

function ConfirmPrompt({
  prompt,
  wsRef,
}: {
  prompt: PromptState
  wsRef: React.MutableRefObject<GameWebSocket | null>
}) {
  const setPrompt = useGameStore((s) => s.setPrompt)

  const yesLabel = prompt.payload.yesButton ?? 'Yes'
  const noLabel = prompt.payload.noButton ?? 'No'

  const handleYes = () => {
    wsRef.current?.sendConfirmResponse(prompt.inputId, true)
    setPrompt(null)
  }

  const handleNo = () => {
    wsRef.current?.sendConfirmResponse(prompt.inputId, false)
    setPrompt(null)
  }

  return (
    <>
      <span className="text-sm text-foreground truncate">
        {prompt.payload.message}
      </span>
      <div className="flex items-center gap-2 shrink-0">
        <Button variant="default" size="sm" onClick={handleYes}>
          {yesLabel}
        </Button>
        <Button variant="outline" size="sm" onClick={handleNo}>
          {noLabel}
        </Button>
      </div>
    </>
  )
}

function AmountInput({
  prompt,
  wsRef,
}: {
  prompt: PromptState
  wsRef: React.MutableRefObject<GameWebSocket | null>
}) {
  const setPrompt = useGameStore((s) => s.setPrompt)
  const min = prompt.payload.min ?? 0
  const max = prompt.payload.max ?? 99
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  const handleOk = () => {
    const value = Number(inputRef.current?.value ?? min)
    const clamped = Math.max(min, Math.min(max, value))
    wsRef.current?.sendAmountResponse(prompt.inputId, clamped)
    setPrompt(null)
  }

  return (
    <>
      <span className="text-sm text-foreground truncate">
        {prompt.payload.message}
      </span>
      <div className="flex items-center gap-2 shrink-0">
        <input
          ref={inputRef}
          type="number"
          min={min}
          max={max}
          defaultValue={prompt.payload.amount ?? min}
          className="w-16 h-7 rounded border border-border bg-background px-2 text-sm text-foreground"
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleOk()
          }}
        />
        <Button variant="default" size="sm" onClick={handleOk}>
          OK
        </Button>
      </div>
    </>
  )
}

export function ActionBar({ wsRef, className }: ActionBarProps) {
  const prompt = useGameStore((s) => s.prompt)
  const buttons = useGameStore((s) => s.buttons)
  const okBtnRef = useRef<HTMLButtonElement>(null)

  // Auto-focus button 1 when focus1 is true
  useEffect(() => {
    if (buttons?.focus1) {
      okBtnRef.current?.focus()
    }
  }, [buttons])

  // Button mode
  if (buttons !== null) {
    // If a prompt is active alongside buttons, render the prompt UI
    if (prompt !== null) {
      if (prompt.type === 'PROMPT_CHOICE') {
        return (
          <div
            className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
          >
            <ChoiceDialog prompt={prompt} wsRef={wsRef} />
          </div>
        )
      }

      if (prompt.type === 'PROMPT_CONFIRM') {
        return (
          <div
            className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
          >
            <ConfirmPrompt prompt={prompt} wsRef={wsRef} />
          </div>
        )
      }

      if (prompt.type === 'PROMPT_AMOUNT') {
        return (
          <div
            className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
          >
            <AmountInput prompt={prompt} wsRef={wsRef} />
          </div>
        )
      }
    }

    return (
      <div
        className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
      >
        <span className="text-sm text-foreground truncate">
          {prompt?.payload.message ?? 'Priority'}
        </span>
        <div className="flex items-center gap-2 shrink-0">
          <Button
            ref={okBtnRef}
            variant="default"
            size="sm"
            disabled={!buttons.enable1}
            onClick={() => wsRef.current?.sendButtonOk()}
          >
            {buttons.label1}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={!buttons.enable2}
            onClick={() => wsRef.current?.sendButtonCancel()}
          >
            {buttons.label2}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => wsRef.current?.sendButtonOk()}
          >
            Pass Priority
          </Button>
        </div>
      </div>
    )
  }

  // Prompt mode (no buttons)
  if (prompt !== null) {
    if (prompt.type === 'PROMPT_CHOICE') {
      return (
        <div
          className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
        >
          <ChoiceDialog prompt={prompt} wsRef={wsRef} />
        </div>
      )
    }

    if (prompt.type === 'PROMPT_CONFIRM') {
      return (
        <div
          className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
        >
          <ConfirmPrompt prompt={prompt} wsRef={wsRef} />
        </div>
      )
    }

    if (prompt.type === 'PROMPT_AMOUNT') {
      return (
        <div
          className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 border-t-2 border-primary ${className ?? ''}`}
        >
          <AmountInput prompt={prompt} wsRef={wsRef} />
        </div>
      )
    }
  }

  // Idle state
  return (
    <div
      className={`h-[44px] bg-card flex items-center justify-center px-4 gap-4 border-t border-border ${className ?? ''}`}
    >
      <span className="text-sm text-muted-foreground">
        Waiting for opponent...
      </span>
    </div>
  )
}
