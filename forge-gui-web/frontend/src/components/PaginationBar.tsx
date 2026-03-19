import { Button } from './ui/button'

interface PaginationBarProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function PaginationBar({ page, totalPages, onPageChange }: PaginationBarProps) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-center gap-4">
      <Button
        variant="ghost"
        disabled={page === 1}
        onClick={() => onPageChange(page - 1)}
      >
        Previous
      </Button>
      <span className="text-[14px] text-muted-foreground">
        Page <span className="text-primary font-semibold">{page}</span> of {totalPages}
      </span>
      <Button
        variant="ghost"
        disabled={page === totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </Button>
    </div>
  )
}
