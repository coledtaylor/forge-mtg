import type { DeckSummary } from '../../types/deck'
import type { JumpstartPack } from '../../types/jumpstart'
import { Badge } from '../ui/badge'
import { Skeleton } from '../ui/skeleton'

interface PackPickerProps {
  label: string
  userPacks: DeckSummary[]
  builtInPacks: JumpstartPack[]
  selectedPack: string | null
  onSelect: (packId: string) => void
  isLoading: boolean
}

const COLOR_MAP: Record<string, string> = {
  W: 'bg-amber-100',
  U: 'bg-blue-400',
  B: 'bg-zinc-600',
  R: 'bg-red-500',
  G: 'bg-green-500',
}

export function PackPicker({
  label,
  userPacks,
  builtInPacks,
  selectedPack,
  onSelect,
  isLoading,
}: PackPickerProps) {
  if (isLoading) {
    return (
      <div className="flex-1 space-y-2">
        <span className="text-[12px] font-normal text-muted-foreground">{label}</span>
        <Skeleton className="h-[44px] w-full" />
        <Skeleton className="h-[44px] w-full" />
        <Skeleton className="h-[44px] w-full" />
      </div>
    )
  }

  const hasUserPacks = userPacks.length > 0
  const hasBuiltIn = builtInPacks.length > 0

  if (!hasUserPacks && !hasBuiltIn) {
    return (
      <div className="flex-1 space-y-2">
        <span className="text-[12px] font-normal text-muted-foreground">{label}</span>
        <div className="rounded-lg border border-border p-4 text-center text-sm text-muted-foreground">
          No packs available
        </div>
      </div>
    )
  }

  return (
    <div className="flex-1 space-y-2">
      <span className="text-[12px] font-normal text-muted-foreground">{label}</span>
      <div className="max-h-[320px] overflow-y-auto rounded-lg border border-border">
        {hasUserPacks && (
          <>
            <div className="px-3 py-1.5 text-[11px] font-medium text-muted-foreground bg-muted/50 sticky top-0">
              Your Packs
            </div>
            {userPacks.map((deck) => {
              const isSelected = deck.name === selectedPack
              return (
                <div
                  key={deck.name}
                  onClick={() => onSelect(deck.name)}
                  className={`flex items-center gap-2 px-3 h-[44px] cursor-pointer transition-colors ${
                    isSelected
                      ? 'border-l-[3px] border-primary bg-primary/10'
                      : 'border-l-[3px] border-transparent hover:bg-muted border-b border-b-border last:border-b-0'
                  }`}
                >
                  <span className="flex-1 text-[14px] font-normal truncate">
                    {deck.name}
                  </span>
                  <div className="flex items-center gap-1">
                    {deck.colors.map((color) => (
                      <span
                        key={color}
                        className={`inline-block size-2.5 rounded-full ${COLOR_MAP[color] ?? 'bg-muted-foreground'}`}
                      />
                    ))}
                  </div>
                  <Badge variant="secondary" className="text-[12px] font-normal">
                    {deck.cardCount}
                  </Badge>
                </div>
              )
            })}
          </>
        )}
        {hasBuiltIn && (
          <>
            <div className="px-3 py-1.5 text-[11px] font-medium text-muted-foreground bg-muted/50 sticky top-0">
              Built-in Packs
            </div>
            {builtInPacks.map((pack) => {
              const isSelected = pack.id === selectedPack
              return (
                <div
                  key={pack.id}
                  onClick={() => onSelect(pack.id)}
                  className={`flex items-center gap-2 px-3 h-[44px] cursor-pointer transition-colors ${
                    isSelected
                      ? 'border-l-[3px] border-primary bg-primary/10'
                      : 'border-l-[3px] border-transparent hover:bg-muted border-b border-b-border last:border-b-0'
                  }`}
                >
                  <span className="flex-1 text-[14px] font-normal truncate">
                    {pack.theme}
                  </span>
                  <div className="flex items-center gap-1">
                    {pack.colors.map((color) => (
                      <span
                        key={color}
                        className={`inline-block size-2.5 rounded-full ${COLOR_MAP[color] ?? 'bg-muted-foreground'}`}
                      />
                    ))}
                  </div>
                  <Badge variant="outline" className="text-[11px] font-normal">
                    {pack.setCode}
                  </Badge>
                  <Badge variant="secondary" className="text-[12px] font-normal">
                    {pack.cardCount}
                  </Badge>
                </div>
              )
            })}
          </>
        )}
      </div>
    </div>
  )
}
