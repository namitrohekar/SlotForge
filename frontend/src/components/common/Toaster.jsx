import { X } from 'lucide-react'
import { useToastStore } from '../../store/toastStore.js'
import IconButton from './IconButton.jsx'
import { toneIcons } from './toneIcons.js'

const iconColours = { info: 'text-info', success: 'text-success', warning: 'text-warning', danger: 'text-danger' }

export default function Toaster() {
  const toasts = useToastStore((state) => state.toasts)
  const dismiss = useToastStore((state) => state.dismiss)

  return (
    <div
      aria-live="polite"
      className="fixed inset-x-4 bottom-[calc(var(--tabbar-height)+1rem+env(safe-area-inset-bottom))] z-50 flex flex-col gap-2 md:top-4 md:right-4 md:bottom-auto md:left-auto md:w-88"
    >
      {toasts.map((toast) => {
        const Icon = toneIcons[toast.tone]
        return (
          <div
            key={toast.id}
            role={toast.tone === 'danger' ? 'alert' : 'status'}
            className="sf-toast flex items-start gap-2.5 rounded-md border border-line bg-raised py-2 pr-1.5 pl-3.5 shadow-1"
          >
            <Icon size={18} aria-hidden="true" className={`mt-2 shrink-0 ${iconColours[toast.tone]}`} />
            <p className="min-w-0 flex-1 self-center text-callout text-ink">{toast.message}</p>
            <IconButton label="Dismiss" icon={X} onClick={() => dismiss(toast.id)} />
          </div>
        )
      })}
    </div>
  )
}
