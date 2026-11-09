import { useState } from 'react'
import { Building2, PencilLine, Plus, Trash2, UserRoundCheck } from 'lucide-react'
import { Link } from 'react-router'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { deleteDepartment, listDepartments } from '../services/academicsApi.js'
import AppointHeadDialog from './AppointHeadDialog.jsx'
import DepartmentFormDialog from './DepartmentFormDialog.jsx'

const columns = [
  { key: 'name', header: 'Department', primary: true, sortable: true },
  { key: 'code', header: 'Code', sortable: true, mono: true },
  {
    key: 'headName',
    header: 'Head',
    render: (department) => department.headName ?? <span className="text-ink-tertiary">Not appointed</span>,
  },
  {
    key: 'courseCount',
    header: 'Programmes',
    numeric: true,
    render: (department) =>
      department.courseCount > 0 ? (
        <Link
          to={`/academics/courses?departmentId=${department.id}`}
          data-numeric
          className="rounded-sm text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink"
        >
          {department.courseCount}
        </Link>
      ) : (
        <span data-numeric>0</span>
      ),
  },
  {
    key: 'subjectCount',
    header: 'Subjects',
    numeric: true,
    render: (department) =>
      department.subjectCount > 0 ? (
        <Link
          to={`/academics/subjects?departmentId=${department.id}`}
          data-numeric
          className="rounded-sm text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink"
        >
          {department.subjectCount}
        </Link>
      ) : (
        <span data-numeric>0</span>
      ),
  },
]

export default function DepartmentsTab() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const canAppoint = useAuthStore((state) => hasAnyRole(state.user, ['ADMIN']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listDepartments)
  // null when closed, { department: null } to add, { department } to edit.
  const [form, setForm] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [appointing, setAppointing] = useState(null)
  const [busy, setBusy] = useState(false)

  const remove = async () => {
    setBusy(true)
    try {
      await deleteDepartment(deleting.id)
      toast({ message: `${deleting.name} was deleted` })
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setBusy(false)
      setDeleting(null)
    }
  }

  const actions = (department) => (
    <Dropdown
      label={`Actions for ${department.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ department }) },
        ...(canAppoint ? [{ label: 'Appoint head', icon: UserRoundCheck, onSelect: () => setAppointing(department) }] : []),
        // Only a department nothing refers to can go; the counts beside it say why.
        {
          label: 'Delete',
          icon: Trash2,
          danger: true,
          disabled: department.courseCount + department.subjectCount > 0,
          onSelect: () => setDeleting(department),
        },
      ]}
    />
  )

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search departments" value={query.q} onSearch={(value) => setFilter('q', value)} />
        </div>
        {canEdit && (
          <Button icon={Plus} onClick={() => setForm({ department: null })}>
            Add department
          </Button>
        )}
      </div>

      <div>
        <DataTable
          caption="Departments"
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
              icon={Building2}
              title={query.q ? 'No departments match' : 'No departments yet'}
              description={query.q ? 'Try another name or code' : 'Departments come first: every programme and subject belongs to one'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <DepartmentFormDialog
          department={form.department}
          onClose={() => setForm(null)}
          onSaved={(message) => {
            setForm(null)
            toast({ message })
            reload()
          }}
        />
      )}

      {appointing && (
        <AppointHeadDialog
          department={appointing}
          onClose={() => setAppointing(null)}
          onSaved={(message) => {
            setAppointing(null)
            toast({ message })
            reload()
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deleting)}
        title={`Delete ${deleting?.name ?? ''}?`}
        message="This cannot be undone. Only a department with no programmes or subjects can be deleted."
        confirmLabel="Delete"
        loading={busy}
        onConfirm={remove}
        onClose={() => setDeleting(null)}
      />
    </>
  )
}
