import { useState } from 'react'
import { formatDay, formatTime } from '../../../utils/format.js'
import ExamCard from './ExamCard.jsx'

// The raw times sort correctly, which "10:00 am" and "2:00 pm" would not.
const periodOf = ({ start, end }) => `${start}|${end}`
const label = (period) => period.split('|').map(formatTime).join(' – ')

// One row per day and one column per exam period, the shape a timetable has on a noticeboard.
export default function TimetableGrid({ entries, onOpen }) {
  const [focused, setFocused] = useState(null)
  const days = [...new Set(entries.map((entry) => entry.date))]
  const periods = [...new Set(entries.map(periodOf))].sort()
  const inFocus = entries.find((entry) => entry.examinationId === focused)
  const sharedWith = new Map(inFocus?.conflicts.map((conflict) => [conflict.examinationId, conflict.sharedStudents]))

  const toneOf = (entry) => (inFocus && entry !== inFocus && !sharedWith.has(entry.examinationId) ? 'rest' : undefined)

  return (
    <div onMouseLeave={() => setFocused(null)} onBlur={() => setFocused(null)}>
      <div className="w-fit max-w-full overflow-x-auto rounded-lg border border-line">
        <table className="border-collapse text-left">
          <caption className="sr-only">Timetable by day and exam period</caption>
          <thead>
            <tr className="border-b border-line bg-sunken text-footnote text-ink-secondary">
              <th scope="col" className="sticky left-0 z-10 w-32 bg-sunken px-4 py-2.5 font-medium">
                Day
              </th>
              {periods.map((period) => (
                <th key={period} scope="col" data-numeric className="w-72 min-w-60 px-3 py-2.5 font-medium">
                  {label(period)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {days.map((date) => (
              <tr key={date}>
                <th scope="row" className="sticky left-0 z-10 bg-surface px-4 py-3 align-top text-callout font-medium whitespace-nowrap text-ink">
                  {formatDay(date)}
                </th>
                {periods.map((period) => (
                  <td key={period} className="px-2 py-2 align-top">
                    <div className="space-y-2">
                      {entries
                        .filter((entry) => entry.date === date && periodOf(entry) === period)
                        .map((entry) => (
                          <ExamCard
                            key={entry.examinationId}
                            entry={entry}
                            tone={toneOf(entry)}
                            shared={sharedWith.get(entry.examinationId)}
                            onOpen={onOpen}
                            onFocusChange={setFocused}
                          />
                        ))}
                    </div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-3 text-footnote text-ink-tertiary">Point at a paper to see which others share its students.</p>
    </div>
  )
}
