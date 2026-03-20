import { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog'
import { Button } from '../ui/button'
import { FileUp, CheckCircle, AlertTriangle, XCircle } from 'lucide-react'
import { toast } from 'sonner'
import { parseDeckText } from '../../api/decks'
import type { ParseToken } from '../../types/deck'

interface ImportDeckDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onImport: (tokens: ParseToken[], mode: 'replace' | 'add') => void
}

const LEGAL_TYPES = new Set([
  'LEGAL_CARD', 'FREE_CARD_NOT_IN_INVENTORY',
  'LIMITED_CARD', 'CARD_FROM_NOT_ALLOWED_SET', 'CARD_FROM_INVALID_SET',
])
const WARNING_TYPES = new Set([
  'LIMITED_CARD', 'CARD_FROM_NOT_ALLOWED_SET', 'CARD_FROM_INVALID_SET',
])
const UNKNOWN_TYPES = new Set(['UNKNOWN_CARD', 'UNSUPPORTED_CARD'])

function debounce<T extends (...args: unknown[]) => void>(fn: T, ms: number): T & { cancel: () => void } {
  let timer: ReturnType<typeof setTimeout> | null = null
  const debounced = (...args: unknown[]) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), ms)
  }
  debounced.cancel = () => { if (timer) clearTimeout(timer) }
  return debounced as T & { cancel: () => void }
}

