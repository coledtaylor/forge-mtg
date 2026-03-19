import { useState } from 'react'
import { SearchBar } from '../SearchBar'
import { PaginationBar } from '../PaginationBar'
import { Skeleton } from '../ui/skeleton'
import { useCardSearch } from '../../hooks/useCardSearch'
import { getScryfallImageUrl } from '../../lib/scryfall'
import type { CardSearchParams, CardSearchResult } from '../../types/card'

interface CardSearchPanelProps {
  onCardClick: (card: CardSearchResult) => void
  onCardMouseEnter: (card: CardSearchResult, e: React.MouseEvent) => void
  onCardMouseMove: (e: React.MouseEvent) => void
  onCardMouseLeave: () => void
  deckCardNames?: Set<string>
}

export function CardSearchPanel({
  onCardClick, onCardMouseEnter, onCardMouseMove, onCardMouseLeave, deckCardNames,
}: CardSearchPanelProps) {
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

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="shrink-0">
        <SearchBar onSearch={handleSearch} />
      </div>

      {hasSearched && (
        <>
          <div className="mt-3 flex items-center justify-between shrink-0">
            <span className="text-[14px] text-muted-foreground">
              {isLoading ? 'Searching...' : isError ? '' : `${data?.total ?? 0} cards found`}
            </span>
            {data && data.totalPages > 1 && (
              <PaginationBar page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
            )}
          </div>

          <div className="mt-3 flex-1 overflow-y-auto min-h-0">
            {isError ? (
              <div className="flex flex-col items-center justify-center py-16">
                <p className="text-[14px] text-destructive">
                  Could not load search results. Check that the server is running and try again.
                </p>
              </div>
            ) : isLoading ? (
              <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))' }}>
                {Array.from({ length: 12 }).map((_, i) => (
                  <Skeleton key={i} className="aspect-[488/680] rounded-lg" />
                ))}
              </div>
            ) : data && data.cards.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16">
                <h3 className="text-[20px] font-semibold text-foreground">No matching cards</h3>
                <p className="text-[14px] text-muted-foreground mt-2">Try adjusting your search terms or filters.</p>
              </div>
            ) : (
              <div className="grid gap-3" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))' }}>
                {(data?.cards ?? []).map((card) => (
                  <div
                    key={card.name + card.setCode}
                    className="cursor-pointer rounded-lg hover:ring-2 hover:ring-primary transition-all"
                    onClick={() => onCardClick(card)}
                    onMouseEnter={(e) => onCardMouseEnter(card, e)}
                    onMouseMove={onCardMouseMove}
                    onMouseLeave={onCardMouseLeave}
                  >
                    <img
                      src={getScryfallImageUrl(card.setCode, card.collectorNumber, 'normal')}
                      alt={card.name}
                      loading="lazy"
                      className="w-full rounded-lg"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          {data && data.totalPages > 1 && (
            <div className="mt-3 shrink-0">
              <PaginationBar page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
            </div>
          )}
        </>
      )}

      {!hasSearched && (
        <div className="flex-1 flex flex-col items-center justify-center py-16">
          <p className="text-[14px] text-muted-foreground">Search for cards to add to your deck.</p>
        </div>
      )}
    </div>
  )
}
