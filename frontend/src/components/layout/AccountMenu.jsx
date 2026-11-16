import { useCallback } from 'react'
import { Bell, CircleUserRound, LogOut, Monitor, Moon, Sun } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router'
import { fetchNotifications } from '../../features/notifications/services/notificationsApi.js'
import { useApi } from '../../hooks/useApi.js'
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
  const navigate = useNavigate()
  const { pathname } = useLocation()
  // Asked again on every page, so a notice shows up as soon as the person moves, without a timer running.
  // eslint-disable-next-line react-hooks/exhaustive-deps -- pathname is the trigger, not an input
  const notices = useApi(useCallback(() => fetchNotifications(), [pathname]))
  const unread = notices.data?.unread ?? 0

  if (!user) return null

  const items = [
    { label: unread ? `Notifications, ${unread} new` : 'Notifications', icon: Bell, onSelect: () => navigate('/notifications') },
    ...themes.map(({ value, label, icon }) => ({ label: theme === value ? `${label} (current)` : label, icon, onSelect: () => setTheme(value) })),
    { label: 'Sign out', icon: LogOut, onSelect: signOut },
  ]

  return (
    <div className="flex min-w-0 items-center gap-2">
      <span className="relative">
        <Dropdown label={unread ? `Account, ${unread} new ${unread === 1 ? 'notification' : 'notifications'}` : 'Account'} icon={CircleUserRound} items={items} />
        {unread > 0 && <span aria-hidden="true" className="pointer-events-none absolute top-1.5 right-1.5 size-2 rounded-full bg-tick ring-2 ring-canvas" />}
      </span>
      <p className={`min-w-0 leading-tight whitespace-nowrap ${detailClassName}`}>
        <span className="block truncate text-footnote text-ink">{user.fullName}</span>
        <span className="block truncate text-caption text-ink-tertiary">{roleSummary(user.roles)}</span>
      </p>
    </div>
  )
}
