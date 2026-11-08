import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'
import { useSheetDrag } from '../../hooks/useSheetDrag.js'
import IconButton from './IconButton.jsx'

function growFrom(trigger, dialog) {
  if (!trigger) return
  dialog.style.setProperty('--origin-x', `${trigger.left + trigger.width / 2 - dialog.offsetLeft}px`)
  dialog.style.setProperty('--origin-y', `${trigger.top + trigger.height / 2 - dialog.offsetTop}px`)
}

// Native <dialog> gives focus trapping, Escape and the top layer. CSS makes it a sheet on phones.
export default function Dialog({ open, onClose, title, description, footer, children }) {
  const ref = useRef(null)
  const titleId = useId()
  const descriptionId = useId()
  const { reset, handleProps } = useSheetDrag(ref, onClose)

  useEffect(() => {
    const dialog = ref.current
    if (open && !dialog.open) {
      const focused = document.activeElement
      const trigger = focused && focused !== document.body ? focused.getBoundingClientRect() : null
      dialog.showModal()
      growFrom(trigger, dialog)
    }
    if (!open && dialog.open) {
      dialog.close()
      reset()
    }
    // reset only touches refs, so it does not need to re-run this effect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  return (
    <dialog
      ref={ref}
      className="sf-dialog"
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      onClose={onClose}
      onClick={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="flex max-h-[inherit] flex-col">
        <div className="sf-sheet-handle" {...handleProps}>
          <span aria-hidden="true" className="sf-grabber" />
          <header className="flex items-start justify-between gap-4 px-6 pt-5 pb-3">
            <div className="min-w-0">
              <h2 id={titleId} className="font-serif text-title-2 font-medium">
                {title}
              </h2>
              {description && (
                <p id={descriptionId} className="mt-1 text-callout text-ink-secondary">
                  {description}
                </p>
              )}
            </div>
            <IconButton label="Close" icon={X} onClick={onClose} className="-mt-1 -mr-2" />
          </header>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-6 pb-5">{children}</div>
        {footer && (
          <footer className="flex flex-col-reverse gap-2 border-t border-line px-6 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:flex-row sm:justify-end">
            {footer}
          </footer>
        )}
      </div>
    </dialog>
  )
}
