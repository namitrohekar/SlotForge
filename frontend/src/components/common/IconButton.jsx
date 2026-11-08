// The label is required: without it a screen reader would announce only "button".
export default function IconButton({ label, icon: Icon, className = '', type = 'button', ...props }) {
  return (
    <button
      type={type}
      aria-label={label}
      title={label}
      className={`inline-flex size-control shrink-0 items-center justify-center rounded-md text-ink-secondary select-none sf-press hover:bg-sunken hover:text-ink active:bg-sunken disabled:opacity-50 ${className}`}
      {...props}
    >
      <Icon size={18} aria-hidden="true" />
    </button>
  )
}
