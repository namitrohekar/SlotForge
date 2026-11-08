import { useCallback, useSyncExternalStore } from 'react'

// Only for cases where a different component must mount. Layout changes belong in CSS.
export function useMediaQuery(query) {
  const subscribe = useCallback(
    (notify) => {
      const media = window.matchMedia(query)
      media.addEventListener('change', notify)
      return () => media.removeEventListener('change', notify)
    },
    [query],
  )

  return useSyncExternalStore(subscribe, () => window.matchMedia(query).matches)
}
