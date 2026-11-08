import { useCallback, useEffect, useState } from 'react'
import { toApiError } from '../services/apiError.js'

// `load` must be stable: a module-level function, or one wrapped in useCallback.
export function useApi(load) {
  const [state, setState] = useState({ data: null, error: null, loading: true })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false

    load()
      .then((data) => {
        if (!cancelled) setState({ data, error: null, loading: false })
      })
      .catch((error) => {
        if (!cancelled) setState({ data: null, error: toApiError(error), loading: false })
      })

    return () => {
      cancelled = true
    }
  }, [load, attempt])

  const reload = useCallback(() => {
    setState((current) => ({ ...current, error: null, loading: true }))
    setAttempt((current) => current + 1)
  }, [])

  return { ...state, reload }
}
