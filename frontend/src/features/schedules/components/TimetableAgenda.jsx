import { formatDay, formatTime } from '../../../utils/format.js'
import ExamCard from './ExamCard.jsx'

// A grid is unreadable at phone width, so phones get the same timetable as a list of days.
export default function TimetableAgenda({ entries, onOpen }) {
  const days = [...new Set(entries.map((entry) => entry.date))]

  return (
    <ol className="space-y-6">
      {days.map((date) => (
        <li key={date}>
          <h3 className="mb-2 text-title-3">{formatDay(date)}</h3>
          <ul className="space-y-2">
            {entries
              .filter((entry) => entry.date === date)
              .map((entry) => (
                <li key={entry.examinationId}>
                  <p data-numeric className="mb-1 text-footnote text-ink-tertiary">
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
