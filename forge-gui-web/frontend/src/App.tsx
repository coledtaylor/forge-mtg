import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DeckList } from './components/DeckList'
import { DeckEditor } from './components/deck-editor/DeckEditor'
import { GameBoard } from './components/game/GameBoard'
import { GameLobby, type GameStartConfig } from './components/lobby/GameLobby'
import { Button } from './components/ui/button'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 min -- card data rarely changes
      retry: 1,
    },
  },
})

type View =
  | { type: 'list' }
  | { type: 'editor'; deckName: string; format?: string }
  | { type: 'lobby'; preSelectedDeck?: string; preSelectedFormat?: string }
  | {
      type: 'game'
      gameId: string
      gameConfig?: GameStartConfig
      returnState?: { deckName: string; format: string }
    }

function AppContent() {
  const [view, setView] = useState<View>({ type: 'list' })

  if (view.type === 'game') {
    return (
      <GameBoard
        gameId={view.gameId}
        gameConfig={view.gameConfig}
        onExit={() => {
          if (view.returnState) {
            setView({
              type: 'lobby',
              preSelectedDeck: view.returnState.deckName,
              preSelectedFormat: view.returnState.format,
            })
          } else {
            setView({ type: 'list' })
          }
        }}
      />
    )
  }

  if (view.type === 'lobby') {
    return (
      <GameLobby
        preSelectedDeck={view.preSelectedDeck}
        preSelectedFormat={view.preSelectedFormat}
        onStartGame={(gameId, deckName, format, gameConfig) =>
          setView({
            type: 'game',
            gameId,
            gameConfig,
            returnState: { deckName, format },
          })
        }
        onBack={() => setView({ type: 'list' })}
      />
    )
  }

  if (view.type === 'editor') {
    return (
      <DeckEditor
        deckName={view.deckName}
        format={view.format}
        onBack={() => setView({ type: 'list' })}
        onPlayDeck={() => setView({
          type: 'lobby',
          preSelectedDeck: view.deckName,
          preSelectedFormat: view.format,
        })}
      />
    )
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="max-w-[1200px] mx-auto px-6 py-12">
        <div className="flex items-center justify-between">
          <h1 className="text-[28px] font-semibold text-foreground">Forge</h1>
          <Button
            variant="default"
            onClick={() => setView({ type: 'lobby' })}
          >
            Play a Game
          </Button>
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
