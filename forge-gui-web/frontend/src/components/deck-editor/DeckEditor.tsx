// Stub -- full implementation in Task 2
interface DeckEditorProps {
  deckName: string
  onBack: () => void
}

export function DeckEditor({ deckName, onBack }: DeckEditorProps) {
  return (
    <div className="min-h-screen bg-background text-foreground flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-[20px] font-semibold">{deckName}</h1>
        <button onClick={onBack} className="mt-4 text-muted-foreground hover:text-foreground">
          Back
        </button>
      </div>
    </div>
  )
}
