import { Monitor, Moon, Sun } from 'lucide-react'
import { usePreferencesStore } from '../../store/preferencesStore.js'
import SegmentedControl from './SegmentedControl.jsx'

const options = [
  { value: 'system', label: 'System theme', icon: Monitor },
  { value: 'light', label: 'Light theme', icon: Sun },
  { value: 'dark', label: 'Dark theme', icon: Moon },
]

export default function ThemeToggle() {
  const theme = usePreferencesStore((state) => state.theme)
  const setTheme = usePreferencesStore((state) => state.setTheme)

  return <SegmentedControl label="Theme" options={options} value={theme} onChange={setTheme} iconOnly />
}
