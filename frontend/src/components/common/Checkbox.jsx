export default function Checkbox({ label, ...props }) {
  return (
    <label className="inline-flex min-h-control items-center gap-2.5 text-callout text-ink">
      <input type="checkbox" className="size-4 shrink-0 accent-(--sf-accent)" {...props} />
      {label}
    </label>
  )
}
