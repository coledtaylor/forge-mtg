import { useState } from 'react'
import { Skeleton } from '../ui/skeleton'
import { getScryfallImageUrl } from '../../lib/scryfall'

interface GameCardImageProps {
  name: string
  setCode?: string | null
  collectorNumber?: string | null
  width?: number
  className?: string
}

export function GameCardImage({ name, setCode, collectorNumber, width = 100, className }: GameCardImageProps) {
  const [imgError, setImgError] = useState(false)
  const [imgLoaded, setImgLoaded] = useState(false)

  // Use set/collector URL if available (CARD-01), fall back to name-based for tokens
  const imageUrl = setCode && collectorNumber
    ? getScryfallImageUrl(setCode, collectorNumber)
    : `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(name)}&format=image&version=normal&lang=en`

  if (imgError) {
    return (
      <div
        className={`aspect-[5/7] bg-[hsl(240,3.7%,12%)] border border-[hsl(240,3.7%,20%)] rounded-md flex items-center justify-center p-1 ${className ?? ''}`}
        style={{ width }}
      >
        <span className="text-[10px] text-muted-foreground text-center leading-tight line-clamp-3">
          {name}
        </span>
      </div>
    )
  }

  return (
    <div
      className={`aspect-[5/7] relative rounded-md overflow-hidden ${className ?? ''}`}
      style={{ width }}
    >
      {!imgLoaded && (
        <Skeleton className="absolute inset-0 rounded-md" />
      )}
      <img
        src={imageUrl}
        alt={name}
        loading="lazy"
        onError={() => setImgError(true)}
        onLoad={() => setImgLoaded(true)}
        className={`w-full rounded-md ${imgLoaded ? '' : 'opacity-0'}`}
      />
    </div>
  )
}
