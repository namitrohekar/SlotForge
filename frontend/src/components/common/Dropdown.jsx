import { useId, useRef } from 'react'
import { Ellipsis } from 'lucide-react'
import IconButton from './IconButton.jsx'

const GAP = 4
const EDGE = 8

function place(menu, trigger) {
  const rect = trigger.getBoundingClientRect()
  const left = Math.max(EDGE, Math.min(rect.right - menu.offsetWidth, window.innerWidth - menu.offsetWidth - EDGE))
  const fitsBelow = rect.bottom + GAP + menu.offsetHeight + EDGE <= window.innerHeight
  menu.style.transformOrigin = fitsBelow ? 'top right' : 'bottom right'
  menu.style.left = `${left}px`
  menu.style.top = `${fitsBelow ? rect.bottom + GAP : rect.top - GAP - menu.offsetHeight}px`
}

// The Popover API gives the top layer, click-outside and Escape. Only keyboard focus is handled here.
export default function Dropdown({ label, icon = Ellipsis, items }) {
  const id = useId()
  const menuRef = useRef(null)
  const triggerRef = useRef(null)

  const menuItems = () => [...menuRef.current.querySelectorAll('[role="menuitem"]:not(:disabled)')]

  const handleToggle = (event) => {
    if (event.newState !== 'open') return
    place(menuRef.current, triggerRef.current)
    menuItems()[0]?.focus()
  }

  const handleKeyDown = (event) => {
    const options = menuItems()
    const current = options.indexOf(document.activeElement)
    const targets = {
      ArrowDown: options[(current + 1) % options.length],
      ArrowUp: options[(current - 1 + options.length) % options.length],
      Home: options[0],
      End: options[options.length - 1],
    }
    if (event.key in targets) {
      event.preventDefault()
      targets[event.key]?.focus()
    }
  }

  const choose = (item) => {
    menuRef.current.hidePopover()
    triggerRef.current.focus()
    item.onSelect()
  }

  return (
    <>
      <IconButton ref={triggerRef} label={label} icon={icon} aria-haspopup="menu" popoverTarget={id} />
      <div
        ref={menuRef}
        id={id}
        popover="auto"
        role="menu"
        aria-label={label}
        className="sf-menu"
        onToggle={handleToggle}
        onKeyDown={handleKeyDown}
      >
        {items.map((item) => (
          <button
            key={item.label}
            type="button"
            role="menuitem"
            disabled={item.disabled}
            onClick={() => choose(item)}
            className={`flex min-h-control w-full items-center gap-2.5 rounded-sm px-2.5 text-left text-callout hover:bg-sunken focus-visible:bg-sunken active:bg-sunken disabled:opacity-50 ${item.danger ? 'text-danger' : 'text-ink'}`}
          >
            {item.icon && <item.icon size={16} aria-hidden="true" />}
            {item.label}
          </button>
        ))}
      </div>
    </>
  )
}
