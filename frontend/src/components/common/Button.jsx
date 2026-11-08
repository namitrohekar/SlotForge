import Spinner from './Spinner.jsx'

const variants = {
  primary: 'bg-accent text-on-accent hover:bg-accent-hover active:bg-accent-hover',
  secondary: 'border border-line-strong bg-surface text-ink hover:bg-sunken active:bg-sunken',
  quiet: 'text-ink-secondary hover:bg-sunken hover:text-ink active:bg-sunken',
  danger: 'bg-danger text-on-danger hover:bg-danger-hover active:bg-danger-hover',
}

export default function Button({
  variant = 'primary',
  icon: Icon,
  loading = false,
  disabled = false,
  type = 'button',
  className = '',
  children,
  ...props
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`inline-flex h-control items-center justify-center gap-2 rounded-md px-4 text-callout font-medium whitespace-nowrap select-none sf-press disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {loading ? <Spinner label="Working" size={16} /> : Icon && <Icon size={16} aria-hidden="true" />}
      {children}
    </button>
  )
}
