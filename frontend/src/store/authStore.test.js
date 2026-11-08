import { beforeEach, describe, expect, it, vi } from 'vitest'
import { demoLogin, fetchCurrentUser, logout } from '../features/auth/services/authApi.js'
import { api } from '../services/apiClient.js'
import { useAuthStore } from './authStore.js'

vi.mock('../features/auth/services/authApi.js', () => ({
  fetchCurrentUser: vi.fn(),
  demoLogin: vi.fn(),
  logout: vi.fn(),
}))

const admin = { id: 1, fullName: 'Demo Admin', email: 'admin@demo.slotforge.test', roles: ['ADMIN'] }

// Sends a request through the real interceptors, answered by a stub adapter.
const respond = (status, data) =>
  api.get('/anything', { adapter: async (config) => Promise.reject(Object.assign(new Error('fail'), { config, response: { status, data } })) })

describe('authStore', () => {
  beforeEach(() => useAuthStore.setState({ status: 'loading', user: null, reason: null }))

  it('restores an existing session on start', async () => {
    fetchCurrentUser.mockResolvedValue(admin)

    await useAuthStore.getState().load()

    expect(useAuthStore.getState()).toMatchObject({ status: 'signedIn', user: admin })
  })

  it('is signed out when there is no session', async () => {
    fetchCurrentUser.mockRejectedValue(new Error('401'))

    await useAuthStore.getState().load()

    expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null })
  })

  it('signs in and out', async () => {
    demoLogin.mockResolvedValue(admin)
    logout.mockResolvedValue()

    await useAuthStore.getState().signIn(1)
    expect(useAuthStore.getState().user).toEqual(admin)

    await useAuthStore.getState().signOut()
    expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null, reason: 'signedOut' })
  })

  it('returns to sign-in when the server ends the session', async () => {
    useAuthStore.setState({ status: 'signedIn', user: admin })

    await expect(respond(401, { code: 'UNAUTHENTICATED' })).rejects.toThrow()

    expect(useAuthStore.getState()).toMatchObject({ status: 'signedOut', user: null, reason: 'expired' })
  })

  it('explains a deactivated account', async () => {
    useAuthStore.setState({ status: 'signedIn', user: admin })

    await expect(respond(401, { code: 'ACCOUNT_DEACTIVATED' })).rejects.toThrow()

    expect(useAuthStore.getState().reason).toBe('deactivated')
  })

  it('leaves the session alone on other errors', async () => {
    useAuthStore.setState({ status: 'signedIn', user: admin })

    await expect(respond(403, { code: 'FORBIDDEN' })).rejects.toThrow()

    expect(useAuthStore.getState().status).toBe('signedIn')
  })
})
