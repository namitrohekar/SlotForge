import { NavLink } from 'react-router'

// A session is one job in three stages, so its sections read as steps with their progress, not as loose tabs.
// The brass tick marks the page you are on, or on the dashboard the step that is up next.
export default function SessionSteps({ steps, base = '' }) {
  const upNext = steps.find((step) => step.state !== 'done')

  return (
    <nav aria-label="Session steps">
      <ol className="mb-6 grid auto-cols-fr grid-flow-col border-y border-line">
        {steps.map((step, index) => (
          <li key={step.to} className="min-w-0">
            <NavLink
              to={base ? `${base}/${step.to}` : step.to}
              className={({ isActive }) =>
                `group block py-3 -outline-offset-2 ${index ? 'border-l border-line px-2 sm:px-4' : 'pr-2 sm:pr-4'} ${
                  (base ? step === upNext : isActive) ? 'shadow-[inset_0_-2px_0_var(--sf-tick)]' : ''
                }`
              }
            >
              <span className="block text-callout font-medium text-ink decoration-line-strong underline-offset-2 group-hover:underline">{step.label}</span>
              <span className={`block text-footnote sm:truncate ${step.state === 'attention' ? 'text-warning' : 'text-ink-tertiary'}`}>{step.detail}</span>
            </NavLink>
          </li>
        ))}
      </ol>
    </nav>
  )
}
