import { useState } from 'react'
import { toApiError } from '../services/apiError.js'
import { useToastStore } from '../store/toastStore.js'

// Confirm, delete, report: the same three steps for every academic record.
export function useDelete(remove, reload) {
  const toast = useToastStore((state) => state.push)
  const [target, setTarget] = useState(null)
  const [busy, setBusy] = useState(false)

  const confirm = async () => {
    setBusy(true)
    try {
      await remove(target.id)
      toast({ message: `${target.name} was deleted` })
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setBusy(false)
      setTarget(null)
    }
  }

  return { target, ask: setTarget, cancel: () => setTarget(null), confirm, busy }
}
