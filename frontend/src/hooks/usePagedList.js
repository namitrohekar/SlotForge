import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import { useApi } from './useApi.js'

// List state lives in the URL, so it survives reload and can be shared.
export function usePagedList(load) {
  const [params, setParams] = useSearchParams()
  const query = useMemo(() => Object.fromEntries(params), [params])

  const fetchPage = useCallback(() => load(query), [load, query])
  const result = useApi(fetchPage)

  const update = useCallback(
    (changes) => {
      setParams((current) => {
        const next = new URLSearchParams(current)
        Object.entries(changes).forEach(([key, value]) => {
          if (value === '' || value === null || value === undefined) next.delete(key)
          else next.set(key, value)
        })
        return next
      })
    },
    [setParams],
  )

  const setFilter = useCallback((key, value) => update({ [key]: value, page: null }), [update])
  const setPage = useCallback((page) => update({ page: page || null }), [update])
  const setSort = useCallback((sort) => update({ sort, page: null }), [update])

  return { ...result, query, setFilter, setPage, setSort }
}
