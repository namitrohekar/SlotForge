import { useState } from 'react'
import { CalendarDays, Plus } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import Button from '../../../components/common/Button.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDate } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import SessionActionsMenu from '../components/SessionActionsMenu.jsx'
import SessionFormDialog from '../components/SessionFormDialog.jsx'
import { listSessions } from '../services/examsApi.js'

const columns = [
  {
    key: 'name',
    header: 'Session',
    primary: true,
    sortable: true,
    render: (session) => (
      <Link to={`/sessions/${session.id}`} className="rounded-sm text-ink hover:text-accent hover:underline">
        {session.name}
      </Link>
    ),
  },
  {
    key: 'startDate',
    header: 'Dates',
    sortable: true,
    render: (session) => `${formatDate(session.startDate)} – ${formatDate(session.endDate)}`,
  },
  { key: 'slotCount', header: 'Slots', numeric: true, render: (session) => <span data-numeric>{session.slotCount}</span> },
  { key: 'examinationCount', header: 'Papers', numeric: true, render: (session) => <span data-numeric>{session.examinationCount}</span> },
  { key: 'status', header: 'Status', render: (session) => <StatusBadge status={session.status} /> },
]

export default function SessionsPage() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const navigate = useNavigate()
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listSessions)
  const [creating, setCreating] = useState(false)

  const actions = (session) => <SessionActionsMenu session={session} showOpen onChanged={reload} onDeleted={reload} />

  return (
    <>
      <PageHeader
        title="Exam sessions"
        description="Each session holds its own dates, rules, slots and papers."
        actions={
          canEdit && (
            <Button icon={Plus} onClick={() => setCreating(true)}>
              New session
            </Button>
          )
        }
      />

      <div className="mb-4">
        <SearchInput label="Search sessions" value={query.q} onSearch={(value) => setFilter('q', value)} />
      </div>

      <div className="overflow-hidden rounded-lg border border-line">
        <DataTable
          caption="Exam sessions"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'startDate,desc'}
          onSort={setSort}
          actions={canEdit ? actions : undefined}
          empty={
            <EmptyState
              icon={CalendarDays}
              title={query.q ? 'No sessions match' : 'No exam sessions yet'}
              description={query.q ? 'Try another name.' : 'A timetable starts with a session: its dates and its rules.'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {creating && (
        <SessionFormDialog
          session={null}
          onClose={() => setCreating(false)}
          onSaved={(message, saved) => {
            setCreating(false)
            toast({ message })
            // A new session is empty, so go straight to giving it slots.
            navigate(`/sessions/${saved.id}`)
          }}
        />
      )}
    </>
  )
}
