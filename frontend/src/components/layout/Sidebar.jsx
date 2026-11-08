import { PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { NavLink, useLocation } from 'react-router'
import { activeNavIndex, visibleNavigation } from '../../routes/navigation.js'
import { useAuthStore } from '../../store/authStore.js'
import { usePreferencesStore } from '../../store/preferencesStore.js'
import IconButton from '../common/IconButton.jsx'
import Brand from './Brand.jsx'

// A rail on tablets. On laptops and up, a sidebar that collapses to the rail with its icons held still.
export default function Sidebar() {
  const collapsed = usePreferencesStore((state) => state.sidebarCollapsed)
  const toggle = usePreferencesStore((state) => state.toggleSidebar)
  const user = useAuthStore((state) => state.user)
  const navigation = visibleNavigation(user)
  const { pathname } = useLocation()
  const active = activeNavIndex(navigation, pathname)

  const fade = collapsed ? 'opacity-0' : 'opacity-0 lg:opacity-100'

  return (
    <aside
      className={`sf-move sticky top-0 hidden h-dvh w-(--rail-width) shrink-0 flex-col overflow-hidden md:flex ${collapsed ? '' : 'lg:w-(--sidebar-width)'}`}
    >
      <div className="flex h-(--topbar-height) shrink-0 items-center pl-6.75 md:mt-2">
        <Brand fadeClassName={fade} />
      </div>

      <nav aria-label="Main" className="relative flex flex-1 flex-col gap-0.5 overflow-x-hidden overflow-y-auto px-3 pt-2">
        {active >= 0 && (
          <span
            aria-hidden="true"
            className="sf-move absolute inset-x-3 top-2 h-control rounded-md bg-surface shadow-1"
            style={{ transform: `translateY(calc(${active} * (var(--control-height) + 0.125rem)))` }}
          />
        )}
        {navigation.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            title={label}
            className={({ isActive }) =>
              `sf-press relative flex h-control shrink-0 items-center gap-3 rounded-md px-3.75 text-callout whitespace-nowrap [--press-scale:0.98] ${
                isActive ? 'font-medium text-ink' : 'text-ink-secondary hover:text-ink'
              }`
            }
          >
            <Icon size={18} aria-hidden="true" className="shrink-0" />
            <span className={`sf-fade ${fade}`}>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="hidden px-4.5 py-3 lg:block">
        <IconButton
          label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          icon={collapsed ? PanelLeftOpen : PanelLeftClose}
          onClick={toggle}
        />
      </div>
    </aside>
  )
}
