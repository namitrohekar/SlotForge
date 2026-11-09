import HeavyBadge from '../../../components/common/HeavyBadge.jsx'

function hallSummary(halls) {
  if (halls.length <= 2) return halls.map((hall) => hall.code).join(', ')
  return `${halls[0].code}, ${halls[1].code} +${halls.length - 2}`
}

/** tone is 'rest' when another paper is in focus and this one is unrelated. shared is students in common with it. */
export default function ExamCard({ entry, tone, shared, onOpen, onFocusChange }) {
  const invigilators = entry.halls.reduce((total, hall) => total + hall.invigilators.length, 0)

  return (
    <button
      type="button"
      onClick={() => onOpen(entry)}
      onMouseEnter={() => onFocusChange?.(entry.examinationId)}
      onFocus={() => onFocusChange?.(entry.examinationId)}
      className={`@container relative block w-full px-3 py-3 text-left transition-[background-color,opacity] duration-(--duration-fast) -outline-offset-2 hover:bg-sunken ${
        tone === 'rest' ? 'opacity-40' : ''
      }`}
    >
      {shared && <span aria-hidden="true" className="absolute inset-y-3 left-0 w-0.5 bg-tick" />}
      <span className="flex flex-col gap-1 @lg:flex-row @lg:items-baseline @lg:justify-between @lg:gap-6">
        <span className="min-w-0">
          <span className="flex items-center justify-between gap-2 @lg:justify-start">
            <span className="font-mono text-caption text-ink-tertiary">{entry.subjectCode}</span>
            {entry.heavy && <HeavyBadge />}
          </span>
          <span className="mt-0.5 line-clamp-2 text-callout font-medium text-ink">{entry.subjectName}</span>
        </span>
        {shared ? (
          <span data-numeric className="block text-footnote text-ink @lg:shrink-0">
            Shares {shared} {shared === 1 ? 'student' : 'students'}
          </span>
        ) : (
          <span data-numeric className="block truncate text-footnote text-ink-secondary @lg:shrink-0">
            {entry.students} students · {hallSummary(entry.halls)} · {invigilators} {invigilators === 1 ? 'invigilator' : 'invigilators'}
          </span>
        )}
      </span>
    </button>
  )
}
