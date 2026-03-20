import { useRef, useEffect } from 'react'
import { useGameStore, type PromptState } from '../../stores/gameStore'
import type { GameWebSocket } from '../../lib/gameWebSocket'
import { Button } from '../ui/button'
import { ChoiceDialog } from './ChoiceDialog'

const priorityPulseStyle = `
@keyframes priority-pulse {
  0%, 100% { box-shadow: 0 -2px 8px 0 hsl(var(--primary) / 0.3); }
  50% { box-shadow: 0 -2px 16px 0 hsl(var(--primary) / 0.7); }
}
`

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
  const hasPriority = useGameStore((s) => s.hasPriority)
  const okBtnRef = useRef<HTMLButtonElement>(null)

  // Auto-focus button 1 when focus1 is true
  useEffect(() => {
    if (buttons?.focus1) {
      okBtnRef.current?.focus()
    }
  }, [buttons])

  // Determine confirm vs pass mode
  const isConfirmMode = prompt !== null
  const primaryLabel = isConfirmMode ? 'Confirm' : 'Pass'
  const primaryShortcut = isConfirmMode ? '' : '[Space]'

  // Priority wrapper classes
  const priorityClasses = hasPriority
    ? 'border-t-2 border-primary'
    : 'border-t border-border opacity-60'

  const priorityStyle = hasPriority
    ? { animation: 'priority-pulse 2s ease-in-out infinite' }
    : undefined

  // Render prompt-specific content inside priority wrapper
  const renderPromptContent = () => {
    if (!prompt) return null

    if (prompt.type === 'PROMPT_CHOICE') {
      return <ChoiceDialog prompt={prompt} wsRef={wsRef} />
    }

    if (prompt.type === 'PROMPT_CONFIRM') {
      return <ConfirmPrompt prompt={prompt} wsRef={wsRef} />
    }

    if (prompt.type === 'PROMPT_AMOUNT') {
      return <AmountInput prompt={prompt} wsRef={wsRef} />
    }

    return null
  }

  // Button mode: player has priority
  if (buttons !== null) {
    // If a prompt is active alongside buttons, render the prompt UI in the priority wrapper
    if (prompt !== null) {
      const promptContent = renderPromptContent()
      if (promptContent) {
        return (
          <>
            <style>{priorityPulseStyle}</style>
            <div
              className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 ${priorityClasses} ${className ?? ''}`}
              style={priorityStyle}
            >
              {promptContent}
            </div>
          </>
        )
      }
    }

    // Standard button layout: priority indicator with Confirm/Pass split
    return (
      <>
        <style>{priorityPulseStyle}</style>
        <div
          className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 ${priorityClasses} ${className ?? ''}`}
          style={priorityStyle}
        >
          {/* Left: status text */}
          <span className="text-sm text-foreground truncate">
            You have priority
          </span>

          {/* Right: buttons */}
          <div className="flex items-center gap-2 shrink-0">
            {/* Primary action: Confirm (green) or Pass (muted) */}
            <Button
              ref={okBtnRef}
              variant={isConfirmMode ? 'default' : 'secondary'}
              size="sm"
              disabled={!buttons.enable1}
              onClick={() => wsRef.current?.sendButtonOk()}
            >
              {primaryLabel}
              {primaryShortcut && (
                <span className="text-xs opacity-60 ml-1">{primaryShortcut}</span>
              )}
            </Button>

            {/* Cancel button (when enabled) */}
            {buttons.enable2 && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => wsRef.current?.sendButtonCancel()}
              >
                Cancel <span className="text-xs opacity-60 ml-1">[Esc]</span>
              </Button>
            )}
          </div>
        </div>
      </>
    )
  }

  // Prompt mode (no buttons) -- still show with priority styling if hasPriority
  if (prompt !== null) {
    const promptContent = renderPromptContent()
    if (promptContent) {
      return (
        <>
          <style>{priorityPulseStyle}</style>
          <div
            className={`h-[44px] bg-card flex items-center justify-between px-4 gap-4 ${priorityClasses} ${className ?? ''}`}
            style={priorityStyle}
          >
            {promptContent}
          </div>
        </>
      )
    }
  }

  // Idle state: no priority, no buttons
  return (
    <div
      className={`h-[44px] bg-card flex items-center justify-center px-4 gap-4 border-t border-border opacity-60 ${className ?? ''}`}
    >
      <span className="text-sm text-muted-foreground">
        Waiting for opponent...
      </span>
    </div>
  )
}
