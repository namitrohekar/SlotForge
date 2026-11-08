import { ChevronLeft, ChevronRight } from 'lucide-react'
import { formatNumber } from '../../utils/format.js'
import IconButton from './IconButton.jsx'

export default function Pagination({ page, totalPages, totalItems, onChange }) {
  if (totalItems === 0) return null

  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-4 pt-3">
      <p data-numeric className="text-footnote text-ink-secondary">
        {formatNumber(totalItems)} {totalItems === 1 ? 'result' : 'results'}
      </p>
      <div className="flex items-center gap-1">
        <IconButton label="Previous page" icon={ChevronLeft} disabled={page === 0} onClick={() => onChange(page - 1)} />
        <p data-numeric className="px-1 text-footnote text-ink-secondary">
          Page {page + 1} of {totalPages}
        </p>
        <IconButton
          label="Next page"
          icon={ChevronRight}
          disabled={page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        />
      </div>
    </nav>
  )
}
