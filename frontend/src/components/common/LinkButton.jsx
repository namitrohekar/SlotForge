import { Link } from 'react-router'

// Looks like the primary Button, but it navigates, so it must be a real link.
export default function LinkButton({ to, children }) {
  return (
    <Link
      to={to}
      className="sf-press inline-flex h-control shrink-0 items-center justify-center rounded-md bg-accent px-4 text-callout font-medium whitespace-nowrap text-on-accent hover:bg-accent-hover"
    >
      {children}
    </Link>
  )
}
