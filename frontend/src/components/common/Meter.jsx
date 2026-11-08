// The value is always written out, so the bar is never the only way to read it.
export default function Meter({ label, value, max = 100, display }) {
  const percent = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0

  return (
    <div>
      <div className="flex items-baseline justify-between gap-3 text-footnote">
        <span className="min-w-0 text-ink-secondary">{label}</span>
        <span data-numeric className="shrink-0 font-medium text-ink">
          {display ?? `${percent}%`}
        </span>
      </div>
      <div
        role="meter"
        aria-label={label}
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line"
      >
        <div className="h-full rounded-full bg-accent" style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
