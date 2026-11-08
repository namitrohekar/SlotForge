export default function Switch({ label, ...props }) {
  return (
    <label className="inline-flex min-h-control items-center gap-3 text-callout text-ink">
      <input type="checkbox" role="switch" className="peer sr-only" {...props} />
      <span
        aria-hidden="true"
        className="relative h-6 w-10 shrink-0 rounded-full border border-line-strong bg-sunken transition-colors duration-(--duration-fast) peer-checked:border-accent peer-checked:bg-accent peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-focus peer-disabled:opacity-50 after:absolute after:top-0.5 after:left-0.5 after:size-4.5 after:rounded-full after:bg-ink-secondary after:transition-transform after:duration-(--duration-fast) peer-checked:after:translate-x-4 peer-checked:after:bg-on-accent"
      />
      {label}
    </label>
  )
}
