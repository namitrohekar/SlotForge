import { useState } from 'react'
import { PencilLine, Plus, Trash2, UsersRound } from 'lucide-react'
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
import { deleteBatch, listAllCourses, listAllCurricula, listBatches } from '../services/academicsApi.js'
import BatchFormDialog from './BatchFormDialog.jsx'

const columns = [
  { key: 'name', header: 'Batch', primary: true, sortable: true },
  { key: 'courseName', header: 'Programme' },
  { key: 'curriculumName', header: 'Curriculum', priority: 'low' },
  { key: 'admissionYear', header: 'Admitted', sortable: true, render: (batch) => <span data-numeric>{batch.admissionYear}</span> },
  {
    key: 'currentSemester',
    header: 'Semester',
    sortable: true,
    render: (batch) => (
      <span data-numeric>
        {batch.currentSemester} of {batch.totalSemesters}
      </span>
    ),
  },
  { key: 'studentCount', header: 'Students', numeric: true, render: (batch) => <span data-numeric>{batch.studentCount}</span> },
]

export default function BatchesTab() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listBatches)
  const { data: courses } = useApi(listAllCourses)
  const { data: curricula } = useApi(listAllCurricula)
  const [form, setForm] = useState(null)
  const deletion = useDelete(deleteBatch, reload)

  const actions = (batch) => (
    <Dropdown
      label={`Actions for ${batch.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ batch }) },
        // A batch with students cannot go; the count beside it says why.
        { label: 'Delete', icon: Trash2, danger: true, disabled: batch.studentCount > 0, onSelect: () => deletion.ask(batch) },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.courseId)

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search batches" value={query.q} onSearch={(value) => setFilter('q', value)} />
        </div>
        <div className="sm:w-72">
          <Select aria-label="Programme" value={query.courseId ?? ''} onChange={(event) => setFilter('courseId', event.target.value)}>
            <option value="">All programmes</option>
            {courses?.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </Select>
        </div>
        {canEdit && (
          <Button icon={Plus} onClick={() => setForm({ batch: null })} disabled={!curricula?.length}>
            Add batch
          </Button>
        )}
      </div>

      <div className="overflow-hidden rounded-lg border border-line">
        <DataTable
          caption="Batches"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'name,asc'}
          onSort={setSort}
          actions={canEdit ? actions : undefined}
          empty={
            <EmptyState
              icon={UsersRound}
              title={filtered ? 'No batches match' : 'No batches yet'}
              description={filtered ? 'Try another name or programme.' : 'A batch is the students admitted together under one curriculum.'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <BatchFormDialog
          batch={form.batch}
          curricula={curricula ?? []}
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
        message="This cannot be undone. Only a batch with no students can be deleted."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
