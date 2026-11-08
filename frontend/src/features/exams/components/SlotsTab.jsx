import { useCallback, useState } from 'react'
import { Ban, CalendarClock, CircleCheck, Plus, Trash2 } from 'lucide-react'
import { useOutletContext } from 'react-router'
import Alert from '../../../components/common/Alert.jsx'
import Badge from '../../../components/common/Badge.jsx'
import Button from '../../../components/common/Button.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useDelete } from '../../../hooks/useDelete.js'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDay, formatDuration, formatTime } from '../../../utils/format.js'
import { changeSlotStatus, deleteSlot, listSlots } from '../services/examsApi.js'
import BlockSlotDialog from './BlockSlotDialog.jsx'
import GenerateSlotsDialog from './GenerateSlotsDialog.jsx'

const toMinutes = (time) => {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}
const minutesOf = (slot) => toMinutes(slot.end) - toMinutes(slot.start)
const timeRange = (slot) => `${formatTime(slot.start)} – ${formatTime(slot.end)}`

function groupByDay(slots) {
  const days = new Map()
  slots.forEach((slot) => days.set(slot.date, [...(days.get(slot.date) ?? []), slot]))
  return [...days.entries()]
}

export default function SlotsTab() {
  const { session, reloadSession, canEdit } = useOutletContext()
  const toast = useToastStore((state) => state.push)
  const { data: slots, error, reload } = useApi(useCallback(() => listSlots(session.id), [session.id]))
  const [generating, setGenerating] = useState(false)
  const [blocking, setBlocking] = useState(null)

  const refresh = () => {
    reload()
    reloadSession()
  }
  const deletion = useDelete(deleteSlot, refresh)

  const open = async (slot) => {
    try {
      await changeSlotStatus(slot.id, { status: 'OPEN' })
      toast({ message: 'Slot opened' })
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    }
  }

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!slots) return <Skeleton className="h-64 w-full rounded-lg" />

  const days = groupByDay(slots)
  const { slotsTooShort: tooShort, shortestPaperMinutes: shortest } = session.progress
  const generate = (
    <Button icon={Plus} onClick={() => setGenerating(true)}>
      Generate slots
    </Button>
  )

  return (
    <>
      {slots.length === 0 ? (
        <EmptyState
          icon={CalendarClock}
          title="No time slots yet"
          description="Slots are the periods papers can be placed in."
          action={canEdit ? generate : undefined}
        />
      ) : (
        <>
          <div className="mb-4 flex items-center justify-between gap-4">
            <p data-numeric className="text-footnote text-ink-secondary">
              {slots.length} {slots.length === 1 ? 'slot' : 'slots'} over {days.length} {days.length === 1 ? 'day' : 'days'}
            </p>
            {canEdit && generate}
          </div>
          {tooShort > 0 && (
            <div className="mb-4">
              <Alert tone="warning">
                {tooShort === 1 ? '1 open slot is' : `${tooShort} open slots are`} shorter than the shortest paper ({formatDuration(shortest)}), so nothing can be placed there.
              </Alert>
            </div>
          )}
          <ol className="border-t border-line">
            {days.map(([date, daySlots]) => (
              <li key={date} className="grid gap-x-8 border-b border-line py-2 sm:grid-cols-[9rem_minmax(0,1fr)]">
                <p className="py-2 text-callout font-medium text-ink">{formatDay(date)}</p>
                <ul>
                  {daySlots.map((slot) => (
                    <li key={slot.id} className="flex min-h-control items-center gap-3">
                      <span data-numeric className={`text-callout ${slot.status === 'BLOCKED' ? 'text-ink-tertiary' : 'text-ink'}`}>
                        {timeRange(slot)}
                      </span>
                      {slot.status === 'OPEN' && shortest && minutesOf(slot) < shortest && <Badge tone="warning">Too short</Badge>}
                      {slot.status === 'BLOCKED' && (
                        <>
                          <StatusBadge status="BLOCKED" />
                          <span className="min-w-0 truncate text-footnote text-ink-tertiary">{slot.blockReason}</span>
                        </>
                      )}
                      {canEdit && (
                        <div className="ml-auto">
                          <Dropdown
                            label={`Actions for ${formatDay(date)}, ${timeRange(slot)}`}
                            items={[
                              slot.status === 'OPEN'
                                ? { label: 'Block', icon: Ban, onSelect: () => setBlocking(slot) }
                                : { label: 'Open', icon: CircleCheck, onSelect: () => open(slot) },
                              {
                                label: 'Delete',
                                icon: Trash2,
                                danger: true,
                                disabled: slot.usageCount > 0,
                                onSelect: () => deletion.ask({ id: slot.id, name: `${formatDay(date)}, ${timeRange(slot)}` }),
                              },
                            ]}
                          />
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        </>
      )}

      {generating && (
        <GenerateSlotsDialog
          session={session}
          existing={slots}
          onClose={() => setGenerating(false)}
          onGenerated={({ created, skipped }) => {
            setGenerating(false)
            toast({ message: skipped ? `${created} added, ${skipped} already covered` : `${created} slots added` })
            refresh()
          }}
        />
      )}

      {blocking && (
        <BlockSlotDialog
          slot={blocking}
          onClose={() => setBlocking(null)}
          onBlocked={() => {
            setBlocking(null)
            toast({ message: 'Slot blocked' })
            reload()
          }}
        />
      )}

      <ConfirmDialog
        open={Boolean(deletion.target)}
        title={`Delete ${deletion.target?.name ?? ''}?`}
        message="This cannot be undone."
        confirmLabel="Delete"
        loading={deletion.busy}
        onConfirm={deletion.confirm}
        onClose={deletion.cancel}
      />
    </>
  )
}
