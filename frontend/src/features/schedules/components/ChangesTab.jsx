import { useCallback } from 'react'
import { ChevronLeft } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import StatTile from '../../../components/common/StatTile.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatDay, formatNumber, formatTime } from '../../../utils/format.js'
import { fetchChanges } from '../services/schedulesApi.js'

const KINDS = { MOVED: 'Moved', HALLS: 'Hall changed', INVIGILATORS: 'Invigilators changed', ADDED: 'Added', REMOVED: 'Removed' }
const STATES = { DRAFT: 'a draft', REVIEWED: 'approved', PUBLISHED: 'published', SUPERSEDED: 'superseded' }

function Place({ place, showPeople }) {
  if (!place) return <span className="text-ink-tertiary">Not in the timetable</span>
  return (
    <>
      <span className="block text-ink">
        {formatDay(place.date)}, <span className="font-mono text-caption">{formatTime(place.start)}</span>
      </span>
      <span className="block font-mono text-caption text-ink-tertiary">{place.halls.join(', ')}</span>
      {showPeople && <span className="block text-footnote text-ink-tertiary">{place.invigilators.join(', ') || 'Nobody assigned'}</span>}
    </>
  )
}

// What a repair or an edit actually changed, so the people affected can be told exactly that and nothing more.
export default function ChangesTab() {
  const [params] = useSearchParams()
  const versionId = params.get('version')
  const { data, error, reload } = useApi(useCallback(() => fetchChanges(versionId), [versionId]))

  const back = (
    <Link to={`../timetable?version=${versionId}`} className="sf-press mb-4 inline-flex items-center gap-1 rounded-sm text-footnote text-ink-secondary hover:text-ink">
      <ChevronLeft size={16} aria-hidden="true" />
      Timetable
    </Link>
  )
  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-64 w-full rounded-lg" />
  if (data.againstVersionNo === null) {
    return (
      <>
        {back}
        <p className="text-callout text-ink-secondary">This is the only version, so there is nothing to compare it with yet.</p>
      </>
    )
  }

  const { summary } = data
  return (
    <>
      {back}
      <h2 className="text-title-1">What changed in version {data.versionNo}</h2>
      <p className="mt-1 text-footnote text-ink-tertiary">
        Compared with version {data.againstVersionNo}, which is {STATES[data.againstStatus] ?? data.againstStatus}
      </p>

      <div className="mt-6 grid gap-y-6 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile label="Papers moved" value={summary.papersMoved} context={`${summary.unchanged} of ${summary.unchanged + data.changes.length} papers untouched`} />
        <StatTile label="Hall changes" value={summary.hallChanges} context="Same day and time, different hall" />
        <StatTile label="Student sittings affected" value={formatNumber(summary.studentsAffected)} context="Students who must be told of a new day or hall" />
        <StatTile label="Staff with changed duties" value={summary.staffAffected} context="Invigilators whose duties differ" />
      </div>

      {data.changes.length === 0 ? (
        <p className="mt-10 text-callout text-ink-secondary">Nothing differs between the two timetables.</p>
      ) : (
        <div className="mt-10 overflow-x-auto">
          <table className="w-full border-collapse text-left text-callout">
            <caption className="sr-only">Papers that changed</caption>
            <thead>
              <tr className="border-b border-ink text-caption text-ink-tertiary">
                {['Paper', 'What changed', 'Before', 'Now'].map((header) => (
                  <th key={header} scope="col" className="px-3 pb-2 font-normal first:pl-0 last:pr-0">
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {data.changes.map((change) => (
                <tr key={change.paperCode} className="align-top">
                  <td className="py-3 pr-3">
                    <span className="block font-medium text-ink">{change.paperName}</span>
                    <span data-numeric className="block text-footnote text-ink-tertiary">
                      <span className="font-mono text-caption">{change.paperCode}</span> · {change.students} students
                    </span>
                  </td>
                  <td className="px-3 py-3 text-ink-secondary">{KINDS[change.kind] ?? change.kind}</td>
                  <td className="px-3 py-3">
                    <Place place={change.before} showPeople={change.kind === 'INVIGILATORS'} />
                  </td>
                  <td className="py-3 pl-3">
                    <Place place={change.after} showPeople={change.kind === 'INVIGILATORS'} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
