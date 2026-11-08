import { create } from 'zustand'

const AUTO_DISMISS_MS = 5000
let nextId = 1

export const useToastStore = create((set, get) => ({
  toasts: [],
  dismiss: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
  push: ({ tone = 'success', message }) => {
    const id = nextId++
    set((state) => ({ toasts: [...state.toasts, { id, tone, message }] }))
    // Errors stay until dismissed, so they cannot be missed.
    if (tone !== 'danger') setTimeout(() => get().dismiss(id), AUTO_DISMISS_MS)
  },
}))
