import { useState } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDateTime } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { approveVersion, publishVersion } from '../services/schedulesApi.js'
import ReturnDialog from './ReturnDialog.jsx'

function statusLine(version, placedAll, activeApprovers, outOfDate) {
  if (version.status === 'PUBLISHED') return `Published ${formatDateTime(version.publishedAt)}.`
  if (version.status === 'SUPERSEDED') return 'Replaced by a newer published timetable.'
  if (outOfDate) return 'Out of date. It has to be generated again.'
  if (version.status === 'REVIEWED') return `Approved by ${version.reviewedBy}. Ready to publish.`
  if (!placedAll) return 'Every paper must be placed before it can be approved.'
  return activeApprovers > 0 ? 'Waiting for a head of department to approve it.' : 'No head of department can sign in to approve this. Ask an administrator to activate one.'
}

// One step at a time: the bar offers only what this person can do to this version now.
export default function WorkflowBar({ version, planning, activeApprovers, outOfDate, onChanged }) {
  const isHod = useAuthStore((state) => hasAnyRole(state.user, ['HOD']))
  const isCoordinator = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const [confirming, setConfirming] = useState(null)
  const [returning, setReturning] = useState(false)
  const [busy, setBusy] = useState(false)

  const placedAll = version.examsScheduled === version.examsTotal
  const canApprove = planning && isHod && version.status === 'DRAFT' && placedAll && !outOfDate
  const canReturn = planning && isHod && ['DRAFT', 'REVIEWED'].includes(version.status)
  const canPublish = planning && isCoordinator && version.status === 'REVIEWED' && !outOfDate

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
    approve: {
      title: `Approve version ${version.versionNo}?`,
      message: 'The coordinator can then publish it.',
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
      <div className="flex min-h-14 flex-wrap items-center justify-between gap-x-4 gap-y-2 rounded-lg border border-line px-4 py-2.5">
        <div className="flex min-w-0 items-center gap-3">
          <StatusBadge status={version.status} />
          <p className="text-callout text-ink-secondary">{statusLine(version, placedAll, activeApprovers, outOfDate)}</p>
        </div>
        {(canApprove || canReturn || canPublish) && (
          <div className="flex items-center gap-2">
            {canReturn && (
              <Button variant="secondary" onClick={() => setReturning(true)}>
                Send back
              </Button>
            )}
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
