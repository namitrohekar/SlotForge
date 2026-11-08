import { useCallback, useState } from 'react'
import { ChevronLeft, Plus, X } from 'lucide-react'
import { Link, useParams } from 'react-router'
import Badge from '../../../components/common/Badge.jsx'
import HeavyBadge from '../../../components/common/HeavyBadge.jsx'
import Button from '../../../components/common/Button.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { hasAnyRole } from '../../../utils/roles.js'
import PlacementDialog from '../components/PlacementDialog.jsx'
import { getCurriculum, listCurriculumSubjects, replaceCurriculumSubjects } from '../services/academicsApi.js'

const placement = ({ subjectId, semesterNo, elective }) => ({ subjectId, semesterNo, elective })

function Semester({ number, items, canEdit, saving, onAdd, onRemove }) {
  const headingId = `semester-${number}`
  return (
    <section aria-labelledby={headingId} className="rounded-lg border border-line">
      <header className="flex min-h-13 items-center justify-between gap-3 border-b border-line py-2 pr-2 pl-4">
        <h3 id={headingId} className="text-title-3">
          Semester {number}
          <span data-numeric className="ml-2 text-footnote font-normal text-ink-tertiary">
            {items.length} {items.length === 1 ? 'subject' : 'subjects'}
          </span>
        </h3>
        {canEdit && (
          <Button variant="quiet" icon={Plus} onClick={onAdd} disabled={saving}>
            Add
          </Button>
        )}
      </header>
      {items.length === 0 ? (
        <p className="px-4 py-5 text-callout text-ink-tertiary">No subjects yet.</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item.subjectId} className="flex min-h-12 items-center gap-3 border-b border-line py-1.5 pr-2 pl-4 last:border-b-0">
              <span data-numeric className="w-24 shrink-0 text-footnote text-ink-tertiary">
                {item.code}
              </span>
              <span className="min-w-0 flex-1 text-callout text-ink">{item.name}</span>
              {item.elective && <Badge tone="info">Elective</Badge>}
              {item.heavy && <HeavyBadge />}
              {canEdit && (
                <IconButton
                  label={`Remove ${item.name} from semester ${number}`}
                  icon={X}
                  disabled={saving}
                  onClick={() => onRemove(item)}
                />
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

// The syllabus laid out the way students read it: semester by semester.
export default function CurriculumDetailPage() {
  const { id } = useParams()
  const canEdit = useAuthStore((state) => hasAnyRole(state.user, ['EXAM_COORDINATOR']))
  const toast = useToastStore((state) => state.push)
  const curriculum = useApi(useCallback(() => getCurriculum(id), [id]))
  const placed = useApi(useCallback(() => listCurriculumSubjects(id), [id]))
  const [addingTo, setAddingTo] = useState(null)
  const [saving, setSaving] = useState(false)

  const save = async (next, message) => {
    setSaving(true)
    try {
      await replaceCurriculumSubjects(id, next.map(placement))
      toast({ message })
      placed.reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setSaving(false)
    }
  }

  if (curriculum.error) return <ErrorState message={curriculum.error.message} onRetry={curriculum.reload} />
  if (!curriculum.data || !placed.data) return <Skeleton className="h-96 w-full rounded-lg" />

  const { name, courseName, courseCode, effectiveFromYear, totalSemesters } = curriculum.data
  const semesters = Array.from({ length: totalSemesters }, (_, index) => index + 1)

  return (
    <>
      <Link
        to="/academics/curricula"
        className="sf-press inline-flex items-center gap-1 rounded-sm text-footnote text-ink-secondary hover:text-ink"
      >
        <ChevronLeft size={16} aria-hidden="true" />
        Curricula
      </Link>
      <header className="mt-3 mb-8">
        <h2 className="font-serif text-title-1 font-medium">{name}</h2>
        <p className="mt-1 text-callout text-ink-secondary">
          {courseName} · {courseCode} · in force from <span data-numeric>{effectiveFromYear}</span>
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-2">
        {semesters.map((number) => (
          <Semester
            key={number}
            number={number}
            items={placed.data.filter((item) => item.semesterNo === number)}
            canEdit={canEdit}
            saving={saving}
            onAdd={() => setAddingTo(number)}
            onRemove={(item) =>
              save(
                placed.data.filter((other) => other.subjectId !== item.subjectId),
                `${item.code} removed from semester ${number}`,
              )
            }
          />
        ))}
      </div>

      {addingTo && (
        <PlacementDialog
          semester={addingTo}
          placedIds={placed.data.map((item) => item.subjectId)}
          onClose={() => setAddingTo(null)}
          onAdd={(subject, elective) => {
            const semester = addingTo
            setAddingTo(null)
            save(
              [...placed.data, { subjectId: subject.id, semesterNo: semester, elective }],
              `${subject.code} added to semester ${semester}`,
            )
          }}
        />
      )}
    </>
  )
}
