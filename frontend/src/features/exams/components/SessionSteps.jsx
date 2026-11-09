import { Check } from 'lucide-react'
import { NavLink } from 'react-router'
import { STEP_MARKS } from '../sessionSteps.js'

// A session is one job in three stages, so its sections read as steps with their progress, not as loose tabs.
export default function SessionSteps({ steps, base = '' }) {
  return (
    <nav aria-label="Session steps">
      <ol className="mb-6 grid auto-cols-fr grid-flow-col gap-1 border-b border-line">
        {steps.map((step, index) => (
          <li key={step.to} className="min-w-0">
            <NavLink
              to={base ? `${base}/${step.to}` : step.to}
              className={({ isActive }) =>
                `sf-press flex min-h-14 items-center gap-2.5 px-2 py-2 [--press-scale:0.98] sm:px-3 ${
                  isActive && !base ? 'shadow-[inset_0_-2px_0_var(--sf-accent)]' : ''
                }`
              }
            >
              <span aria-hidden="true" data-numeric className={`grid size-6 shrink-0 place-items-center rounded-full text-caption font-medium ${STEP_MARKS[step.state]}`}>
                {step.state === 'done' ? <Check size={14} /> : index + 1}
              </span>
              <span className="min-w-0">
                <span className="block text-callout font-medium text-ink">{step.label}</span>
                <span className="block truncate text-footnote text-ink-tertiary">{step.detail}</span>
              </span>
            </NavLink>
          </li>
        ))}
      </ol>
    </nav>
  )
}
