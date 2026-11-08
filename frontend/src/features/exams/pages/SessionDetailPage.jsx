import { useCallback } from 'react'
import { ChevronLeft } from 'lucide-react'
import { Link, Outlet, useLocation, useNavigate, useParams } from 'react-router'
import Alert from '../../../components/common/Alert.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useAuthStore } from '../../../store/authStore.js'
import { formatDate } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import NextStepBanner from '../components/NextStepBanner.jsx'
import SessionActionsMenu from '../components/SessionActionsMenu.jsx'
import SessionSteps from '../components/SessionSteps.jsx'
import { nextStep, sessionSteps } from '../sessionSteps.js'
import { getSession } from '../services/examsApi.js'

const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`

export default function SessionDetailPage() {
  const { id } = useParams()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const isCoordinator = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const isHod = useAuthStore((state) => hasAnyRole(state.user, ['HOD']))
  // Drafts are visible only to the people who produce and review them.
  const canSeeTimetable = useAuthStore((state) => hasAnyRole(state.user, ['ADMIN', 'EXAM_COORDINATOR', 'HOD']))
  const { data: session, error, reload } = useApi(useCallback(() => getSession(id), [id]))

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!session) return <Skeleton className="h-96 w-full rounded-lg" />

  const rest = session.heavyGapDays ? `${plural(session.heavyGapDays, 'rest day')} between heavy papers` : 'no rest day needed between heavy papers'
  const rules = `${plural(session.maxExamsPerStudentPerDay, 'paper')} per student per day, ${rest}.`
  const next = nextStep(session, { isCoordinator, isHod })

  return (
    <>
      <Link
        to="/sessions"
        className="sf-press mb-4 inline-flex items-center gap-1 rounded-sm text-footnote text-ink-secondary hover:text-ink"
      >
        <ChevronLeft size={16} aria-hidden="true" />
        Exam sessions
      </Link>
      <PageHeader
        eyebrow={`${formatDate(session.startDate)} – ${formatDate(session.endDate)}`}
        title={session.name}
        description={rules}
        actions={
          <>
            <StatusBadge status={session.status} />
            {isCoordinator && <SessionActionsMenu session={session} onChanged={reload} onDeleted={() => navigate('/sessions')} />}
          </>
        }
      />
      {session.status === 'CANCELLED' && (
        <div className="mb-6">
          <Alert tone="warning">This session is cancelled. Everything in it is kept, but nothing can change until it is reopened.</Alert>
        </div>
      )}
      <SessionSteps steps={sessionSteps(session, canSeeTimetable)} />
      {next && next.to !== pathname.split('/').pop() && <NextStepBanner next={next} base={`/sessions/${id}`} />}
      <Outlet context={{ session, reloadSession: reload, canEdit: isCoordinator && session.status === 'PLANNING' }} />
    </>
  )
}
