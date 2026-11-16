import { FileText } from 'lucide-react'
import { Link } from 'react-router'
import DataTable from '../../../components/common/DataTable.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatDate, formatDay, formatTime } from '../../../utils/format.js'
import { listPublishedTimetables } from '../services/schedulesApi.js'

const columns = [
  { key: 'day', header: 'Day', render: (paper) => formatDay(paper.date) },
  { key: 'time', header: 'Time', mono: true, render: (paper) => `${formatTime(paper.start)} – ${formatTime(paper.end)}` },
  { key: 'paperCode', header: 'Code', mono: true },
  { key: 'paperName', header: 'Paper', primary: true },
  { key: 'halls', header: 'Halls', render: (paper) => paper.halls.join(', ') },
]

function Notice({ timetable }) {
  const rows = timetable.papers.map((paper) => ({ ...paper, id: `${paper.date}|${paper.start}|${paper.paperCode}` }))

  return (
    <section aria-label={timetable.sessionName}>
      <p className="font-mono text-caption text-ink-tertiary">
        {formatDate(timetable.startDate)} – {formatDate(timetable.endDate)}
      </p>
      <div className="mt-0.5 flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1">
        <h2 className="text-title-1">{timetable.sessionName}</h2>
        <Link
          to={`/print/notice/${timetable.sessionId}`}
          className="rounded-sm text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink"
        >
          Print notice
        </Link>
      </div>
      <p data-numeric className="mt-1 mb-4 text-footnote text-ink-tertiary">
        {rows.length} {rows.length === 1 ? 'paper' : 'papers'}
        {timetable.publishedAt && ` · published ${formatDate(timetable.publishedAt)}`}
      </p>
      <DataTable caption={`${timetable.sessionName} timetable`} columns={columns} rows={rows} />
    </section>
  )
}

// The noticeboard: every published timetable, open to everyone who can sign in.
export default function TimetablesPage() {
  const { data, error, reload } = useApi(listPublishedTimetables)

  return (
    <>
      <PageHeader title="Timetables" description="Published exam timetables: each paper, its day and time, and the halls it is held in" />
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {!data && !error && <Skeleton className="h-64 w-full rounded-lg" />}
      {data?.length === 0 && (
        <EmptyState icon={FileText} title="No timetable has been published yet" description="It appears here for everyone as soon as the exam office publishes it" />
      )}
      {data?.length > 0 && (
        <div className="space-y-14">
          {data.map((timetable) => (
            <Notice key={timetable.sessionId} timetable={timetable} />
          ))}
        </div>
      )}
    </>
  )
}
