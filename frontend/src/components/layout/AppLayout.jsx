import { Outlet, useLocation } from 'react-router'
import Sidebar from './Sidebar.jsx'
import SkipLink from './SkipLink.jsx'
import TabBar from './TabBar.jsx'
import Topbar from './Topbar.jsx'

// Quiet chrome on the canvas. A hairline separates it from one continuous page.
export default function AppLayout() {
  const { pathname } = useLocation()

  return (
    <div className="min-h-dvh md:flex">
      <SkipLink />
      <Sidebar />
      <div className="flex min-h-dvh min-w-0 flex-1 flex-col bg-surface md:border-l md:border-line">
        <Topbar />
        <main
          id="main"
          tabIndex={-1}
          className="flex-1 px-4 pt-8 pb-[calc(var(--tabbar-height)+2rem+env(safe-area-inset-bottom))] md:px-8 md:pt-10 md:pb-12 lg:px-14 lg:pt-12"
        >
          {/* Restart only for a new page. Sections inside a page keep their heading, tabs and focus. */}
          <div key={pathname.match(/^\/[^/]*(\/\d+)?/)[0]} className="sf-page mx-auto max-w-(--content-max)">
            <Outlet />
          </div>
        </main>
      </div>
      <TabBar />
    </div>
  )
}
