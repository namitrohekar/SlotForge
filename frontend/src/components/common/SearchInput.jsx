import { useRef } from 'react'
import { Search } from 'lucide-react'
import { controlStyles } from './controlStyles.js'

const DEBOUNCE_MS = 250

export default function SearchInput({ label = 'Search', value = '', onSearch }) {
  const timer = useRef(null)

  const handleChange = (event) => {
    const next = event.target.value
    clearTimeout(timer.current)
    timer.current = setTimeout(() => onSearch(next), DEBOUNCE_MS)
  }

  return (
    <div className="relative min-w-0">
      <Search
        size={16}
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-ink-tertiary"
      />
      <input
        type="search"
        aria-label={label}
        placeholder={label}
        defaultValue={value}
        onChange={handleChange}
        className={`h-control pr-3 pl-9 ${controlStyles}`}
      />
    </div>
  )
}
