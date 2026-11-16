import { useState } from 'react'
import { Ban, CircleCheck, Eye, PencilLine, Plus, Users } from 'lucide-react'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Select from '../../../components/common/Select.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDateTime } from '../../../utils/format.js'
import { ROLE_LABELS, roleSummary } from '../../../utils/roles.js'
import UserDetailsDialog from '../components/UserDetailsDialog.jsx'
import UserFormDialog from '../components/UserFormDialog.jsx'
import { activateUser, deactivateUser, listUsers } from '../services/usersApi.js'

const columns = [
  {
    key: 'fullName',
    header: 'Name',
    primary: true,
    sortable: true,
    render: (user) => (
      <>
        <span className="block">{user.fullName}</span>
        <span className="block text-footnote font-normal text-ink-tertiary">{user.email}</span>
      </>
    ),
  },
  { key: 'roles', header: 'Roles', render: (user) => roleSummary(user.roles) },
  { key: 'status', header: 'Status', render: (user) => <StatusBadge status={user.status} /> },
  {
    key: 'lastLoginAt',
    header: 'Last sign-in',
    sortable: true,
    priority: 'low',
    render: (user) => (user.lastLoginAt ? formatDateTime(user.lastLoginAt) : 'Never'),
  },
]

export default function UsersPage() {
  const currentUserId = useAuthStore((state) => state.user?.id)
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listUsers)
  // null when closed, { user: null } to add, { user } to edit.
  const [form, setForm] = useState(null)
  const [viewing, setViewing] = useState(null)
  const [deactivating, setDeactivating] = useState(null)
  const [reactivating, setReactivating] = useState(null)
  const [busy, setBusy] = useState(false)

  const run = async (action, message) => {
    setBusy(true)
    try {
      await action()
      toast({ message })
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setBusy(false)
      setDeactivating(null)
      setReactivating(null)
    }
  }

  // A deactivated account is read-only until it is explicitly reactivated. The API enforces the same.
  const actions = (user) => (
    <Dropdown
      label={`Actions for ${user.fullName}`}
      items={
        user.status === 'ACTIVE'
          ? [
              { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ user }) },
              {
                label: 'Deactivate',
                icon: Ban,
                danger: true,
                disabled: user.id === currentUserId,
                onSelect: () => setDeactivating(user),
              },
            ]
          : [
              { label: 'View details', icon: Eye, onSelect: () => setViewing(user) },
              { label: 'Reactivate', icon: CircleCheck, onSelect: () => setReactivating(user) },
            ]
      }
    />
  )

  return (
    <>
      <PageHeader
        title="Users"
        description="Who can sign in, and what each person may do"
      />

      <div className="mb-4 grid gap-2 sm:grid-cols-[minmax(0,1fr)_12rem_10rem_auto]">
        <SearchInput label="Search name or email" value={query.q} onSearch={(value) => setFilter('q', value)} />
        <Select aria-label="Role" value={query.role ?? ''} onChange={(event) => setFilter('role', event.target.value)}>
          <option value="">All roles</option>
          {Object.entries(ROLE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <Select aria-label="Status" value={query.status ?? ''} onChange={(event) => setFilter('status', event.target.value)}>
          <option value="">Any status</option>
          <option value="ACTIVE">Active</option>
          <option value="DEACTIVATED">Deactivated</option>
        </Select>
        <Button icon={Plus} onClick={() => setForm({ user: null })}>
          Add user
        </Button>
      </div>

      <div>
        <DataTable
          caption="Users"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'fullName,asc'}
          onSort={setSort}
          actions={actions}
          empty={<EmptyState icon={Users} title="No users match" description="Try another name, role or status" />}
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <UserFormDialog
          user={form.user}
          isSelf={form.user?.id === currentUserId}
          onClose={() => setForm(null)}
          onSaved={(message) => {
            setForm(null)
            toast({ message })
            reload()
          }}
        />
      )}

      {viewing && <UserDetailsDialog user={viewing} onClose={() => setViewing(null)} />}

      <ConfirmDialog
        open={Boolean(reactivating)}
        title={`Reactivate ${reactivating?.fullName ?? ''}?`}
        message={`They can sign in again as ${reactivating ? roleSummary(reactivating.roles) : ''}, and their account becomes editable`}
        confirmLabel="Reactivate"
        variant="primary"
        loading={busy}
        onConfirm={() => run(() => activateUser(reactivating.id), `${reactivating.fullName} can sign in again`)}
        onClose={() => setReactivating(null)}
      />

      <ConfirmDialog
        open={Boolean(deactivating)}
        title={`Deactivate ${deactivating?.fullName ?? ''}?`}
        message="They are signed out at once and cannot sign in until an administrator activates them again. Their history stays."
        confirmLabel="Deactivate"
        loading={busy}
        onConfirm={() => run(() => deactivateUser(deactivating.id), `${deactivating.fullName} was deactivated`)}
        onClose={() => setDeactivating(null)}
      />
    </>
  )
}
