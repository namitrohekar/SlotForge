import { useCallback, useState } from 'react'
import { FileText, ListPlus, PencilLine, Trash2 } from 'lucide-react'
import { useOutletContext } from 'react-router'
import HeavyBadge from '../../../components/common/HeavyBadge.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import { useDelete } from '../../../hooks/useDelete.js'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDuration } from '../../../utils/format.js'
import { hasAnyRole } from '../../../utils/roles.js'
import { deleteExamination, listExaminations } from '../services/examsApi.js'
import EnrolmentDialog from './EnrolmentDialog.jsx'
import ExaminationFormDialog from './ExaminationFormDialog.jsx'
import FromCurriculumDialog from './FromCurriculumDialog.jsx'

const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`

export default function ExaminationsTab() {
  const { session, reloadSession, canEdit } = useOutletContext()
  const canSeeStudents = useAuthStore((state) => hasAnyRole(state.user, ['ADMIN', 'EXAM_COORDINATOR', 'HOD']))
  const toast = useToastStore((state) => state.push)
  const load = useCallback((query) => listExaminations(session.id, query), [session.id])
  const { data, error, loading, reload, query, setFilter, setPage, setSort } = usePagedList(load)
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState(null)
  const [students, setStudents] = useState(null)

  const refresh = () => {
    reload()
    reloadSession()
  }
  const deletion = useDelete(deleteExamination, refresh)

  const columns = [
    {
      key: 'subject.name',
      header: 'Paper',
      primary: true,
      sortable: true,
      render: (exam) => (
        <span className="inline-flex flex-wrap items-center gap-2">
          {exam.subjectName}
          {exam.heavy && <HeavyBadge />}
        </span>
      ),
    },
    { key: 'subject.code', header: 'Code', sortable: true, render: (exam) => <span data-numeric>{exam.subjectCode}</span> },
    { key: 'departmentCode', header: 'Taught by', priority: 'low' },
    { key: 'durationMinutes', header: 'Duration', sortable: true, render: (exam) => formatDuration(exam.durationMinutes) },
    {
      key: 'enrolled',
      header: 'Students',
      numeric: true,
      render: (exam) =>
        canSeeStudents ? (
          <button
            type="button"
            data-numeric
            aria-label={`${plural(exam.enrolled, 'student')} in ${exam.subjectName}`}
            onClick={() => setStudents(exam)}
            className="rounded-sm text-accent hover:underline"
          >
            {exam.enrolled}
          </button>
        ) : (
          <span data-numeric>{exam.enrolled}</span>
        ),
    },
    { key: 'status', header: 'Status', render: (exam) => <StatusBadge status={exam.status === 'DRAFT' ? 'NO_STUDENTS' : exam.status} /> },
  ]

  const actions = (exam) => (
    <Dropdown
      label={`Actions for ${exam.subjectName}`}
      items={[
        { label: 'Edit', icon: PencilLine, onSelect: () => setEditing(exam) },
        { label: 'Delete', icon: Trash2, danger: true, onSelect: () => deletion.ask({ id: exam.id, name: exam.subjectName }) },
      ]}
    />
  )

  const addButton = (
    <Button icon={ListPlus} onClick={() => setAdding(true)}>
      Add from curriculum
    </Button>
  )

  return (
    <>
      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1">
          <SearchInput label="Search papers" value={query.q} onSearch={(value) => setFilter('q', value)} />
        </div>
        {canEdit && addButton}
      </div>

      <div className="overflow-hidden rounded-lg border border-line">
        <DataTable
          caption="Papers"
          columns={columns}
          rows={data?.items}
          loading={loading}
          error={error}
          onRetry={reload}
          sort={query.sort ?? 'subject.code,asc'}
          onSort={setSort}
          actions={canEdit ? actions : undefined}
          empty={
            <EmptyState
              icon={FileText}
              title={query.q ? 'No papers match' : 'No papers yet'}
              description={query.q ? 'Try another name or code.' : 'Add the papers each batch sits in this session.'}
            />
          }
        />
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}

      {adding && (
        <FromCurriculumDialog
          session={session}
          onClose={() => setAdding(false)}
          onCreated={({ created, enrolled, electivesAwaitingStudents }) => {
            setAdding(false)
            const electives = electivesAwaitingStudents ? `, ${plural(electivesAwaitingStudents, 'elective')} awaiting students` : ''
            toast({ message: `${plural(created, 'paper')} added, ${plural(enrolled, 'enrolment')}${electives}` })
            refresh()
          }}
        />
      )}

      {editing && (
        <ExaminationFormDialog
          exam={editing}
          session={session}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            toast({ message: 'Paper updated' })
            reload()
          }}
        />
      )}

      {students && <EnrolmentDialog exam={students} canEdit={canEdit} onClose={() => setStudents(null)} onChanged={refresh} />}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title={`Delete ${deletion.target?.name ?? ''}?`}
        message="Its enrolment goes with it. This cannot be undone."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
