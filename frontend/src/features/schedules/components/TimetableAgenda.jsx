import { formatDay, formatTime } from '../../../utils/format.js'
import ExamCard from './ExamCard.jsx'

// A grid is unreadable at phone width, so phones get the same timetable as a list of days.
export default function TimetableAgenda({ entries, onOpen }) {
  const days = [...new Set(entries.map((entry) => entry.date))]

  return (
    <ol className="space-y-8">
      {days.map((date) => (
        <li key={date}>
          <h3 className="mb-2 text-title-3">{formatDay(date)}</h3>
          <ul className="divide-y divide-line border-y border-line">
            {entries
              .filter((entry) => entry.date === date)
              .map((entry) => (
                <li key={entry.examinationId}>
                  <p className="px-3 pt-3 font-mono text-caption text-ink-tertiary">
                    {formatTime(entry.start)} – {formatTime(entry.end)}
                  </p>
                  <ExamCard entry={entry} onOpen={onOpen} />
                </li>
              ))}
          </ul>
        </li>
      ))}
    </ol>
  )
}
