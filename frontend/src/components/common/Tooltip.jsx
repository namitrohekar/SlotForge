import { cloneElement, useId, useRef } from 'react'

const GAP = 6
const EDGE = 8

// Hover and focus only, so it must never be the only way to learn something: touch has no hover.
// In the top layer, so a scrolling table or a dialog cannot clip it.
export default function Tooltip({ text, children }) {
  const id = useId()
  const tip = useRef(null)

  const show = (event) => {
    const element = tip.current
    if (!element.showPopover || element.matches(':popover-open')) return
    element.showPopover()
    const target = event.currentTarget.getBoundingClientRect()
    const left = target.left + target.width / 2 - element.offsetWidth / 2
    element.style.left = `${Math.max(EDGE, Math.min(left, window.innerWidth - element.offsetWidth - EDGE))}px`
    element.style.top = `${target.top - element.offsetHeight - GAP}px`
  }

  const hide = () => {
    if (tip.current.hidePopover && tip.current.matches(':popover-open')) tip.current.hidePopover()
  }

  return (
    <span className="relative inline-flex" onMouseEnter={show} onMouseLeave={hide} onFocus={show} onBlur={hide}>
      {cloneElement(children, { 'aria-describedby': id })}
      <span
        ref={tip}
        role="tooltip"
        id={id}
        popover="manual"
        className="pointer-events-none inset-auto m-0 rounded-sm border-0 bg-ink px-2 py-1 text-caption whitespace-nowrap text-canvas"
      >
        {text}
      </span>
    </span>
  )
}
