import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// The storage key is also read by the theme script in index.html.
export const usePreferencesStore = create(
  persist(
    (set) => ({
      theme: 'system',
      sidebarCollapsed: false,
      setTheme: (theme) => set({ theme }),
      toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
    }),
    { name: 'slotforge-preferences' },
  ),
)
