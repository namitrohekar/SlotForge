import { Check } from 'lucide-react'

// A list of whole-row choices with a trailing check, as in iOS Settings. Native inputs keep keyboard and screen reader behaviour.
export default function ChoiceList({ type = 'checkbox', name, options, isChosen, onToggle }) {
  return (
    <ul className="border-y border-line">
      {options.map((option) => {
        const chosen = isChosen(option.value)
        return (
          <li key={option.value} className="border-b border-line last:border-b-0">
            <label
              className={`flex min-h-12 items-center gap-3 px-3 py-2 -outline-offset-2 has-focus-visible:outline-2 has-focus-visible:outline-(--sf-focus) ${
                option.disabled ? 'opacity-50' : 'cursor-pointer hover:bg-sunken'
              } ${chosen ? 'bg-accent-subtle hover:bg-accent-subtle' : ''}`}
            >
              <input
                type={type}
                name={name}
                checked={chosen}
                disabled={option.disabled}
                onChange={(event) => onToggle(option.value, event.target.checked)}
                className="sr-only"
              />
              <span className="min-w-0 flex-1">
                <span className={`block text-callout text-ink ${chosen ? 'font-medium' : ''}`}>{option.label}</span>
                {option.detail && <span className="block text-footnote text-ink-tertiary">{option.detail}</span>}
              </span>
              {option.accessory}
              <Check size={16} aria-hidden="true" className={`shrink-0 text-accent ${chosen ? '' : 'invisible'}`} />
            </label>
          </li>
        )
      })}
    </ul>
  )
}
