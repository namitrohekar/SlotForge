import { create } from 'zustand'
import { demoLogin, fetchCurrentUser, logout } from '../features/auth/services/authApi.js'
import { api } from '../services/apiClient.js'

// Held in memory only. The session itself is an HttpOnly cookie that JavaScript cannot read.
export const useAuthStore = create((set) => ({
  status: 'loading',
  user: null,
  // Why the user is signed out: 'expired', 'deactivated' or 'signedOut'. Null on first visit.
  reason: null,

  load: async () => {
    try {
      set({ status: 'signedIn', user: await fetchCurrentUser(), reason: null })
    } catch {
      set({ status: 'signedOut', user: null })
    }
  },

  signIn: async (userId) => {
    const user = await demoLogin(userId)
    set({ status: 'signedIn', user, reason: null })
  },

  signOut: async () => {
    await logout().catch(() => {})
    set({ status: 'signedOut', user: null, reason: 'signedOut' })
  },

  expire: (code) =>
    set({ status: 'signedOut', user: null, reason: code === 'ACCOUNT_DEACTIVATED' ? 'deactivated' : 'expired' }),
}))

// Any 401 while signed in means the session ended on the server, so the app returns to sign-in.
api.interceptors.response.use(undefined, (error) => {
  if (error.response?.status === 401 && useAuthStore.getState().status === 'signedIn') {
    useAuthStore.getState().expire(error.response.data?.code)
  }
  return Promise.reject(error)
})
