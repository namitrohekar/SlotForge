import { useState } from 'react'
import { Ellipsis } from 'lucide-react'
import { NavLink, useLocation } from 'react-router'
import { activeNavIndex, TAB_BAR_LIMIT, visibleNavigation } from '../../routes/navigation.js'
import { useAuthStore } from '../../store/authStore.js'
import MoreSheet from './MoreSheet.jsx'

const tabStyles =
  'sf-press flex min-h-(--tabbar-height) flex-1 flex-col items-center justify-center gap-0.5 text-caption [--press-scale:0.94]'

// Phones get a bottom tab bar, within reach of the thumb. Anything beyond four items goes in More.
export default function TabBar() {
  const [moreOpen, setMoreOpen] = useState(false)
  const user = useAuthStore((state) => state.user)
  const navigation = visibleNavigation(user)
  const { pathname } = useLocation()
  const needsMore = navigation.length > TAB_BAR_LIMIT
  const tabs = needsMore ? navigation.slice(0, TAB_BAR_LIMIT - 1) : navigation
  const slots = tabs.length + (needsMore ? 1 : 0)
  const active = activeNavIndex(tabs, pathname)

  return (
    <>
      <nav
        aria-label="Main"
        className="sf-material fixed inset-x-0 bottom-0 z-30 flex border-t border-line pb-[env(safe-area-inset-bottom)] md:hidden"
      >
        {active >= 0 && (
          <span
            aria-hidden="true"
            className="sf-move absolute top-0 left-0 h-0.5 bg-accent"
            style={{ width: `${100 / slots}%`, transform: `translateX(${active * 100}%)` }}
          />
        )}
        {tabs.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) => `${tabStyles} ${isActive ? 'font-medium text-ink' : 'text-ink-secondary'}`}
          >
            <Icon size={22} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
        {needsMore && (
          <button type="button" onClick={() => setMoreOpen(true)} className={`${tabStyles} text-ink-secondary`}>
            <Ellipsis size={22} aria-hidden="true" />
            More
          </button>
        )}
      </nav>
      {needsMore && (
        <MoreSheet open={moreOpen} onClose={() => setMoreOpen(false)} items={navigation.slice(TAB_BAR_LIMIT - 1)} />
      )}
    </>
  )
}
