import { useCallback, useState } from 'react'
import { Link } from 'react-router'
import { CalendarCheck, CalendarDays, CalendarClock } from 'lucide-react'
import Alert from '../../../components/common/Alert.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import LinkButton from '../../../components/common/LinkButton.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useAuthStore } from '../../../store/authStore.js'
import { formatDate, formatDateTime, formatDay, formatLongDate } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import NextStepBanner from '../../exams/components/NextStepBanner.jsx'
import SessionSteps from '../../exams/components/SessionSteps.jsx'
import { getSession, listSessions } from '../../exams/services/examsApi.js'
import { nextStep, sessionSteps } from '../../exams/sessionSteps.js'
import { listAwaitingApproval, listDutiesAtRisk } from '../../schedules/services/schedulesApi.js'
import { fetchHealth } from '../../system/services/systemApi.js'

const loadSessions = () => listSessions({ size: 50, sort: 'startDate,asc' })
const dates = (session) => `${formatDate(session.startDate)} – ${formatDate(session.endDate)}`

function SystemLine() {
  const { data, error, loading } = useApi(fetchHealth)

  if (loading) return <Skeleton className="h-4.5 w-56" />

  return (
    <Link
      to="/system"
      className="sf-press mt-8 inline-flex items-center gap-2 rounded-sm text-footnote text-ink-secondary hover:text-ink [--press-scale:0.98]"
    >
      <span aria-hidden="true" className={`size-1.5 shrink-0 rounded-full ${error ? 'bg-danger' : 'bg-success'}`} />
      {error ? 'System not responding' : `Connected to ${data.database}, schema version ${data.schemaVersion}`}
    </Link>
  )
}

function CurrentSession({ id }) {
  const { data: session, error, reload } = useApi(useCallback(() => getSession(id), [id]))

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!session) return <Skeleton className="h-40 w-full rounded-lg" />

  const base = `/sessions/${id}`
  const next = nextStep(session, { isCoordinator: true })

  return (
    <section aria-labelledby="current-session">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-x-6 gap-y-1">
        <div className="min-w-0">
          <p className="text-footnote text-ink-tertiary">Being planned · {dates(session)}</p>
          <h2 id="current-session" className="mt-0.5 font-serif text-title-1 font-medium">
            {session.name}
          </h2>
        </div>
        <Link to={base} className="text-callout font-medium text-accent hover:underline">
          Open session
        </Link>
      </div>
      <SessionSteps steps={sessionSteps(session, true)} base={base} />
      {next && <NextStepBanner next={next} base={base} />}
    </section>
  )
}

function DutiesAtRisk() {
  const { data } = useApi(listDutiesAtRisk)
  if (!data?.length) return null

  return (
    <div className="mb-8">
      <Alert tone="danger" title={data.length === 1 ? '1 published duty has nobody able to do it' : `${data.length} published duties have nobody able to do them`}>
        <ul className="mt-1 space-y-0.5">
          {data.map((duty) => (
            <li key={`${duty.sessionId}-${duty.staffName}-${duty.date}-${duty.paperCode}`}>
              <Link to={`/sessions/${duty.sessionId}/timetable`} className="font-medium underline">
                {duty.sessionName}
              </Link>
              : {duty.staffName} cannot invigilate {duty.paperCode} in {duty.hallCode} on {formatDay(duty.date)}.
            </li>
          ))}
        </ul>
      </Alert>
    </div>
  )
}

function CoordinatorHome() {
  const { data, error, reload } = useApi(loadSessions)

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-64 w-full rounded-lg" />

  const current = data.items.find((session) => session.status === 'PLANNING')
  const others = data.items.filter((session) => session !== current)

  return (
    <>
      <DutiesAtRisk />
      {current ? (
        <CurrentSession id={current.id} />
      ) : (
        <EmptyState
          icon={CalendarDays}
          title="No session is being planned"
          description="A timetable starts with a session: its dates and its rules."
          action={<LinkButton to="/sessions">Go to exam sessions</LinkButton>}
        />
      )}
      {others.length > 0 && (
        <section aria-labelledby="other-sessions" className="mt-10">
          <h2 id="other-sessions" className="mb-2 text-footnote font-medium text-ink-secondary">
            Other sessions
          </h2>
          <ul className="border-t border-line">
            {others.map((session) => (
              <li key={session.id} className="flex min-h-12 items-center justify-between gap-4 border-b border-line py-2">
                <Link to={`/sessions/${session.id}`} className="min-w-0 truncate rounded-sm text-callout font-medium text-ink hover:text-accent">
                  {session.name}
                </Link>
                <StatusBadge status={session.status} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}

function ReviewerHome() {
  const { data, error, reload } = useApi(listAwaitingApproval)

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-40 w-full rounded-lg" />
  if (data.length === 0) {
    return <EmptyState icon={CalendarCheck} title="Nothing is waiting for you" description="Timetables that need your approval appear here." />
  }

  return (
    <section aria-labelledby="awaiting">
      <h2 id="awaiting" className="mb-3 font-serif text-title-2 font-medium">
        Waiting for your approval
      </h2>
      <ul className="overflow-hidden rounded-lg border border-line">
        {data.map((item) => (
          <li key={item.versionId} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line px-4 py-3 last:border-b-0">
            <div className="min-w-0">
              <p className="text-callout font-medium text-ink">{item.sessionName}</p>
              <p data-numeric className="text-footnote text-ink-tertiary">
                Version {item.versionNo} · {item.papers} {item.papers === 1 ? 'paper' : 'papers'} · generated {formatDateTime(item.generatedAt)}
              </p>
            </div>
            <LinkButton to={`/sessions/${item.sessionId}/timetable?version=${item.versionId}`}>Review</LinkButton>
          </li>
        ))}
      </ul>
    </section>
  )
}

function PersonalHome() {
  const isStaff = useAuthStore((state) => hasAnyRole(state.user, ['FACULTY', 'INVIGILATOR']))
  if (isStaff) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="Your invigilation duties"
        description="See where and when you are on duty, and tell us the dates you cannot invigilate."
        action={<LinkButton to="/my/duties">Open My duties</LinkButton>}
      />
    )
  }
  return (
    <EmptyState
      icon={CalendarClock}
      title="No timetable to show yet"
      description="Your exam timetable will appear here once it has been published."
    />
  )
}

// Each role opens on what it needs to act on today.
export default function DashboardPage() {
  const [today] = useState(() => formatLongDate(new Date()))
  const isAdmin = useAuthStore((state) => hasAnyRole(state.user, ['ADMIN']))
  const isCoordinator = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const isHod = useAuthStore((state) => hasAnyRole(state.user, ['HOD']))

  return (
    <>
      <PageHeader eyebrow={today} title="Dashboard" />
      {isCoordinator ? <CoordinatorHome /> : isHod ? <ReviewerHome /> : <PersonalHome />}
      {isAdmin && <SystemLine />}
    </>
  )
}
