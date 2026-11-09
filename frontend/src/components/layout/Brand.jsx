// A wordmark that collapses to its initial when the sidebar becomes a rail.
export default function Brand({ fadeClassName = '' }) {
  return (
    <span className="text-title-2 font-semibold whitespace-nowrap text-ink">
      S<span className={`sf-fade ${fadeClassName}`}>lotForge</span>
    </span>
  )
}
