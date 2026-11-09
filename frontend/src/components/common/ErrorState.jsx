import { TriangleAlert } from 'lucide-react'
import Button from './Button.jsx'

export default function ErrorState({ title = 'This could not be loaded', message, onRetry }) {
  return (
    <div role="alert" className="flex flex-col items-center px-6 py-12 text-center">
      <TriangleAlert size={20} aria-hidden="true" className="text-danger" />
      <h3 className="mt-3 text-title-3">{title}</h3>
      {message && <p className="mt-1 max-w-md text-callout text-ink-secondary">{message}</p>}
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} className="mt-4">
          Try again
        </Button>
      )}
    </div>
  )
}
