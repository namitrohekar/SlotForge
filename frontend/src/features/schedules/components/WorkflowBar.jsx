import { useState } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDateTime } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { approverLabel } from '../../exams/sessionSteps.js'
import { approveVersion, publishVersion, submitVersion } from '../services/schedulesApi.js'
import ContinueAsDemo from './ContinueAsDemo.jsx'
import ReturnDialog from './ReturnDialog.jsx'

function statusLine(version, placedAll, progress, outOfDate) {
  if (version.status === 'PUBLISHED') return `Published ${formatDateTime(version.publishedAt)}`
  if (version.status === 'SUPERSEDED') return 'Replaced by a newer published timetable'
  if (outOfDate) return 'Fix these problems, then generate it again'
  if (version.status === 'REVIEWED') return `Approved by ${version.reviewedBy}. Ready to publish.`
  if (!placedAll) return 'Every paper must be placed before it can be approved'
  if (progress.approverIds.length === 0) return `There is no ${progress.approverPost}. Ask an administrator to appoint one.`
  return version.submittedAt ? `Waiting for ${approverLabel(progress)} to approve it` : 'Not sent for approval yet'
}

// One step at a time: the bar offers only what this person can do to this version now.
export default function WorkflowBar({ version, planning, progress, outOfDate, onChanged }) {
  const isApprover = useAuthStore((state) => progress.approverIds.includes(state.user?.id))
  const isCoordinator = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const [confirming, setConfirming] = useState(null)
  const [returning, setReturning] = useState(false)
  const [busy, setBusy] = useState(false)

  const placedAll = version.examsScheduled === version.examsTotal
  const sent = version.status === 'REVIEWED' || (version.status === 'DRAFT' && Boolean(version.submittedAt))
  const canSend = planning && isCoordinator && version.status === 'DRAFT' && !sent && placedAll && !outOfDate && progress.approverIds.length > 0
  const canApprove = planning && isApprover && version.status === 'DRAFT' && sent && placedAll && !outOfDate
  const canReturn = planning && isApprover && sent
  const canPublish = planning && isCoordinator && version.status === 'REVIEWED' && !outOfDate
  const canAct = canSend || canApprove || canPublish
  const isCoordinatorAccount = (user) => user.roles.includes('EXAM_COORDINATOR')
  const isApproverAccount = (user) => progress.approverIds.includes(user.id)
  const isInvigilatorAccount = (user) => user.roles.includes('INVIGILATOR')
  const nextPerson = version.status === 'PUBLISHED'
    ? isInvigilatorAccount
    : !planning || !placedAll || outOfDate || canAct
    ? null
    : version.status === 'REVIEWED' || (version.status === 'DRAFT' && !sent)
      ? isCoordinatorAccount
      : version.status === 'DRAFT'
        ? isApproverAccount
        : null

  const run = async (action, message) => {
    setBusy(true)
    try {
      await action()
      toast({ message })
      onChanged()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
      onChanged() // the refusal may mean this page is behind, so show the reason
    } finally {
      setBusy(false)
      setConfirming(null)
    }
  }

  const confirmations = {
    send: {
      title: `Send version ${version.versionNo} for approval?`,
      message: `It goes to ${approverLabel(progress)} for review`,
      confirmLabel: 'Send',
      onConfirm: () => run(() => submitVersion(version.id), 'Sent for approval'),
    },
    approve: {
      title: `Approve version ${version.versionNo}?`,
      message: 'The coordinator can then publish it',
      confirmLabel: 'Approve',
      onConfirm: () => run(() => approveVersion(version.id), 'Timetable approved'),
    },
    publish: {
      title: `Publish version ${version.versionNo}?`,
      message: 'This becomes the official timetable and locks the session. Reopen the session to plan a replacement.',
      confirmLabel: 'Publish',
      onConfirm: () => run(() => publishVersion(version.id), 'Timetable published'),
    },
  }
  const confirmation = confirmations[confirming]

  return (
    <div className="mb-6 space-y-3">
      {version.status === 'DRAFT' && version.reviewComment && (
        <Alert tone="warning" title="Sent back by the head of department">
          {version.reviewComment}
        </Alert>
      )}
      <div className="flex min-h-control flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <p className="min-w-0 text-callout text-ink-secondary">{statusLine(version, placedAll, progress, outOfDate)}</p>
        {(canAct || canReturn || nextPerson) && (
          <div className="flex flex-wrap items-center gap-2">
            {nextPerson && <ContinueAsDemo match={nextPerson} to={version.status === 'PUBLISHED' ? '/my/duties' : undefined} />}
            {canReturn && (
              <Button variant="secondary" onClick={() => setReturning(true)}>
                Send back
              </Button>
            )}
            {canSend && <Button onClick={() => setConfirming('send')}>Send for approval</Button>}
            {canApprove && <Button onClick={() => setConfirming('approve')}>Approve</Button>}
            {canPublish && <Button onClick={() => setConfirming('publish')}>Publish</Button>}
          </div>
        )}
      </div>

      {confirmation && <ConfirmDialog open variant="primary" loading={busy} onClose={() => setConfirming(null)} {...confirmation} />}
      {returning && (
        <ReturnDialog
          version={version}
          onClose={() => setReturning(false)}
          onReturned={() => {
            setReturning(false)
            toast({ message: 'Sent back to the coordinator' })
            onChanged()
          }}
        />
      )}
    </div>
  )
}
