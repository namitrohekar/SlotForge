import { useState } from 'react'
import { Ban, CircleCheck, FolderOpen, PencilLine, Trash2 } from 'lucide-react'
import { useNavigate } from 'react-router'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { changeSessionStatus, deleteSession } from '../services/examsApi.js'
import SessionFormDialog from './SessionFormDialog.jsx'

const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`

// Everything that can happen to a session as a whole, in one place on the list and on its page.
export default function SessionActionsMenu({ session, showOpen = false, onChanged, onDeleted }) {
  const navigate = useNavigate()
  const toast = useToastStore((state) => state.push)
  const [editing, setEditing] = useState(false)
  const [confirming, setConfirming] = useState(null)
  const [busy, setBusy] = useState(false)

  const run = async (action, message, after) => {
    setBusy(true)
    try {
      await action()
      toast({ message })
      setConfirming(null)
      after()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
      setConfirming(null)
    } finally {
      setBusy(false)
    }
  }

  const planning = session.status === 'PLANNING'
  const items = [
    showOpen && { label: 'Open session', icon: FolderOpen, onSelect: () => navigate(`/sessions/${session.id}`) },
    planning && { label: 'Edit details', icon: PencilLine, onSelect: () => setEditing(true) },
    planning && { label: 'Cancel session', icon: Ban, onSelect: () => setConfirming('cancel') },
    ['CANCELLED', 'PUBLISHED'].includes(session.status) && { label: 'Reopen session', icon: CircleCheck, onSelect: () => setConfirming('reopen') },
    planning && { label: 'Delete session', icon: Trash2, danger: true, onSelect: () => setConfirming('delete') },
  ].filter(Boolean)

  const confirmations = {
    cancel: {
      title: `Cancel ${session.name}?`,
      message: 'Its slots, papers and timetables are kept for the record, but nothing can change until it is reopened.',
      confirmLabel: 'Cancel session',
      variant: 'danger',
      onConfirm: () => run(() => changeSessionStatus(session.id, 'CANCELLED'), 'Session cancelled', onChanged),
    },
    reopen: {
      title: `Reopen ${session.name}?`,
      message:
        session.status === 'PUBLISHED'
          ? 'It returns to planning. The published timetable stays official until a new one is approved and published.'
          : 'It returns to planning, and its slots, papers and timetables can be changed again.',
      confirmLabel: 'Reopen session',
      variant: 'primary',
      onConfirm: () => run(() => changeSessionStatus(session.id, 'PLANNING'), 'Session reopened', onChanged),
    },
    delete: {
      title: `Delete ${session.name}?`,
      message: `This permanently removes ${plural(session.slotCount, 'time slot')}, ${plural(session.examinationCount, 'paper')} with their enrolment, and every draft timetable. It cannot be undone. To keep the record, cancel the session instead.`,
      confirmLabel: 'Delete everything',
      variant: 'danger',
      onConfirm: () => run(() => deleteSession(session.id), 'Session deleted', onDeleted),
    },
  }
  const confirmation = confirmations[confirming]

  return (
    <>
      <Dropdown label={`Actions for ${session.name}`} items={items} />
      {editing && (
        <SessionFormDialog
          session={session}
          onClose={() => setEditing(false)}
          onSaved={(message) => {
            setEditing(false)
            toast({ message })
            onChanged()
          }}
        />
      )}
      {confirmation && <ConfirmDialog open loading={busy} onClose={() => setConfirming(null)} {...confirmation} />}
    </>
  )
}
