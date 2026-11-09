import { useMediaQuery } from '../../hooks/useMediaQuery.js'
import AccountMenu from './AccountMenu.jsx'
import Brand from './Brand.jsx'

// Phones only. From tablet width up, the sidebar carries the brand and the account.
export default function Topbar() {
  const isDesktop = useMediaQuery('(min-width: 48rem)')
  if (isDesktop) return null

  return (
    <header className="sf-bar sticky top-0 z-20 flex h-(--topbar-height) items-center justify-between gap-4 border-b border-line px-4">
      <Brand />
      <AccountMenu detailClassName="sr-only" />
    </header>
  )
}
