import { useId } from 'react'

// Native radio buttons, so arrow keys and screen readers work without extra code.
export default function SegmentedControl({ label, options, value, onChange, iconOnly = false }) {
  const name = useId()
  const selected = options.findIndex((option) => option.value === value)

  return (
    <fieldset className="relative inline-grid w-fit auto-cols-fr grid-flow-col rounded-md bg-sunken p-0.5">
      <legend className="sr-only">{label}</legend>
      <span
        aria-hidden="true"
        className="sf-move absolute inset-y-0.5 left-0.5 rounded-sm bg-surface shadow-1"
        style={{ width: `calc((100% - 0.25rem) / ${options.length})`, transform: `translateX(${selected * 100}%)` }}
      />
      {options.map((option) => (
        <label key={option.value} title={iconOnly ? option.label : undefined} className="relative">
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
            className="peer sr-only"
          />
          <span className="sf-press flex h-8 min-w-8 items-center justify-center gap-1.5 rounded-sm px-2 text-footnote text-ink-secondary peer-checked:font-medium peer-checked:text-ink peer-focus-visible:outline-2 peer-focus-visible:outline-offset-1 peer-focus-visible:outline-focus">
            {option.icon && <option.icon size={15} aria-hidden="true" />}
            <span className={iconOnly ? 'sr-only' : undefined}>{option.label}</span>
          </span>
        </label>
      ))}
    </fieldset>
  )
}
