import { useState } from 'react'
import { Skeleton } from '../ui/skeleton'

interface GameCardImageProps {
  name: string
  width?: number
  className?: string
}

function getScryfallNameImageUrl(
  name: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): string {
  return `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(name)}&format=image&version=${version}`
}

export function GameCardImage({ name, width = 100, className }: GameCardImageProps) {
  const [imgError, setImgError] = useState(false)
  const [imgLoaded, setImgLoaded] = useState(false)

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
        src={getScryfallNameImageUrl(name)}
        alt={name}
        loading="lazy"
        onError={() => setImgError(true)}
        onLoad={() => setImgLoaded(true)}
        className={`w-full rounded-md ${imgLoaded ? '' : 'opacity-0'}`}
      />
    </div>
  )
}
