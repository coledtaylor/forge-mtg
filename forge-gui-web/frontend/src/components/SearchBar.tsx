import { useState } from 'react'
import { Input } from './ui/input'
import { Button } from './ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select'
import type { CardSearchParams } from '../types/card'

interface SearchBarProps {
  onSearch: (params: CardSearchParams) => void
}

const COLOR_OPTIONS = [
  { value: '', label: 'Any Color' },
  { value: 'W', label: 'White' },
  { value: 'U', label: 'Blue' },
  { value: 'B', label: 'Black' },
  { value: 'R', label: 'Red' },
  { value: 'G', label: 'Green' },
]

const TYPE_OPTIONS = [
  { value: '', label: 'Any Type' },
  { value: 'Creature', label: 'Creature' },
  { value: 'Instant', label: 'Instant' },
  { value: 'Sorcery', label: 'Sorcery' },
  { value: 'Enchantment', label: 'Enchantment' },
  { value: 'Artifact', label: 'Artifact' },
  { value: 'Planeswalker', label: 'Planeswalker' },
  { value: 'Land', label: 'Land' },
]

const FORMAT_OPTIONS = [
  { value: '', label: 'Any Format' },
  { value: 'Standard', label: 'Standard' },
  { value: 'Modern', label: 'Modern' },
  { value: 'Legacy', label: 'Legacy' },
  { value: 'Vintage', label: 'Vintage' },
  { value: 'Commander', label: 'Commander' },
  { value: 'Pioneer', label: 'Pioneer' },
  { value: 'Pauper', label: 'Pauper' },
]

const CMC_OP_OPTIONS = [
  { value: 'eq', label: '=' },
  { value: 'lt', label: '<' },
  { value: 'gt', label: '>' },
  { value: 'lte', label: '<=' },
  { value: 'gte', label: '>=' },
]

export function SearchBar({ onSearch }: SearchBarProps) {
  const [q, setQ] = useState('')
  const [color, setColor] = useState('')
  const [type, setType] = useState('')
  const [format, setFormat] = useState('')
  const [cmc, setCmc] = useState('')
  const [cmcOp, setCmcOp] = useState('eq')

  const handleSubmit = () => {
    const params: CardSearchParams = {}
    if (q) params.q = q
    if (color) params.color = color
    if (type) params.type = type
    if (format) params.format = format
    if (cmc !== '') {
      params.cmc = Number(cmc)
      params.cmcOp = cmcOp as CardSearchParams['cmcOp']
    }
    onSearch(params)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSubmit()
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      <Input
        className="flex-1 min-w-[200px]"
        placeholder="Search cards by name..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onKeyDown={handleKeyDown}
      />

      <Select value={color} onValueChange={(v) => setColor(v ?? '')}>
        <SelectTrigger className="w-[130px]">
          <SelectValue placeholder="Any Color" />
        </SelectTrigger>
        <SelectContent>
          {COLOR_OPTIONS.map((opt) => (
            <SelectItem key={opt.value || 'any-color'} value={opt.value || ' '}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={type} onValueChange={(v) => setType(v ?? '')}>
        <SelectTrigger className="w-[140px]">
          <SelectValue placeholder="Any Type" />
        </SelectTrigger>
        <SelectContent>
          {TYPE_OPTIONS.map((opt) => (
            <SelectItem key={opt.value || 'any-type'} value={opt.value || ' '}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={format} onValueChange={(v) => setFormat(v ?? '')}>
        <SelectTrigger className="w-[140px]">
          <SelectValue placeholder="Any Format" />
        </SelectTrigger>
        <SelectContent>
          {FORMAT_OPTIONS.map((opt) => (
            <SelectItem key={opt.value || 'any-format'} value={opt.value || ' '}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <div className="flex items-center gap-1">
        <span className="text-[12px] text-muted-foreground">CMC</span>
        <Select value={cmcOp} onValueChange={(v) => setCmcOp(v ?? 'eq')}>
          <SelectTrigger className="w-[60px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {CMC_OP_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          type="number"
          className="w-[60px]"
          placeholder="--"
          value={cmc}
          onChange={(e) => setCmc(e.target.value)}
          onKeyDown={handleKeyDown}
          min={0}
        />
      </div>

      <Button onClick={handleSubmit}>
        Search Cards
      </Button>
    </div>
  )
}
