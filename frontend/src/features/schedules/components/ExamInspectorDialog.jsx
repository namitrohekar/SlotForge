import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import { formatDay, formatDuration, formatTime } from '../../../utils/format.js'

const when = (entry) => `${formatDay(entry.date)}, ${formatTime(entry.start)} – ${formatTime(entry.end)}`

export default function ExamInspectorDialog({ entry, entries, onClose }) {
  const related = entry.conflicts
    .map((conflict) => ({ ...conflict, paper: entries.find((other) => other.examinationId === conflict.examinationId) }))
    .filter(({ paper }) => paper)
    .sort((a, b) => `${a.paper.date}${a.paper.start}`.localeCompare(`${b.paper.date}${b.paper.start}`))

  const facts = [
    ['When', when(entry)],
    ['Duration', formatDuration(entry.durationMinutes)],
    ['Students', entry.students],
    ['Taught by', entry.departmentCode],
  ]

  return (
    <Dialog
      open
      onClose={onClose}
      title={entry.subjectName}
      description={entry.heavy ? `${entry.subjectCode}, heavy paper` : entry.subjectCode}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Done
        </Button>
      }
    >
      <div className="space-y-6">
        <dl className="divide-y divide-line">
          {facts.map(([label, value]) => (
            <div key={label} className="grid grid-cols-[7rem_minmax(0,1fr)] gap-3 py-2.5 text-callout">
              <dt className="text-ink-tertiary">{label}</dt>
              <dd data-numeric className="min-w-0 text-ink">
                {value}
              </dd>
            </div>
          ))}
        </dl>

        <section aria-labelledby="inspector-halls">
          <h3 id="inspector-halls" className="mb-2 text-footnote font-medium text-ink">
            {entry.halls.length === 1 ? 'Hall' : `Split across ${entry.halls.length} halls`}
          </h3>
          <ul className="overflow-hidden rounded-md border border-line">
            {entry.halls.map((hall) => (
              <li key={hall.code} className="flex items-center justify-between gap-4 border-b border-line px-3 py-2.5 last:border-b-0">
                <span className="min-w-0">
                  <span className="block text-callout text-ink">{hall.name}</span>
                  <span className="block text-footnote text-ink-tertiary">{hall.building}</span>
                </span>
                <span data-numeric className="shrink-0 text-footnote text-ink-secondary">
                  {hall.seats} of {hall.capacity} seats
                </span>
              </li>
            ))}
          </ul>
        </section>

        {related.length > 0 && (
          <section aria-labelledby="inspector-shared">
            <h3 id="inspector-shared" className="mb-2 text-footnote font-medium text-ink">
              Shares students with
            </h3>
            <ul className="overflow-hidden rounded-md border border-line">
              {related.map(({ paper, sharedStudents }) => (
                <li key={paper.examinationId} className="border-b border-line px-3 py-2.5 last:border-b-0">
                  <span className="block text-callout text-ink">{paper.subjectName}</span>
                  <span data-numeric className="block text-footnote text-ink-tertiary">
                    {when(paper)} · {sharedStudents} shared
                  </span>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </Dialog>
  )
}
