import { LoaderCircle } from 'lucide-react'

export default function Spinner({ label = 'Loading', size = 20 }) {
  return (
    <span role="status" className="inline-flex">
      <LoaderCircle size={size} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
      <span className="sr-only">{label}</span>
    </span>
  )
}
