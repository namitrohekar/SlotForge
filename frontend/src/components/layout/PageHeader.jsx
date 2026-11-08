import { useEffect, useRef } from 'react'
import { useNavigationType } from 'react-router'

export default function PageHeader({ eyebrow, title, description, actions }) {
  const heading = useRef(null)
  const shownTitle = useRef(null)
  const navigationType = useNavigationType()

  // Tells screen reader and keyboard users that the page changed. On first load
  // focus stays at the top of the document, so the first Tab reaches the skip link.
  useEffect(() => {
    document.title = `${title} · SlotForge`
    // Only for a new page: moving between sections of this one keeps focus where it is.
    if (navigationType !== 'POP' && shownTitle.current !== title) heading.current.focus({ preventScroll: true })
    shownTitle.current = title
  }, [title, navigationType])

  return (
    <header className="mb-8 md:mb-12">
      {eyebrow && <p className="mb-1.5 text-footnote text-ink-tertiary">{eyebrow}</p>}
      <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <h1 ref={heading} tabIndex={-1} className="font-serif text-display font-medium">
          {title}
        </h1>
        {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
      </div>
      {description && <p className="mt-2 max-w-(--reading-max) text-body text-ink-secondary">{description}</p>}
    </header>
  )
}
