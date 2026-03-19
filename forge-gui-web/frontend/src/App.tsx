import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useCardSearch } from './hooks/useCardSearch'
import { SearchBar } from './components/SearchBar'
import { CardGrid } from './components/CardGrid'
import { PaginationBar } from './components/PaginationBar'
import { DeckList } from './components/DeckList'
import type { CardSearchParams } from './types/card'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 min -- card data rarely changes
      retry: 1,
    },
  },
})

function AppContent() {
  const [searchParams, setSearchParams] = useState<CardSearchParams>({})
  const [page, setPage] = useState(1)
  const [hasSearched, setHasSearched] = useState(false)

  const { data, isLoading, isError } = useCardSearch(
    { ...searchParams, page, limit: 20 },
    hasSearched
  )

  const handleSearch = (params: CardSearchParams) => {
    setSearchParams(params)
    setPage(1)
    setHasSearched(true)
  }

  const handlePageChange = (newPage: number) => {
    setPage(newPage)
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="max-w-[1200px] mx-auto px-6 py-12">
        <h1 className="text-[28px] font-semibold text-foreground">Forge</h1>

        <div className="mt-8">
          <SearchBar onSearch={handleSearch} />
        </div>

        {hasSearched && (
          <>
            <div className="mt-4 flex items-center justify-between">
              <span className="text-[14px] text-muted-foreground">
                {isLoading ? 'Searching...' : isError ? '' : `${data?.total ?? 0} cards found`}
              </span>
              {data && data.totalPages > 1 && (
                <PaginationBar
                  page={data.page}
                  totalPages={data.totalPages}
                  onPageChange={handlePageChange}
                />
              )}
            </div>

            <div className="mt-4">
              {isError ? (
                <div className="flex flex-col items-center justify-center py-16">
                  <p className="text-[14px] text-destructive">
                    Could not load search results. Check that the server is running and try again.
                  </p>
                </div>
              ) : (
                <CardGrid
                  cards={data?.cards ?? []}
                  isLoading={isLoading}
                />
              )}
            </div>

            {data && data.totalPages > 1 && (
              <div className="mt-6">
                <PaginationBar
                  page={data.page}
                  totalPages={data.totalPages}
                  onPageChange={handlePageChange}
                />
              </div>
            )}
          </>
        )}

        <div className="mt-12 border-t border-border pt-8">
          <DeckList />
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
