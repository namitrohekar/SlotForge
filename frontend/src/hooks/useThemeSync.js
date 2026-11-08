import { useEffect } from 'react'
import { usePreferencesStore } from '../store/preferencesStore.js'

export function resolveTheme(preference, systemPrefersDark) {
  if (preference === 'system') return systemPrefersDark ? 'dark' : 'light'
  return preference
}

// One crossfade of the whole page, instead of animating every element's colours.
function applyTheme(theme) {
  const root = document.documentElement
  if (root.dataset.theme === theme) return

  const change = () => {
    root.dataset.theme = theme
  }
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (document.startViewTransition && !reduceMotion) document.startViewTransition(change)
  else change()
}

export function useThemeSync() {
  const preference = usePreferencesStore((state) => state.theme)

  useEffect(() => {
    const systemDark = window.matchMedia('(prefers-color-scheme: dark)')
    const sync = () => applyTheme(resolveTheme(preference, systemDark.matches))

    sync()
    systemDark.addEventListener('change', sync)
    return () => systemDark.removeEventListener('change', sync)
  }, [preference])
}
