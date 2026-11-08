import { Inbox } from 'lucide-react'

export default function EmptyState({ icon: Icon = Inbox, title, description, action }) {
  return (
    <div className="flex flex-col items-center px-6 py-12 text-center">
      <Icon size={28} aria-hidden="true" className="text-ink-tertiary" />
      <h3 className="mt-3 text-title-3">{title}</h3>
      {description && <p className="mt-1 max-w-md text-callout text-ink-secondary">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
