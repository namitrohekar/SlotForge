import { toneIcons, toneStyles } from './toneIcons.js'

export default function Alert({ tone = 'info', title, children }) {
  const Icon = toneIcons[tone]

  return (
    <div role={tone === 'danger' ? 'alert' : 'status'} className={`flex items-start gap-2.5 rounded-md px-3.5 py-3 ${toneStyles[tone]}`}>
      <Icon size={18} aria-hidden="true" className="mt-px shrink-0" />
      <div className="min-w-0 text-callout">
        {title && <p className="font-medium">{title}</p>}
        {children && <div>{children}</div>}
      </div>
    </div>
  )
}