export function ImportDeckDialog({ open, onOpenChange, onImport }: ImportDeckDialogProps) {
  const [text, setText] = useState('')
  const [parseResult, setParseResult] = useState<ParseToken[]>([])
  const [fileName, setFileName] = useState<string | null>(null)
  const [fileSize, setFileSize] = useState<number>(0)
  const [isDragging, setIsDragging] = useState(false)
  const [isParsing, setIsParsing] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Reset state when dialog opens/closes
  useEffect(() => {
    if (open) {
      setText('')
      setParseResult([])
      setFileName(null)
      setFileSize(0)
      setIsDragging(false)
      setIsParsing(false)
    }
  }, [open])

  // Debounced parse
  const debouncedParse = useMemo(() => debounce(async (...args: unknown[]) => {
    const value = args[0] as string
    if (!value.trim()) {
      setParseResult([])
      setIsParsing(false)
      return
    }
    try {
      const result = await parseDeckText(value)
      setParseResult(result)
    } catch {
      // Parse error - leave empty
      setParseResult([])
    }
    setIsParsing(false)
  }, 300), [])

  // Clean up debounce on unmount
  useEffect(() => () => debouncedParse.cancel(), [debouncedParse])

  const handleTextChange = useCallback((value: string) => {
    setText(value)
    if (!value.trim()) {
      setParseResult([])
      setIsParsing(false)
      debouncedParse.cancel()
    } else {
      setIsParsing(true)
      debouncedParse(value)
    }
  }, [debouncedParse])

  const handleFileLoad = useCallback((file: File) => {
    setFileName(file.name)
    setFileSize(file.size)
    const reader = new FileReader()
    reader.onload = (e) => {
      const content = e.target?.result as string
      setText(content)
      setIsParsing(true)
      debouncedParse(content)
    }
    reader.readAsText(file)
  }, [debouncedParse])

  const handleFileInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFileLoad(file)
  }, [handleFileLoad])

  const handleClearFile = useCallback(() => {
    setFileName(null)
    setFileSize(0)
    setText('')
    setParseResult([])
    if (fileInputRef.current) fileInputRef.current.value = ''
  }, [])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback(() => {
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFileLoad(file)
  }, [handleFileLoad])

  const handleImport = useCallback((mode: 'replace' | 'add') => {
    onImport(parseResult, mode)
    onOpenChange(false)
    toast.success('Deck imported successfully')
  }, [parseResult, onImport, onOpenChange])

  // Compute summary counts
  const { recognized, warnings, notFound, hasCards } = useMemo(() => {
    let rec = 0, warn = 0, nf = 0
    for (const token of parseResult) {
      if (LEGAL_TYPES.has(token.type)) rec++
      if (WARNING_TYPES.has(token.type)) warn++
      if (UNKNOWN_TYPES.has(token.type)) nf++
    }
    return { recognized: rec, warnings: warn, notFound: nf, hasCards: rec > 0 }
  }, [parseResult])

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    return `${(bytes / 1024).toFixed(1)} KB`
  }

  const replaceLabel = notFound > 0 ? `Replace Deck (${notFound} skipped)` : 'Replace Deck'
  const addLabel = notFound > 0 ? `Add to Deck (${notFound} skipped)` : 'Add to Deck'

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>Import Deck</DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-2 gap-8 min-h-[400px]">
          {/* Left column: Input */}
          <div className="flex flex-col gap-2">
            {/* File drop zone */}
            <div
              className={`border-2 border-dashed rounded-md p-4 text-center cursor-pointer text-muted-foreground transition-colors ${
                isDragging ? 'border-primary bg-primary/5' : 'border-border'
              }`}
              onClick={() => fileInputRef.current?.click()}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".dck,.dec,.txt"
                className="hidden"
                onChange={handleFileInputChange}
              />
              {fileName ? (
                <div className="flex items-center justify-center gap-2 text-sm">
                  <span className="text-foreground">{fileName}</span>
                  <span className="text-muted-foreground">({formatFileSize(fileSize)})</span>
                  <button
                    type="button"
                    className="text-muted-foreground hover:text-foreground underline text-xs"
                    onClick={(e) => { e.stopPropagation(); handleClearFile() }}
                  >
                    Clear
                  </button>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-1">
                  <FileUp className="h-6 w-6" />
                  <span className="text-sm">
                    {isDragging ? 'Drop file to import' : 'Drop .dck, .dec, or .txt file here, or click to browse'}
                  </span>
                </div>
              )}
            </div>

            {/* Textarea */}
            <textarea
              className="flex-1 resize-none font-mono text-sm border border-border rounded-md p-2 bg-background min-h-[200px] focus:outline-none focus:ring-1 focus:ring-ring"
              placeholder="Paste deck list here..."
              value={text}
              onChange={(e) => handleTextChange(e.target.value)}
            />
          </div>

          {/* Right column: Preview */}
          <div className="flex flex-col overflow-hidden">
            <span className="text-xs text-muted-foreground uppercase mb-2">Preview</span>

            {parseResult.length === 0 && !isParsing ? (
              <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
                Paste or upload a deck list to see preview
              </div>
            ) : (
              <div className="flex-1 overflow-y-auto flex flex-col">
                <div className="flex-1">
                  {parseResult.map((token, i) => (
                    <div
                      key={i}
                      className="flex items-center gap-2 px-2 py-1"
                      style={{ height: '28px' }}
                    >
                      {renderTokenIcon(token)}
                      <span className={`text-sm truncate ${getTokenTextClass(token)}`}>
                        {token.cardName
                          ? `${token.quantity} ${token.cardName}`
                          : token.text}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Summary */}
                {parseResult.length > 0 && (
                  <div className="sticky bottom-0 border-t border-border pt-2 pb-1 bg-background">
                    <span className="text-xs text-muted-foreground">
                      {[
                        recognized > 0 ? `${recognized} recognized` : null,
                        warnings > 0 ? `${warnings} warnings` : null,
                        notFound > 0 ? `${notFound} not found` : null,
                      ].filter(Boolean).join(', ')}
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>
          <Button
            variant="outline"
            disabled={!hasCards}
            className={!hasCards ? 'opacity-50 cursor-not-allowed' : ''}
            onClick={() => handleImport('add')}
          >
            {addLabel}
          </Button>
          <Button
            variant="default"
            disabled={!hasCards}
            className={!hasCards ? 'opacity-50 cursor-not-allowed' : ''}
            onClick={() => handleImport('replace')}
          >
            {replaceLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function renderTokenIcon(token: ParseToken) {
  if (token.type === 'LEGAL_CARD' || token.type === 'FREE_CARD_NOT_IN_INVENTORY') {
    return <CheckCircle className="h-3.5 w-3.5 text-green-500 shrink-0" />
  }
  if (WARNING_TYPES.has(token.type)) {
    return <AlertTriangle className="h-3.5 w-3.5 text-amber-500 shrink-0" />
  }
  if (UNKNOWN_TYPES.has(token.type)) {
    return <XCircle className="h-3.5 w-3.5 text-destructive shrink-0" />
  }
  return <span className="w-3.5 shrink-0" />
}

function getTokenTextClass(token: ParseToken): string {
  if (token.type === 'LEGAL_CARD' || token.type === 'FREE_CARD_NOT_IN_INVENTORY') {
    return 'text-green-500'
  }
  if (WARNING_TYPES.has(token.type)) {
    return 'text-amber-500'
  }
  if (UNKNOWN_TYPES.has(token.type)) {
    return 'text-destructive'
  }
  return 'text-muted-foreground'
}
