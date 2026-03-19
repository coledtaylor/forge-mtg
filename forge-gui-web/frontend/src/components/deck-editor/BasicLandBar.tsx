import { Button } from '../ui/button'
import { Plus, Minus } from 'lucide-react'

interface BasicLandBarProps {
  onAddLand: (landName: string) => void
  onRemoveLand: (landName: string) => void
  landCounts: Record<string, number>
}

const BASIC_LANDS = [
  { name: 'Plains', symbol: 'ms ms-w ms-cost' },
  { name: 'Island', symbol: 'ms ms-u ms-cost' },
  { name: 'Swamp', symbol: 'ms ms-b ms-cost' },
  { name: 'Mountain', symbol: 'ms ms-r ms-cost' },
  { name: 'Forest', symbol: 'ms ms-g ms-cost' },
]

export function BasicLandBar({ onAddLand, onRemoveLand, landCounts }: BasicLandBarProps) {
  return (
    <div className="flex items-center gap-4 px-4 py-2 border-t border-border">
      {BASIC_LANDS.map((land) => {
        const count = landCounts[land.name] || 0
        return (
          <div key={land.name} className="flex items-center gap-1">
            <i className={`${land.symbol} text-[20px] text-muted-foreground`} title={land.name} />
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => onRemoveLand(land.name)}
              disabled={count === 0}
            >
              <Minus className="h-3 w-3" />
            </Button>
            <span className="text-[14px] text-foreground min-w-[24px] text-center">{count}</span>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => onAddLand(land.name)}
            >
              <Plus className="h-3 w-3" />
            </Button>
          </div>
        )
      })}
    </div>
  )
}
