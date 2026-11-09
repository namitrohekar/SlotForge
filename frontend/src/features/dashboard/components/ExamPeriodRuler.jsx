import { useCallback } from 'react'
import { useApi } from '../../../hooks/useApi.js'
import { formatDay } from '../../../utils/format.js'
import { listEntries, listVersions } from '../../schedules/services/schedulesApi.js'
import { rulerDays } from '../rulerDays.js'

async function loadLatestEntries(sessionId) {
  const versions = await listVersions(sessionId)
  return versions.length ? listEntries(versions[0].id) : []
}

// The whole exam period at a glance, one stroke per paper, ruled like the margin of a printed timetable.
export default function ExamPeriodRuler({ session }) {
  const { data: entries } = useApi(useCallback(() => loadLatestEntries(session.id), [session.id]))
  if (!entries?.length) return null

  const papersOn = {}
  entries.forEach(({ date }) => {
    papersOn[date] = (papersOn[date] ?? 0) + 1
  })

  return (
    <figure className="mt-10 hidden md:block">
      <div aria-hidden="true" className="flex">
        {rulerDays(session.startDate, session.endDate, papersOn).map((day, index) =>
          day === null ? (
            <div key={`gap-${index}`} className="h-16 w-5 shrink-0 border-b border-ink bg-sunken" />
          ) : (
            <div key={day} title={formatDay(day)} className="min-w-0 flex-1">
              <div className="flex h-16 items-end justify-center gap-1 border-b border-ink pb-2">
                {Array.from({ length: papersOn[day] ?? 0 }, (_, stroke) => (
                  <span key={stroke} className="h-8 w-0.5 bg-ink" />
                ))}
              </div>
              <p className="pt-2 text-center font-mono text-caption text-ink-tertiary">
                {formatDay(day).split(/[ ,]+/)[0]}
                <span className="block text-ink">{Number(day.slice(8))}</span>
              </p>
            </div>
          ),
        )}
      </div>
      <figcaption className="mt-3 text-footnote text-ink-tertiary">
        Latest version, {entries.length} {entries.length === 1 ? 'paper' : 'papers'} across the exam period
      </figcaption>
    </figure>
  )
}
