import { useCallback, useState } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ChoiceList from '../../../components/common/ChoiceList.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import Switch from '../../../components/common/Switch.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDay, formatDuration, formatTime } from '../../../utils/format.js'
import { REASONS } from '../reasons.js'
import { listCandidateSlots, moveExam, setExamLock } from '../services/schedulesApi.js'

// Every slot of the session for this paper. A slot the rules refuse stays in the list with its reason, so nothing is a mystery.
function MoveSection({ entry, versionId, onChanged }) {
  const toast = useToastStore((state) => state.push)
  const { data, error } = useApi(useCallback(() => listCandidateSlots(versionId, entry.examinationId), [versionId, entry.examinationId]))
  const [chosen, setChosen] = useState(null)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const run = async (action) => {
    setBusy(true)
    setFailure(null)
    try {
      await action()
    } catch (problem) {
      setFailure(toApiError(problem).message)
      setBusy(false)
    }
  }

  const move = () =>
    run(async () => {
      const result = await moveExam(versionId, entry.examinationId, chosen)
      toast({ message: `${entry.subjectCode} moved. It stays there when you generate again` })
      onChanged(result.run.versionId)
    })

  const keep = (locked) =>
    run(async () => {
      await setExamLock(versionId, entry.examinationId, locked)
      onChanged(versionId)
    })

  return (
    <section aria-labelledby="inspector-move">
      <h3 id="inspector-move" className="mb-2 text-footnote font-medium text-ink">
        Move to another slot
      </h3>
      {failure && (
        <div className="mb-3">
          <Alert tone="danger">{failure}</Alert>
        </div>
      )}
      {error && <Alert tone="danger">{error.message}</Alert>}
      {!data && !error && <Skeleton className="h-24 w-full rounded-md" />}
      {data && (
        <>
          <ChoiceList
            type="radio"
            name="slot"
            isChosen={(slotId) => slotId === chosen}
            onToggle={(slotId) => setChosen(slotId)}
            options={data
              .filter((slot) => !slot.current)
              .map((slot) => ({
                value: slot.slotId,
                label: `${formatDay(slot.date)}, ${formatTime(slot.start)} – ${formatTime(slot.end)}`,
                detail: slot.reason ? `Not possible: ${REASONS[slot.reason] ?? slot.reason}${slot.example ? ` (${slot.example})` : ''}` : 'Keeps every rule',
                disabled: Boolean(slot.reason),
              }))}
          />
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
            <Switch label="Keep it where it is when generating again" checked={entry.locked} disabled={busy} onChange={(event) => keep(event.target.checked)} />
            <Button onClick={move} disabled={chosen === null} loading={busy}>
              Move here
            </Button>
          </div>
        </>
      )}
    </section>
  )
}

const when = (entry) => `${formatDay(entry.date)}, ${formatTime(entry.start)} – ${formatTime(entry.end)}`

/** versionId is set only for a draft this person may still change. */
export default function ExamInspectorDialog({ entry, entries, versionId, onClose, onChanged }) {
  const related = entry.conflicts
    .map((conflict) => ({ ...conflict, paper: entries.find((other) => other.examinationId === conflict.examinationId) }))
    .filter(({ paper }) => paper)
    .sort((a, b) => `${a.paper.date}${a.paper.start}`.localeCompare(`${b.paper.date}${b.paper.start}`))

  const facts = [
    ['When', when(entry)],
    ['Duration', formatDuration(entry.durationMinutes)],
    ['Students', entry.students],
    ['Taught by', entry.departmentCode],
  ]

  return (
    <Dialog
      open
      onClose={onClose}
      title={entry.subjectName}
      description={[entry.subjectCode, entry.heavy && 'heavy paper', entry.locked && 'kept in this slot'].filter(Boolean).join(', ')}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Done
        </Button>
      }
    >
      <div className="space-y-6">
        <dl className="divide-y divide-line">
          {facts.map(([label, value]) => (
            <div key={label} className="grid grid-cols-[7rem_minmax(0,1fr)] gap-3 py-2.5 text-callout">
              <dt className="text-ink-tertiary">{label}</dt>
              <dd data-numeric className="min-w-0 text-ink">
                {value}
              </dd>
            </div>
          ))}
        </dl>

        <section aria-labelledby="inspector-halls">
          <h3 id="inspector-halls" className="mb-2 text-footnote font-medium text-ink">
            {entry.halls.length === 1 ? 'Hall' : `Split across ${entry.halls.length} halls`}
          </h3>
          <ul className="border-y border-line">
            {entry.halls.map((hall) => (
              <li key={hall.code} className="flex items-center justify-between gap-4 border-b border-line px-3 py-2.5 last:border-b-0">
                <span className="min-w-0">
                  <span className="block text-callout text-ink">{hall.name}</span>
                  <span className="block text-footnote text-ink-tertiary">{hall.building}</span>
                  <span className="block text-footnote text-ink-secondary">
                    {hall.invigilators.length ? `Invigilators: ${hall.invigilators.join(', ')}` : 'No invigilator assigned'}
                  </span>
                </span>
                <span data-numeric className="shrink-0 text-footnote text-ink-secondary">
                  {hall.seats} of {hall.capacity} seats
                </span>
              </li>
            ))}
          </ul>
        </section>

        {versionId && <MoveSection entry={entry} versionId={versionId} onChanged={onChanged} />}

        {related.length > 0 && (
          <section aria-labelledby="inspector-shared">
            <h3 id="inspector-shared" className="mb-2 text-footnote font-medium text-ink">
              Shares students with
            </h3>
            <ul className="border-y border-line">
              {related.map(({ paper, sharedStudents }) => (
                <li key={paper.examinationId} className="border-b border-line px-3 py-2.5 last:border-b-0">
                  <span className="block text-callout text-ink">{paper.subjectName}</span>
                  <span data-numeric className="block text-footnote text-ink-tertiary">
                    {when(paper)} · {sharedStudents} shared
                  </span>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </Dialog>
  )
}
