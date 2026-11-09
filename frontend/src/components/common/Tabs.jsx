import { useId, useLayoutEffect, useRef } from 'react'

export default function Tabs({ label, tabs, value, onChange, children }) {
  const baseId = useId()
  const listRef = useRef(null)
  const indicatorRef = useRef(null)
  const tabId = (id) => `${baseId}-tab-${id}`
  const panelId = `${baseId}-panel`

  useLayoutEffect(() => {
    const tab = listRef.current.querySelector('[aria-selected="true"]')
    if (!tab) return
    indicatorRef.current.style.width = `${tab.offsetWidth}px`
    indicatorRef.current.style.transform = `translateX(${tab.offsetLeft}px)`
  }, [value])

  const select = (index) => {
    const next = tabs[(index + tabs.length) % tabs.length]
    onChange(next.id)
    listRef.current.querySelector(`[id="${tabId(next.id)}"]`).focus()
  }

  const handleKeyDown = (event) => {
    const current = tabs.findIndex((tab) => tab.id === value)
    const moves = { ArrowRight: current + 1, ArrowLeft: current - 1, Home: 0, End: tabs.length - 1 }
    if (event.key in moves) {
      event.preventDefault()
      select(moves[event.key])
    }
  }

  return (
    <div className="min-w-0">
      <div
        ref={listRef}
        role="tablist"
        aria-label={label}
        onKeyDown={handleKeyDown}
        className="relative flex gap-1 overflow-x-auto border-b border-line"
      >
        {tabs.map((tab) => {
          const selected = tab.id === value
          return (
            <button
              key={tab.id}
              id={tabId(tab.id)}
              type="button"
              role="tab"
              aria-selected={selected}
              aria-controls={panelId}
              tabIndex={selected ? 0 : -1}
              onClick={() => onChange(tab.id)}
              className={`sf-press min-h-control px-3 text-callout whitespace-nowrap [--press-scale:0.98] ${
                selected ? 'font-medium text-ink' : 'text-ink-secondary hover:text-ink'
              }`}
            >
              {tab.label}
            </button>
          )
        })}
        <span ref={indicatorRef} aria-hidden="true" className="sf-move absolute bottom-0 left-0 h-0.5 bg-tick" />
      </div>
      <div role="tabpanel" id={panelId} aria-labelledby={tabId(value)} tabIndex={0} className="pt-5">
        {children}
      </div>
    </div>
  )
}
