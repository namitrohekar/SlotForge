import { NavLink } from 'react-router'
import Dialog from '../common/Dialog.jsx'

export default function MoreSheet({ open, onClose, items }) {
  return (
    <Dialog open={open} onClose={onClose} title="More">
      <nav aria-label="More" className="flex flex-col gap-0.5">
        {items.map(({ label, to, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onClose}
            className={({ isActive }) =>
              `sf-press flex min-h-control items-center gap-3 rounded-md px-3 py-2 text-body [--press-scale:0.98] ${
                isActive ? 'bg-sunken font-medium text-ink' : 'text-ink-secondary'
              }`
            }
          >
            <Icon size={20} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>
    </Dialog>
  )
}
