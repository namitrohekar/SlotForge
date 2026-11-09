import { useCallback, useState } from 'react'
import { Plus, X } from 'lucide-react'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { listStudents } from '../../students/services/studentsApi.js'
import { addEnrolments, listEnrolments, removeEnrolment } from '../services/examsApi.js'

const PAGE_SIZE = 8

export default function EnrolmentDialog({ exam, canEdit, onClose, onChanged }) {
  const toast = useToastStore((state) => state.push)
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [busy, setBusy] = useState(false)
  const enrolled = useApi(useCallback(() => listEnrolments(exam.id, { q: filter, page, size: PAGE_SIZE }), [exam.id, filter, page]))
  const candidates = useApi(useCallback(() => (search ? listStudents({ q: search, size: 5 }) : Promise.resolve(null)), [search]))

  // The action returns the message to show, because whether anything changed is only known afterwards.
  const change = async (action) => {
    setBusy(true)
    try {
      toast({ message: await action() })
      enrolled.reload()
      onChanged()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setBusy(false)
    }
  }

  const applyFilter = (value) => {
    setFilter(value)
    setPage(0)
  }

  const results = enrolled.data

  return (
    <Dialog
      open
      onClose={onClose}
      title={exam.subjectName}
      description={`${exam.subjectCode}. Only students listed here sit this paper.`}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Done
        </Button>
      }
    >
      <div className="space-y-5">
        {canEdit && (
          <section aria-label="Add a student">
            <SearchInput label="Add a student by name or roll number" value={search} onSearch={setSearch} />
            {search && candidates.data?.items.length === 0 && (
              <p className="mt-2 text-callout text-ink-tertiary">No student matches</p>
            )}
            {search && candidates.data?.items.length > 0 && (
              <ul className="mt-2 border-y border-line">
                {candidates.data.items.map((student) => (
                  <li key={student.id} className="flex min-h-12 items-center gap-3 border-b border-line py-1 pr-1 pl-3 last:border-b-0">
                    <span data-numeric className="w-24 shrink-0 text-footnote text-ink-tertiary">
                      {student.rollNo}
                    </span>
                    <span className="min-w-0 flex-1 truncate text-callout text-ink">{student.fullName}</span>
                    <Button
                      variant="quiet"
                      icon={Plus}
                      disabled={busy}
                      aria-label={`Add ${student.fullName}`}
                      onClick={() =>
                        change(async () => {
                          const { added } = await addEnrolments(exam.id, [student.id])
                          return added ? `${student.fullName} added` : `${student.fullName} is already enrolled`
                        })
                      }
                    >
                      Add
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}

        <section aria-label="Enrolled students">
          <div className="mb-2 flex items-center justify-between gap-4">
            <h3 className="text-footnote font-medium text-ink">Enrolled</h3>
            {results && (
              <p data-numeric className="text-footnote text-ink-tertiary">
                {results.totalItems} {results.totalItems === 1 ? 'student' : 'students'}
              </p>
            )}
          </div>
          <SearchInput label="Filter enrolled students" value={filter} onSearch={applyFilter} />
          {enrolled.error && <p className="mt-2 text-callout text-danger">{enrolled.error.message}</p>}
          {!results && !enrolled.error && <Skeleton className="mt-2 h-40 w-full rounded-md" />}
          {results?.items.length === 0 && (
            <p className="py-6 text-center text-callout text-ink-tertiary">{filter ? 'No enrolled student matches' : 'Nobody is enrolled yet'}</p>
          )}
          {results?.items.length > 0 && (
            <ul className="mt-2 border-y border-line">
              {results.items.map((student) => (
                <li key={student.studentId} className="flex min-h-12 items-center gap-3 border-b border-line py-1 pr-1 pl-3 last:border-b-0">
                  <span data-numeric className="w-24 shrink-0 text-footnote text-ink-tertiary">
                    {student.rollNo}
                  </span>
                  <span className="min-w-0 flex-1 truncate text-callout text-ink">{student.fullName}</span>
                  <span className="hidden shrink-0 text-footnote text-ink-tertiary sm:inline">
                    {student.courseCode} · {student.batchName}
                  </span>
                  {canEdit && (
                    <IconButton
                      label={`Remove ${student.fullName}`}
                      icon={X}
                      disabled={busy}
                      onClick={() =>
                        change(async () => {
                          await removeEnrolment(exam.id, student.studentId)
                          return `${student.fullName} removed`
                        })
                      }
                    />
                  )}
                </li>
              ))}
            </ul>
          )}
          {results && <Pagination page={results.page} totalPages={results.totalPages} totalItems={results.totalItems} onChange={setPage} />}
        </section>
      </div>
    </Dialog>
  )
}
