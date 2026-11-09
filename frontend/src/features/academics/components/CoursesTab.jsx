import { useState } from 'react'
import { GraduationCap, PencilLine, Plus, Trash2 } from 'lucide-react'
import { Link } from 'react-router'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Select from '../../../components/common/Select.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatLength } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { deleteCourse, listAllDepartments, listCourses } from '../services/academicsApi.js'
import CourseFormDialog from './CourseFormDialog.jsx'

const columns = [
  { key: 'name', header: 'Programme', primary: true, sortable: true },
  { key: 'code', header: 'Code', sortable: true, mono: true },
  { key: 'departmentName', header: 'Department', priority: 'low' },
  { key: 'totalSemesters', header: 'Length', sortable: true, render: (course) => formatLength(course.totalSemesters) },
  {
    key: 'curriculumCount',
    header: 'Curricula',
    numeric: true,
    render: (course) =>
      course.curriculumCount > 0 ? (
        <Link to={`/academics/curricula?courseId=${course.id}`} data-numeric className="rounded-sm text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
          {course.curriculumCount}
        </Link>
      ) : (
        <span data-numeric>0</span>
      ),
  },
]

export default function CoursesTab() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listCourses)
  const { data: departments } = useApi(listAllDepartments)
  const [form, setForm] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)

  const remove = async () => {
    setBusy(true)
    try {
      await deleteCourse(deleting.id)
      toast({ message: `${deleting.name} was deleted` })
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setBusy(false)
      setDeleting(null)
    }
  }

  const actions = (course) => (
    <Dropdown
      label={`Actions for ${course.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ course }) },
        {
          label: 'Delete',
          icon: Trash2,
          danger: true,
          disabled: course.curriculumCount > 0,
          onSelect: () => setDeleting(course),
        },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.departmentId)

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search programmes" value={query.q} onSearch={(value) => setFilter('q', value)} />
        </div>
        <div className="sm:w-72">
          <Select
            aria-label="Department"
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
          <Button icon={Plus} onClick={() => setForm({ course: null })} disabled={!departments?.length}>
            Add programme
          </Button>
        )}
      </div>

      <div>
        <DataTable
          caption="Programmes"
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
              icon={GraduationCap}
              title={filtered ? 'No programmes match' : 'No programmes yet'}
              description={
                filtered
                  ? 'Try another name, code or department'
                  : 'A programme is a degree a department runs, such as B.Tech Computer Engineering or MCA'
              }
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <CourseFormDialog
          course={form.course}
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
        open={Boolean(deleting)}
        title={`Delete ${deleting?.name ?? ''}?`}
        message="This cannot be undone. Only a programme with no curricula can be deleted."
        confirmLabel="Delete"
        loading={busy}
        onConfirm={remove}
        onClose={() => setDeleting(null)}
      />
    </>
  )
}
