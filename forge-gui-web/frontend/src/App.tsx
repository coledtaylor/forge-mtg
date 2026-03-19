import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DeckList } from './components/DeckList'
import { DeckEditor } from './components/deck-editor/DeckEditor'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 min -- card data rarely changes
      retry: 1,
    },
  },
})

type View = { type: 'list' } | { type: 'editor'; deckName: string; format?: string }

function AppContent() {
  const [view, setView] = useState<View>({ type: 'list' })

  if (view.type === 'editor') {
    return (
      <DeckEditor
        deckName={view.deckName}
        format={view.format}
        onBack={() => setView({ type: 'list' })}
      />
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="max-w-[1200px] mx-auto px-6 py-12">
        <div className="flex items-center justify-between">
          <h1 className="text-[28px] font-semibold text-foreground">Forge</h1>
        </div>
        <div className="mt-8">
          <DeckList onEditDeck={(name, format) => setView({ type: 'editor', deckName: name, format })} />
        </div>
      </div>
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  )
}

export default App
