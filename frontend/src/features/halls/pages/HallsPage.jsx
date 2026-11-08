import { useState } from 'react'
import { DoorOpen, PencilLine, Plus, Trash2 } from 'lucide-react'
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
import { useDelete } from '../../../hooks/useDelete.js'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { hasAnyRole } from '../../../utils/roles.js'
import HallFormDialog from '../components/HallFormDialog.jsx'
import { deleteHall, listHalls } from '../services/hallsApi.js'

const columns = [
  { key: 'name', header: 'Hall', primary: true, sortable: true },
  { key: 'code', header: 'Code', sortable: true },
  {
    key: 'building',
    header: 'Building',
    sortable: true,
    priority: 'low',
    render: (hall) => (hall.floor == null ? hall.building : `${hall.building}, floor ${hall.floor}`),
  },
  { key: 'capacity', header: 'Exam seats', numeric: true, sortable: true, render: (hall) => <span data-numeric>{hall.capacity}</span> },
  { key: 'status', header: 'Status', render: (hall) => <StatusBadge status={hall.status} /> },
]

export default function HallsPage() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listHalls)
  const [form, setForm] = useState(null)
  const deletion = useDelete(deleteHall, reload)

  const actions = (hall) => (
    <Dropdown
      label={`Actions for ${hall.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ hall }) },
        // A hall in a timetable is part of that record. It can be marked Retired instead.
        { label: 'Delete', icon: Trash2, danger: true, disabled: hall.allocationCount > 0, onSelect: () => deletion.ask(hall) },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.status)

  return (
    <>
      <PageHeader
        title="Halls"
        description="The rooms exams can be held in, and how many students each can seat."
        actions={
          canEdit && (
            <Button icon={Plus} onClick={() => setForm({ hall: null })}>
              Add hall
            </Button>
          )
        }
      />

      <div className="mb-4 grid gap-2 sm:grid-cols-[minmax(0,1fr)_12rem]">
        <SearchInput label="Search halls" value={query.q} onSearch={(value) => setFilter('q', value)} />
        <Select aria-label="Status" value={query.status ?? ''} onChange={(event) => setFilter('status', event.target.value)}>
          <option value="">Any status</option>
          <option value="AVAILABLE">Available</option>
          <option value="MAINTENANCE">Maintenance</option>
          <option value="RETIRED">Retired</option>
        </Select>
      </div>

      <div className="overflow-hidden rounded-lg border border-line">
        <DataTable
          caption="Halls"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'code,asc'}
          onSort={setSort}
          actions={canEdit ? actions : undefined}
          empty={
            <EmptyState
              icon={DoorOpen}
              title={filtered ? 'No halls match' : 'No halls yet'}
              description={filtered ? 'Try another name, building or status.' : 'Timetables need halls to place exams in.'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <HallFormDialog
          hall={form.hall}
          onClose={() => setForm(null)}
          onSaved={(message) => {
            setForm(null)
            toast({ message })
            reload()
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title={`Delete ${deletion.target?.name ?? ''}?`}
        message="This cannot be undone."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
