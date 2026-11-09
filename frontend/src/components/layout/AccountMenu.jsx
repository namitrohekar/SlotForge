import { CircleUserRound, LogOut, Monitor, Moon, Sun } from 'lucide-react'
import { useAuthStore } from '../../store/authStore.js'
import { usePreferencesStore } from '../../store/preferencesStore.js'
import { roleSummary } from '../../utils/roles.js'
import Dropdown from '../common/Dropdown.jsx'

const themes = [
  { value: 'system', label: 'System theme', icon: Monitor },
  { value: 'light', label: 'Light theme', icon: Sun },
  { value: 'dark', label: 'Dark theme', icon: Moon },
]

// The theme is set rarely, so it lives with the account rather than on every screen.
export default function AccountMenu({ detailClassName = '' }) {
  const user = useAuthStore((state) => state.user)
  const signOut = useAuthStore((state) => state.signOut)
  const theme = usePreferencesStore((state) => state.theme)
  const setTheme = usePreferencesStore((state) => state.setTheme)

  if (!user) return null

  const items = [
    ...themes.map(({ value, label, icon }) => ({ label: theme === value ? `${label} (current)` : label, icon, onSelect: () => setTheme(value) })),
    { label: 'Sign out', icon: LogOut, onSelect: signOut },
  ]

  return (
    <div className="flex min-w-0 items-center gap-2">
      <Dropdown label="Account" icon={CircleUserRound} items={items} />
      <p className={`min-w-0 leading-tight whitespace-nowrap ${detailClassName}`}>
        <span className="block truncate text-footnote text-ink">{user.fullName}</span>
        <span className="block truncate text-caption text-ink-tertiary">{roleSummary(user.roles)}</span>
      </p>
    </div>
  )
}
