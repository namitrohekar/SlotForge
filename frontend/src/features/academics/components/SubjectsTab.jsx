import { useState } from 'react'
import { BookOpen, PencilLine, Plus, Trash2 } from 'lucide-react'
import HeavyBadge from '../../../components/common/HeavyBadge.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Select from '../../../components/common/Select.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useDelete } from '../../../hooks/useDelete.js'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { deleteSubject, listAllDepartments, listSubjects } from '../services/academicsApi.js'
import SubjectFormDialog from './SubjectFormDialog.jsx'

const columns = [
  {
    key: 'name',
    header: 'Subject',
    primary: true,
    sortable: true,
    render: (subject) => (
      <span className="inline-flex flex-wrap items-center gap-2">
        {subject.name}
        {subject.heavy && <HeavyBadge />}
      </span>
    ),
  },
  { key: 'code', header: 'Code', sortable: true, mono: true },
  { key: 'departmentName', header: 'Taught by', priority: 'low' },
  {
    key: 'curriculumCount',
    header: 'Curricula',
    numeric: true,
    render: (subject) => <span data-numeric>{subject.curriculumCount}</span>,
  },
]

export default function SubjectsTab() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listSubjects)
  const { data: departments } = useApi(listAllDepartments)
  const [form, setForm] = useState(null)
  const deletion = useDelete(deleteSubject, reload)

  const actions = (subject) => (
    <Dropdown
      label={`Actions for ${subject.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ subject }) },
        // A subject placed in a curriculum cannot go; the count beside it says why.
        {
          label: 'Delete',
          icon: Trash2,
          danger: true,
          disabled: subject.curriculumCount > 0,
          onSelect: () => deletion.ask(subject),
        },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.departmentId)

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search subjects" value={query.q} onSearch={(value) => setFilter('q', value)} />
        </div>
        <div className="sm:w-72">
          <Select
            aria-label="Taught by"
            value={query.departmentId ?? ''}
            onChange={(event) => setFilter('departmentId', event.target.value)}
          >
            <option value="">All departments</option>
            {departments?.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </Select>
        </div>
        {canEdit && (
          <Button icon={Plus} onClick={() => setForm({ subject: null })} disabled={!departments?.length}>
            Add subject
          </Button>
        )}
      </div>

      <div>
        <DataTable
          caption="Subjects"
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
              icon={BookOpen}
              title={filtered ? 'No subjects match' : 'No subjects yet'}
              description={
                filtered
                  ? 'Try another name, code or department'
                  : 'A subject is one paper, owned by the department that teaches it'
              }
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <SubjectFormDialog
          subject={form.subject}
          departments={departments ?? []}
          defaultDepartmentId={query.departmentId}
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
        message="This cannot be undone. Only a subject in no curriculum can be deleted."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
