import { useState } from 'react'
import { Link } from 'react-router'
import { CalendarClock } from 'lucide-react'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatDay, formatTime, todayIso } from '../../../utils/format.js'
import { fetchMyExams } from '../services/schedulesApi.js'

function PaperList({ label, papers }) {
  return (
    <ul aria-label={label} className="border-y border-line">
      {papers.map((paper) => (
        <li
          key={`${paper.date}-${paper.start}-${paper.paperCode}`}
          className="grid gap-x-6 gap-y-1 border-b border-line py-3 last:border-b-0 sm:grid-cols-[12rem_minmax(0,1fr)]"
        >
          <div>
            <p className="text-callout font-medium text-ink">{formatDay(paper.date)}</p>
            <p className="font-mono text-caption text-ink-tertiary">
              {formatTime(paper.start)} – {formatTime(paper.end)}
            </p>
          </div>
          <div className="min-w-0">
            <p className="text-callout font-medium text-ink">{paper.paperName}</p>
            <p className="text-footnote text-ink-secondary">
              {paper.hallName} ({paper.hallCode}) · {paper.building}
            </p>
            <p className="text-footnote text-ink-tertiary">
              <span className="font-mono text-caption">{paper.paperCode}</span> · {paper.sessionName}
            </p>
          </div>
        </li>
      ))}
    </ul>
  )
}

// What a student opens the app for: which paper, when, and the hall to walk to.
export default function MyExams() {
  const { data, error, reload } = useApi(fetchMyExams)
  const [today] = useState(todayIso)

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-40 w-full rounded-lg" />
  if (!data.rollNo) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="This account is not on the student roster"
        description="Ask the exam office to link your sign-in to your roll number"
      />
    )
  }

  const coming = data.exams.filter((paper) => paper.date >= today)
  const finished = data.exams.filter((paper) => paper.date < today)

  return (
    <section aria-labelledby="my-exams">
      <p className="font-mono text-caption text-ink-tertiary">{data.rollNo}</p>
      <h2 id="my-exams" className="mt-0.5 text-title-1">
        Your exams
      </h2>
      <p className="mt-1 mb-6 text-callout text-ink-secondary">
        {data.fullName} · {data.programme}, {data.batch}
      </p>

      {coming.length > 0 ? (
        <PaperList label="Coming papers" papers={coming} />
      ) : (
        <EmptyState icon={CalendarClock} title="No papers coming up" description="Your papers and halls appear here once a timetable is published" />
      )}

      {finished.length > 0 && (
        <>
          <h3 className="mt-10 mb-2 text-footnote text-ink-tertiary">Finished</h3>
          <PaperList label="Finished papers" papers={finished} />
        </>
      )}

      <Link
        to="/timetables"
        className="mt-8 inline-block rounded-sm text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink"
      >
        See the full published timetables
      </Link>
    </section>
  )
}
