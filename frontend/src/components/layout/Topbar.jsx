import { CircleUserRound, LogOut } from 'lucide-react'
import { useAuthStore } from '../../store/authStore.js'
import { roleSummary } from '../../utils/roles.js'
import Dropdown from '../common/Dropdown.jsx'
import ThemeToggle from '../common/ThemeToggle.jsx'
import Brand from './Brand.jsx'

export default function Topbar() {
  const user = useAuthStore((state) => state.user)
  const signOut = useAuthStore((state) => state.signOut)

  return (
    <header className="sf-material sticky top-0 z-20 flex h-(--topbar-height) items-center justify-between gap-4 border-b border-line px-4 md:justify-end md:rounded-t-xl md:px-8 lg:px-12">
      <div className="md:hidden">
        <Brand />
      </div>
      <div className="flex items-center gap-3">
        <ThemeToggle />
        {user && (
          <>
            <p className="hidden text-right leading-tight lg:block">
              <span className="block text-footnote font-medium text-ink">{user.fullName}</span>
              <span className="block text-caption text-ink-tertiary">{roleSummary(user.roles)}</span>
            </p>
            <Dropdown
              label="Account"
              icon={CircleUserRound}
              items={[{ label: 'Sign out', icon: LogOut, onSelect: signOut }]}
            />
          </>
        )}
      </div>
    </header>
  )
}
