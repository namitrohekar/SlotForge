import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../../store/authStore.js'
import { demoLogin, fetchAuthOptions, fetchDemoUsers } from '../services/authApi.js'
import LoginPage from './LoginPage.jsx'

vi.mock('../services/authApi.js', () => ({
  fetchAuthOptions: vi.fn(),
  fetchDemoUsers: vi.fn(),
  demoLogin: vi.fn(),
  fetchCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

const users = [
  { id: 1, fullName: 'Demo Admin', email: 'admin@demo.slotforge.test', roles: ['ADMIN'] },
  { id: 6, fullName: 'Demo Student', email: 'student@demo.slotforge.test', roles: ['STUDENT'] },
]

function renderLogin(from) {
  const router = createMemoryRouter(
    [
      { path: '/login', element: <LoginPage /> },
      { path: '*', element: <p>Arrived</p> },
    ],
    { initialEntries: [{ pathname: '/login', state: from ? { from } : null }] },
  )
  render(<RouterProvider router={router} />)
  return router
}

describe('LoginPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ status: 'signedOut', user: null, reason: null })
    fetchAuthOptions.mockResolvedValue({ googleEnabled: false, demoLoginEnabled: true })
    fetchDemoUsers.mockResolvedValue(users)
  })

  it('lists demo accounts by role and signs in as the one chosen', async () => {
    demoLogin.mockResolvedValue(users[1])
    const router = renderLogin('/my/schedule')

    await userEvent.click(await screen.findByRole('button', { name: /Student/ }))

    expect(demoLogin).toHaveBeenCalledWith(6)
    expect(await screen.findByText('Arrived')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/my/schedule')
  })

  it('shows why a sign-in failed and lets the user try again', async () => {
    demoLogin.mockRejectedValue({ response: { data: { code: 'NOT_A_DEMO_USER', detail: 'Only active demo accounts can use demo sign-in.' } } })
    renderLogin()

    await userEvent.click(await screen.findByRole('button', { name: /Administrator/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Only active demo accounts')
    expect(screen.getByRole('button', { name: /Administrator/ })).toBeEnabled()
  })

  it('explains an ended session', async () => {
    useAuthStore.setState({ reason: 'expired' })
    renderLogin()

    expect(await screen.findByText('Your session ended. Sign in again to continue.')).toBeInTheDocument()
  })

  it('says when no sign-in method is set up', async () => {
    fetchAuthOptions.mockResolvedValue({ googleEnabled: false, demoLoginEnabled: false })
    renderLogin()

    expect(await screen.findByText('No sign-in method is set up')).toBeInTheDocument()
    expect(fetchDemoUsers).not.toHaveBeenCalled()
  })
})
