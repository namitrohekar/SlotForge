import { ChevronDown } from 'lucide-react'
import { controlStyles } from './controlStyles.js'

// A native select, so phones use their own picker.
export default function Select({ className = '', children, ...props }) {
  return (
    <div className="relative min-w-0">
      <select className={`h-control appearance-none pr-9 pl-3 ${controlStyles} ${className}`} {...props}>
        {children}
      </select>
      <ChevronDown
        size={16}
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-ink-tertiary"
      />
    </div>
  )
}
