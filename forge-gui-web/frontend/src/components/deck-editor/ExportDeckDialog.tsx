import { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog'
import { Button } from '../ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Copy } from 'lucide-react'
import { toast } from 'sonner'
import { exportDeck } from '../../api/decks'

interface ExportDeckDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  deckName: string
}

const FORMATS = [
  { value: 'generic', label: 'Generic text' },
  { value: 'mtgo', label: 'MTGO' },
  { value: 'arena', label: 'Arena' },
  { value: 'forge', label: 'Forge .dck' },
]

export function ExportDeckDialog({ open, onOpenChange, deckName }: ExportDeckDialogProps) {
  const [format, setFormat] = useState('generic')
  const [exportText, setExportText] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Reset when dialog opens
  useEffect(() => {
    if (open) {
      setFormat('generic')
      setExportText('')
    }
  }, [open])

  // Fetch export text when format changes or dialog opens
  useEffect(() => {
    if (!open) return

    let cancelled = false
    setIsLoading(true)

    exportDeck(deckName, format)
      .then((text) => {
        if (!cancelled) setExportText(text)
      })
      .catch(() => {
        if (!cancelled) setExportText('Error loading export')
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => { cancelled = true }
  }, [deckName, format, open])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(exportText)
      toast.success('Copied to clipboard')
    } catch {
      // Fallback
      const textarea = document.createElement('textarea')
      textarea.value = exportText
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      toast.success('Copied to clipboard')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Export Deck</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <Select value={format} onValueChange={setFormat}>
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {FORMATS.map((f) => (
                <SelectItem key={f.value} value={f.value}>
                  {f.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <textarea
            readOnly
            className="font-mono text-sm h-64 resize-none border border-border rounded-md bg-background w-full p-2 focus:outline-none"
            value={isLoading ? 'Loading...' : exportText}
          />
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          <Button variant="default" onClick={handleCopy} disabled={isLoading || !exportText} className="gap-1.5">
            <Copy className="h-4 w-4" />
            Copy to Clipboard
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
