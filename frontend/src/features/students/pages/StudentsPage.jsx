import { useState } from 'react'
import { PencilLine, Plus, Trash2, UserRound } from 'lucide-react'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Select from '../../../components/common/Select.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useDelete } from '../../../hooks/useDelete.js'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { listAllBatches } from '../../academics/services/academicsApi.js'
import StudentFormDialog from '../components/StudentFormDialog.jsx'
import { deleteStudent, listStudents } from '../services/studentsApi.js'

const columns = [
  {
    key: 'fullName',
    header: 'Student',
    primary: true,
    sortable: true,
    render: (student) => (
      <>
        <span className="block">{student.fullName}</span>
        <span className="block text-footnote font-normal text-ink-tertiary">{student.email}</span>
      </>
    ),
  },
  { key: 'rollNo', header: 'Roll number', sortable: true, render: (student) => <span data-numeric>{student.rollNo}</span> },
  { key: 'batchName', header: 'Batch', render: (student) => `${student.courseCode} · ${student.batchName}` },
  { key: 'currentSemester', header: 'Semester', numeric: true, priority: 'low', render: (student) => <span data-numeric>{student.currentSemester}</span> },
  { key: 'enrolmentCount', header: 'Papers', numeric: true, render: (student) => <span data-numeric>{student.enrolmentCount}</span> },
]

export default function StudentsPage() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listStudents)
  const { data: batches } = useApi(listAllBatches)
  const [form, setForm] = useState(null)
  const deletion = useDelete(deleteStudent, reload)

  const actions = (student) => (
    <Dropdown
      label={`Actions for ${student.fullName}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ student }) },
        // Enrolment is what the scheduler reads, so it has to be removed first.
        {
          label: 'Delete',
          icon: Trash2,
          danger: true,
          disabled: student.enrolmentCount > 0,
          onSelect: () => deletion.ask({ id: student.id, name: student.fullName }),
        },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.batchId)

  return (
    <>
      <PageHeader
        title="Students"
        description="The roster the timetable is built for. A student does not need a login to be scheduled."
        actions={
          canEdit && (
            <Button icon={Plus} onClick={() => setForm({ student: null })} disabled={!batches?.length}>
              Add student
            </Button>
          )
        }
      />

      <div className="mb-4 grid gap-2 sm:grid-cols-[minmax(0,1fr)_16rem]">
        <SearchInput label="Search name, roll number or email" value={query.q} onSearch={(value) => setFilter('q', value)} />
        <Select aria-label="Batch" value={query.batchId ?? ''} onChange={(event) => setFilter('batchId', event.target.value)}>
          <option value="">All batches</option>
          {batches?.map((batch) => (
            <option key={batch.id} value={batch.id}>
              {batch.courseCode} · {batch.name}
            </option>
          ))}
        </Select>
      </div>

      <div className="overflow-hidden rounded-lg border border-line">
        <DataTable
          caption="Students"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'rollNo,asc'}
          onSort={setSort}
          actions={canEdit ? actions : undefined}
          empty={
            <EmptyState
              icon={UserRound}
              title={filtered ? 'No students match' : 'No students yet'}
              description={filtered ? 'Try another name, roll number or batch.' : 'Every student belongs to a batch.'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <StudentFormDialog
          student={form.student}
          batches={batches ?? []}
          defaultBatchId={query.batchId}
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
        message="This cannot be undone. Only a student with no exam enrolment can be deleted."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
