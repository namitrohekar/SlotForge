export default function Skeleton({ className = '' }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-sm bg-sunken motion-reduce:animate-none ${className}`} />
}
