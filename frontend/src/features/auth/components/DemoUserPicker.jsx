import { ChevronRight } from 'lucide-react'
import Spinner from '../../../components/common/Spinner.jsx'
import { roleSummary } from '../../../utils/roles.js'

// One row per seeded account. Choosing a row signs in as that person.
export default function DemoUserPicker({ users, pendingId, onPick }) {
  return (
    <ul aria-label="Demo accounts" className="overflow-hidden rounded-lg border border-line bg-surface">
      {users.map((user) => (
        <li key={user.id} className="border-b border-line last:border-b-0">
          <button
            type="button"
            onClick={() => onPick(user.id)}
            disabled={pendingId != null}
            aria-busy={pendingId === user.id || undefined}
            className="sf-press flex min-h-15 w-full items-center gap-3 px-4 py-3 text-left -outline-offset-2 hover:bg-sunken disabled:opacity-60 [--press-scale:0.99]"
          >
            <span className="min-w-0 flex-1">
              <span className="block text-body font-medium text-ink">{roleSummary(user.roles)}</span>
              <span className="block truncate text-footnote text-ink-tertiary">
                {user.fullName} · {user.email}
              </span>
            </span>
            {pendingId === user.id ? (
              <Spinner label="Signing in" size={16} />
            ) : (
              <ChevronRight size={16} aria-hidden="true" className="shrink-0 text-ink-tertiary" />
            )}
          </button>
        </li>
      ))}
    </ul>
  )
}
