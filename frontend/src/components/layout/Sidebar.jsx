import { PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { NavLink, useLocation } from 'react-router'
import { useMediaQuery } from '../../hooks/useMediaQuery.js'
import { activeNavIndex, visibleNavigation } from '../../routes/navigation.js'
import { useAuthStore } from '../../store/authStore.js'
import { usePreferencesStore } from '../../store/preferencesStore.js'
import IconButton from '../common/IconButton.jsx'
import AccountMenu from './AccountMenu.jsx'
import Brand from './Brand.jsx'

// A rail on tablets. On laptops and up, a sidebar that collapses to the rail with its icons held still.
export default function Sidebar() {
  const collapsed = usePreferencesStore((state) => state.sidebarCollapsed)
  const toggle = usePreferencesStore((state) => state.toggleSidebar)
  const user = useAuthStore((state) => state.user)
  const isDesktop = useMediaQuery('(min-width: 48rem)')
  const navigation = visibleNavigation(user)
  const { pathname } = useLocation()
  const active = activeNavIndex(navigation, pathname)

  const fade = collapsed ? 'opacity-0' : 'opacity-0 lg:opacity-100'

  return (
    <aside
      className={`sf-move sticky top-0 hidden h-dvh w-(--rail-width) shrink-0 flex-col overflow-hidden md:flex ${collapsed ? '' : 'lg:w-(--sidebar-width)'}`}
    >
      <div className="flex h-(--topbar-height) shrink-0 items-center pl-7 md:mt-2">
        <Brand fadeClassName={fade} />
      </div>

      <nav aria-label="Main" className="relative flex flex-1 flex-col gap-0.5 overflow-x-hidden overflow-y-auto pt-2">
        {active >= 0 && (
          <span
            aria-hidden="true"
            className="sf-move absolute top-2 left-0 flex h-control items-center"
            style={{ transform: `translateY(calc(${active} * (var(--control-height) + 0.125rem)))` }}
          >
            <span className="h-4 w-0.5 bg-tick" />
          </span>
        )}
        {navigation.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            title={label}
            className={({ isActive }) =>
              `relative flex h-control shrink-0 items-center gap-2.5 px-7 text-callout whitespace-nowrap transition-colors duration-(--duration-fast) -outline-offset-2 ${
                isActive ? 'font-medium text-ink' : 'text-ink-secondary hover:text-ink'
              }`
            }
          >
            <Icon size={16} aria-hidden="true" className="shrink-0" />
            <span className={`sf-fade ${fade}`}>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="hidden px-4.5 py-2 lg:block">
        <IconButton
          label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          icon={collapsed ? PanelLeftOpen : PanelLeftClose}
          onClick={toggle}
        />
      </div>
      {isDesktop && (
        <div className="border-t border-line px-4.5 py-2">
          <AccountMenu detailClassName={`sf-fade ${fade}`} />
        </div>
      )}
    </aside>
  )
}
