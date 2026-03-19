import { useState } from 'react'
import { getScryfallImageUrl } from '../lib/scryfall'
import { Skeleton } from './ui/skeleton'
import type { CardSearchResult } from '../types/card'

interface CardImageProps {
  card: CardSearchResult
}

export function CardImage({ card }: CardImageProps) {
  const [imgError, setImgError] = useState(false)
  const [imgLoaded, setImgLoaded] = useState(false)

  if (imgError) {
    return (
      <div className="w-[244px] aspect-[488/680] bg-[hsl(240,3.7%,12%)] border border-[hsl(240,3.7%,20%)] rounded-lg p-3 flex flex-col gap-1 hover:ring-2 hover:ring-primary transition-all">
        <div className="text-[14px] font-semibold text-foreground">{card.name}</div>
        <div className="text-[12px] text-muted-foreground">{card.manaCost}</div>
        <div className="text-[12px] text-muted-foreground">{card.typeLine}</div>
      </div>
    )
  }

  return (
    <div className="w-[244px] aspect-[488/680] relative hover:ring-2 hover:ring-primary rounded-lg transition-all">
      {!imgLoaded && (
        <Skeleton className="absolute inset-0 rounded-lg" />
      )}
      <img
        src={getScryfallImageUrl(card.setCode, card.collectorNumber)}
        alt={card.name}
        loading="lazy"
        onError={() => setImgError(true)}
        onLoad={() => setImgLoaded(true)}
        className={`w-[244px] rounded-lg ${imgLoaded ? '' : 'opacity-0'}`}
      />
    </div>
  )
}
