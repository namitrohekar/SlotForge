import { NavLink } from 'react-router'

// Sections of one page, each at its own address, so its filters survive a reload and can be shared.
export default function SectionNav({ label, sections }) {
  return (
    <nav aria-label={label} className="mb-6 flex gap-1 overflow-x-auto border-b border-line">
      {sections.map(({ to, label: name }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            `sf-press flex min-h-control items-center px-3 text-callout whitespace-nowrap [--press-scale:0.98] ${
              isActive ? 'font-medium text-ink shadow-[inset_0_-2px_0_var(--sf-tick)]' : 'text-ink-secondary hover:text-ink'
            }`
          }
        >
          {name}
        </NavLink>
      ))}
    </nav>
  )
}
