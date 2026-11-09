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
      className={`sf-press block w-full rounded-md border bg-surface p-3 text-left transition-opacity duration-(--duration-fast) [--press-scale:0.99] ${
        shared ? 'border-accent' : 'border-line hover:border-line-strong'
      } ${tone === 'rest' ? 'opacity-40' : ''}`}
    >
      <span className="flex items-center justify-between gap-2">
        <span data-numeric className="text-caption text-ink-tertiary">
          {entry.subjectCode}
        </span>
        {entry.heavy && <HeavyBadge />}
      </span>
      <span className="mt-0.5 line-clamp-2 text-callout font-medium text-ink">{entry.subjectName}</span>
      {shared ? (
        <span data-numeric className="mt-1 block text-footnote text-accent">
          Shares {shared} {shared === 1 ? 'student' : 'students'}
        </span>
      ) : (
        <span data-numeric className="mt-1 block truncate text-footnote text-ink-secondary">
          {entry.students} students · {hallSummary(entry.halls)} · {invigilators} {invigilators === 1 ? 'invigilator' : 'invigilators'}
        </span>
      )}
    </button>
  )
}
