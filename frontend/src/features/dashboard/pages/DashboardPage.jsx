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
import { formatDate, formatDateTime, formatDay, formatLongDate, formatTime, todayIso } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import ExamPeriodRuler from '../components/ExamPeriodRuler.jsx'
import NextStepBanner from '../../exams/components/NextStepBanner.jsx'
import SessionSteps from '../../exams/components/SessionSteps.jsx'
import { getSession, listSessions } from '../../exams/services/examsApi.js'
import { nextStep, sessionSteps } from '../../exams/sessionSteps.js'
import MyExams from '../../schedules/components/MyExams.jsx'
import { listAwaitingApproval, listDecisions, listDutiesAtRisk, listMyDuties } from '../../schedules/services/schedulesApi.js'

const loadSessions = () => listSessions({ size: 50, sort: 'startDate,asc' })
const dates = (session) => `${formatDate(session.startDate)} – ${formatDate(session.endDate)}`

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
          <p className="font-mono text-caption text-ink-tertiary">{dates(session)}</p>
          <h2 id="current-session" className="mt-0.5 text-title-1">
            {session.name}
          </h2>
        </div>
        <Link to={base} className="text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
          Open session
        </Link>
      </div>
      <SessionSteps steps={sessionSteps(session, true)} base={base} />
      {next && <NextStepBanner next={next} base={base} />}
      <ExamPeriodRuler session={session} />
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
              : {duty.staffName} cannot invigilate {duty.paperCode} in {duty.hallCode} on {formatDay(duty.date)}
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
          description="A timetable starts with a session: its dates and its rules"
          action={<LinkButton to="/sessions">Go to exam sessions</LinkButton>}
        />
      )}
      {others.length > 0 && (
        <section aria-labelledby="other-sessions" className="mt-10">
          <h2 id="other-sessions" className="mb-2 text-footnote text-ink-tertiary">
            Other sessions
          </h2>
          <ul className="border-t border-line">
            {others.map((session) => (
              <li key={session.id} className="flex min-h-12 items-center justify-between gap-4 border-b border-line py-2">
                <Link to={`/sessions/${session.id}`} className="min-w-0 truncate rounded-sm text-callout font-medium text-ink decoration-line-strong underline-offset-2 hover:underline">
                  {session.name}
                </Link>
                <span className="flex shrink-0 items-center gap-6">
                  <span className="hidden font-mono text-caption text-ink-tertiary sm:inline">{dates(session)}</span>
                  <StatusBadge status={session.status} />
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}

function Decisions() {
  const { data } = useApi(listDecisions)
  if (!data?.length) return null

  return (
    <section aria-labelledby="decisions" className="mt-10">
      <h2 id="decisions" className="mb-2 text-footnote text-ink-tertiary">
        Timetables you approved
      </h2>
      <ul className="border-t border-line">
        {data.map((decision) => (
          <li key={`${decision.sessionId}-${decision.versionNo}`} className="flex min-h-12 items-center justify-between gap-4 border-b border-line py-2">
            <Link
              to={`/sessions/${decision.sessionId}/timetable`}
              className="min-w-0 truncate rounded-sm text-callout font-medium text-ink decoration-line-strong underline-offset-2 hover:underline"
            >
              {decision.sessionName}
            </Link>
            <span className="flex shrink-0 items-center gap-6">
              <span className="hidden font-mono text-caption text-ink-tertiary sm:inline">
                Version {decision.versionNo} · {formatDate(decision.reviewedAt)}
              </span>
              <StatusBadge status={decision.status} />
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

function ReviewerHome() {
  const { data, error, reload } = useApi(listAwaitingApproval)

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-40 w-full rounded-lg" />

  return (
    <>
      {data.length === 0 ? (
        <EmptyState icon={CalendarCheck} title="Nothing is waiting for you" description="Timetables that need your approval appear here" />
      ) : (
        <section aria-labelledby="awaiting">
          <h2 id="awaiting" className="mb-3 text-title-2">
            Waiting for your approval
          </h2>
          <ul className="border-y border-line">
            {data.map((item) => (
              <li key={item.versionId} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line py-3 last:border-b-0">
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
      )}
      <Decisions />
    </>
  )
}

// The one thing an invigilator opens the app to learn: where to be next, and when.
function NextDuty() {
  const { data, error, reload } = useApi(listMyDuties)
  const [today] = useState(todayIso)

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-40 w-full rounded-lg" />

  const [next, ...later] = data.filter((duty) => duty.date >= today)
  if (!next) {
    return (
      <EmptyState
        icon={CalendarClock}
        title="No duties coming up"
        description="A duty appears here as soon as a timetable that includes you is published"
        action={<LinkButton to="/my/duties">Open My duties</LinkButton>}
      />
    )
  }

  return (
    <section aria-labelledby="next-duty">
      <p className="text-footnote text-ink-tertiary">Your next duty</p>
      <h2 id="next-duty" data-numeric className="mt-0.5 text-title-1">
        {formatDay(next.date)}, {formatTime(next.start)} – {formatTime(next.end)}
      </h2>
      <p className="mt-2 text-body text-ink">
        {next.hallName} ({next.hallCode}) · {next.building}
      </p>
      <p data-numeric className="mt-0.5 text-callout text-ink-secondary">
        <span className="font-mono text-caption">{next.paperCode}</span> {next.paperName} · {next.students} students · {next.sessionName}
      </p>
      {next.unavailable && <p className="mt-2 text-footnote text-danger">You marked this date as unavailable. The exam office has been alerted.</p>}
      <div className="mt-6 flex flex-wrap items-center justify-between gap-x-6 gap-y-2 border-t border-line pt-4">
        <p data-numeric className="text-callout text-ink-secondary">
          {later.length === 0 ? 'No other duty after this one' : `${later.length} more ${later.length === 1 ? 'duty' : 'duties'} after this one`}
        </p>
        <LinkButton to="/my/duties">Open My duties</LinkButton>
      </div>
    </section>
  )
}

function PersonalHome() {
  const isStaff = useAuthStore((state) => hasAnyRole(state.user, ['FACULTY', 'INVIGILATOR']))
  return isStaff ? <NextDuty /> : <MyExams />
}

// Each role opens on what it needs to act on today.
export default function DashboardPage() {
  const [today] = useState(() => formatLongDate(new Date()))
  const isCoordinator = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const isHod = useAuthStore((state) => hasAnyRole(state.user, ['HOD']))

  return (
    <>
      <PageHeader eyebrow={today} title="Dashboard" />
      {isCoordinator ? <CoordinatorHome /> : isHod ? <ReviewerHome /> : <PersonalHome />}
    </>
  )
}
