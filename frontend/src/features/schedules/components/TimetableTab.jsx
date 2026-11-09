import { useCallback, useState } from 'react'
import { CalendarRange, RefreshCw, Trash2 } from 'lucide-react'
import { Link, useOutletContext, useSearchParams } from 'react-router'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Select from '../../../components/common/Select.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useDelete } from '../../../hooks/useDelete.js'
import { useMediaQuery } from '../../../hooks/useMediaQuery.js'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDateTime } from '../../../utils/format.js'
import { deleteVersion, generateTimetable, listEntries, listVersions, listViolations } from '../services/schedulesApi.js'
import ApprovalJourney from './ApprovalJourney.jsx'
import ExamInspectorDialog from './ExamInspectorDialog.jsx'
import TimetableAgenda from './TimetableAgenda.jsx'
import TimetableGrid from './TimetableGrid.jsx'
import UnplacedPanel from './UnplacedPanel.jsx'
import WorkflowBar from './WorkflowBar.jsx'

const STATUS_NAMES = { DRAFT: 'Draft', REVIEWED: 'Approved', PUBLISHED: 'Published', SUPERSEDED: 'Superseded' }

const inReview = (version) => version.status === 'REVIEWED' || (version.status === 'DRAFT' && Boolean(version.submittedAt))
const versionState = (version) => (version.status === 'DRAFT' && version.submittedAt ? 'Waiting for approval' : STATUS_NAMES[version.status])

const runSummary = (version) =>
  `${version.slotsUsed} ${version.slotsUsed === 1 ? 'slot' : 'slots'} used · generated ${formatDateTime(version.createdAt)}`

// A paper with no students is not scheduled, and saying "3 of 3 placed" without this would hide that.
function LeftOutNotice({ papers }) {
  if (papers.length === 0) return null
  return (
    <div className="mb-6">
      <Alert tone="warning" title={papers.length === 1 ? '1 paper has no students, so it is not in this timetable' : `${papers.length} papers have no students, so they are not in this timetable`}>
        {papers.join(', ')}.{' '}
        <Link to="../examinations" className="font-medium underline">
          Enrol students
        </Link>
      </Alert>
    </div>
  )
}

function OutOfDateNotice({ problems, published }) {
  if (problems.length === 0) return null
  return (
    <div className="mb-6">
      <Alert
        tone="danger"
        title={published ? 'This published timetable needs attention. Reopen the session, generate again and publish a replacement.' : 'This timetable cannot go forward yet'}
      >
        <ul className="mt-1 space-y-0.5">
          {problems.map((problem) => (
            <li key={problem}>{problem}</li>
          ))}
        </ul>
      </Alert>
    </div>
  )
}

