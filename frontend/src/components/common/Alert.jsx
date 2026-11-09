const rules = { info: 'border-line-strong', success: 'border-line-strong', warning: 'border-warning', danger: 'border-danger' }

// A rule in the margin, not a filled box. The words say what kind of notice it is.
export default function Alert({ tone = 'info', title, children }) {
  return (
    <div role={tone === 'danger' ? 'alert' : 'status'} className={`border-l-2 py-0.5 pl-3 text-callout text-ink-secondary ${rules[tone]}`}>
      {title && <p className="font-medium text-ink">{title}</p>}
      {children && <div>{children}</div>}
    </div>
  )
}
