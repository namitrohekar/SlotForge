const dots = { warning: 'bg-warning', danger: 'bg-danger' }

// Quiet text. A dot appears only when something needs attention, and the words always carry the meaning.
export default function Badge({ tone = 'neutral', icon: Icon, children }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-footnote whitespace-nowrap text-ink-secondary">
      {dots[tone] && <span aria-hidden="true" className={`size-1.5 shrink-0 rounded-full ${dots[tone]}`} />}
      {Icon && <Icon size={16} aria-hidden="true" />}
      {children}
    </span>
  )
}
