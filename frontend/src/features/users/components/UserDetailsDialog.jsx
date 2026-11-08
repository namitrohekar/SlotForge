import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import { formatDateTime } from '../../../utils/format.js'
import { roleSummary } from '../../../utils/roles.js'

// Read-only view for accounts that cannot be edited.
export default function UserDetailsDialog({ user, onClose }) {
  const facts = [
    ['Email', user.email],
    ['Roles', roleSummary(user.roles)],
    ['Status', <StatusBadge key="status" status={user.status} />],
    ['Last sign-in', user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Never'],
    ['Added', user.createdAt ? formatDateTime(user.createdAt) : 'Unknown'],
  ]

  return (
    <Dialog
      open
      onClose={onClose}
      title={user.fullName}
      description="This account is deactivated. Reactivate it to make changes."
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <dl className="divide-y divide-line">
        {facts.map(([label, value]) => (
          <div key={label} className="grid grid-cols-[8rem_minmax(0,1fr)] items-center gap-3 py-2.5 text-callout">
            <dt className="text-ink-tertiary">{label}</dt>
            <dd className="min-w-0 break-words text-ink">{value}</dd>
          </div>
        ))}
      </dl>
    </Dialog>
  )
}
