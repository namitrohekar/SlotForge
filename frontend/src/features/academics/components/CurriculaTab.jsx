import { useState } from 'react'
import { Layers, PencilLine, Plus, Trash2 } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
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
import { deleteCurriculum, listAllCourses, listCurricula } from '../services/academicsApi.js'
import CurriculumFormDialog from './CurriculumFormDialog.jsx'

const columns = [
  {
    key: 'name',
    header: 'Curriculum',
    primary: true,
    sortable: true,
    render: (curriculum) => (
      <Link to={`/academics/curricula/${curriculum.id}`} className="rounded-sm text-ink hover:text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
        {curriculum.name}
      </Link>
    ),
  },
  { key: 'courseName', header: 'Programme' },
  { key: 'effectiveFromYear', header: 'In force from', sortable: true, render: (c) => <span data-numeric>{c.effectiveFromYear}</span> },
  { key: 'subjectCount', header: 'Subjects', numeric: true, render: (c) => <span data-numeric>{c.subjectCount}</span> },
  { key: 'batchCount', header: 'Batches', numeric: true, priority: 'low', render: (c) => <span data-numeric>{c.batchCount}</span> },
]

export default function CurriculaTab() {
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const navigate = useNavigate()
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(listCurricula)
  const { data: courses } = useApi(listAllCourses)
  const [form, setForm] = useState(null)
  const deletion = useDelete(deleteCurriculum, reload)

  const actions = (curriculum) => (
    <Dropdown
      label={`Actions for ${curriculum.name}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setForm({ curriculum }) },
        // A curriculum a batch follows is part of that batch's record, so it stays.
        {
          label: 'Delete',
          icon: Trash2,
          danger: true,
          disabled: curriculum.batchCount > 0,
          onSelect: () => deletion.ask(curriculum),
        },
      ]}
    />
  )

  const filtered = Boolean(query.q || query.courseId)

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search curricula" value={query.q} onSearch={(value) => setFilter('q', value)} />
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
          <Button icon={Plus} onClick={() => setForm({ curriculum: null })} disabled={!courses?.length}>
            Add curriculum
          </Button>
        )}
      </div>

      <div>
        <DataTable
          caption="Curricula"
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
              icon={Layers}
              title={filtered ? 'No curricula match' : 'No curricula yet'}
              description={
                filtered
                  ? 'Try another name or programme'
                  : 'A curriculum sets which subjects a programme teaches in each semester'
              }
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {form && (
        <CurriculumFormDialog
          curriculum={form.curriculum}
          courses={courses ?? []}
          defaultCourseId={query.courseId}
          onClose={() => setForm(null)}
          onSaved={(message, saved) => {
            setForm(null)
            toast({ message })
            // A new curriculum is empty, so go straight to placing its subjects.
            if (form.curriculum) reload()
            else navigate(`/academics/curricula/${saved.id}`)
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title={`Delete ${deletion.target?.name ?? ''}?`}
        message="Its subject placements go with it. This cannot be undone."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