export default function TimetableTab() {
  const { session, reloadSession, canEdit } = useOutletContext()
  const toast = useToastStore((state) => state.push)
  const isPhone = useMediaQuery('(max-width: 47.99rem)')
  const [params, setParams] = useSearchParams()
  const versions = useApi(useCallback(() => listVersions(session.id), [session.id]))
  const current = versions.data?.find((version) => String(version.id) === params.get('version')) ?? versions.data?.find(inReview) ?? versions.data?.[0]
  const currentId = current?.id
  const entries = useApi(useCallback(() => (currentId ? listEntries(currentId) : Promise.resolve([])), [currentId]))
  // An incomplete version already lists its missing papers and cannot be approved. A published one stays live, so it is checked too.
  const checked = current?.status === 'PUBLISHED' || (session.status === 'PLANNING' && ['DRAFT', 'REVIEWED'].includes(current?.status))
  const checkedId = checked && current.examsScheduled === current.examsTotal ? currentId : null
  const violations = useApi(useCallback(() => (checkedId ? listViolations(checkedId) : Promise.resolve([])), [checkedId]))
  const problems = violations.data ?? []
  const [generating, setGenerating] = useState(false)
  const [nothingPlaced, setNothingPlaced] = useState(null)
  const [inspected, setInspected] = useState(null)

  const deletion = useDelete(deleteVersion, () => {
    setParams({}, { replace: true })
    versions.reload()
    reloadSession()
  })

  const generate = async () => {
    setGenerating(true)
    setNothingPlaced(null)
    try {
      const run = await generateTimetable(session.id)
      if (run.versionId === null) {
        setNothingPlaced(run.unplaced)
        return
      }
      toast({
        tone: run.unplaced.length ? 'warning' : 'success',
        message: run.unplaced.length
          ? `${run.examsScheduled} of ${run.examsTotal} papers placed`
          : `All ${run.examsTotal} papers placed, with no clashes`,
      })
      setParams({ version: run.versionId }, { replace: true })
      versions.reload()
      reloadSession()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setGenerating(false)
    }
  }

  if (versions.error) return <ErrorState message={versions.error.message} onRetry={versions.reload} />
  if (!versions.data) return <Skeleton className="h-96 w-full rounded-lg" />

  const leftOut = session.progress.papersWithoutStudents

  if (!current) {
    const missing = session.slotCount === 0 ? 'slots' : session.examinationCount === 0 ? 'examinations' : null
    return (
      <>
        <LeftOutNotice papers={leftOut} />
        {nothingPlaced && <UnplacedPanel unplaced={nothingPlaced} />}
        <EmptyState
          icon={CalendarRange}
          title="No timetable yet"
          description={
            missing === 'slots'
              ? 'This session needs time slots first'
              : missing === 'examinations'
                ? 'This session needs papers first'
                : 'Every paper is placed so that no student has two at once, then seated in halls'
          }
          action={
            missing ? (
              <Link to={`../${missing}`} className="text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
                {missing === 'slots' ? 'Go to time slots' : 'Go to papers'}
              </Link>
            ) : (
              canEdit && (
                <Button onClick={generate} loading={generating}>
                  Generate timetable
                </Button>
              )
            )
          }
        />
      </>
    )
  }

  return (
    <>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <div className="min-w-0">
          <h2 data-numeric className="text-title-2">
            {current.examsScheduled} of {current.examsTotal} ready papers placed
          </h2>
          <p data-numeric className="mt-1 text-footnote text-ink-tertiary">
            {runSummary(current)}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="w-60 max-w-full">
            <Select
              aria-label="Timetable version"
              value={current.id}
              onChange={(event) => setParams({ version: event.target.value }, { replace: true })}
            >
              {versions.data.map((version) => (
                <option key={version.id} value={version.id}>
                  Version {version.versionNo} · {versionState(version)}
                </option>
              ))}
            </Select>
          </div>
          {canEdit && current.status === 'DRAFT' && !current.submittedAt && (
            <IconButton
              label={`Delete version ${current.versionNo}`}
              icon={Trash2}
              onClick={() => deletion.ask({ id: current.id, name: `Version ${current.versionNo}` })}
            />
          )}
          {canEdit && (
            <Button variant="secondary" icon={RefreshCw} onClick={generate} loading={generating}>
              Generate again
            </Button>
          )}
        </div>
      </header>

      <ApprovalJourney version={current} progress={session.progress} />

      <WorkflowBar
        version={current}
        planning={session.status === 'PLANNING'}
        progress={session.progress}
        outOfDate={problems.length > 0}
        onChanged={() => {
          versions.reload()
          violations.reload()
          reloadSession()
        }}
      />

      <OutOfDateNotice problems={problems} published={current.status === 'PUBLISHED'} />
      <LeftOutNotice papers={leftOut} />
      {nothingPlaced && <UnplacedPanel unplaced={nothingPlaced} />}
      <UnplacedPanel unplaced={current.unplaced} />

      {entries.error && <ErrorState message={entries.error.message} onRetry={entries.reload} />}
      {!entries.data && !entries.error && <Skeleton className="h-96 w-full rounded-lg" />}
      {entries.data &&
        (isPhone ? (
          <TimetableAgenda entries={entries.data} onOpen={setInspected} />
        ) : (
          <TimetableGrid entries={entries.data} onOpen={setInspected} />
        ))}

      {inspected && <ExamInspectorDialog entry={inspected} entries={entries.data} onClose={() => setInspected(null)} />}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title={`Delete ${deletion.target?.name ?? ''}?`}
        message="Its timetable and hall placements go with it. This cannot be undone."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
