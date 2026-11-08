import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../store/authStore.js'
import ProtectedRoute, { RequireRoles } from './ProtectedRoute.jsx'

function renderAt(path) {
  const router = createMemoryRouter(
    [
      { path: '/login', element: <p>Login screen</p> },
      {
        Component: ProtectedRoute,
        children: [
          { path: '/', element: <p>Dashboard</p> },
          { element: <RequireRoles roles={['ADMIN']} />, children: [{ path: '/system', element: <p>System</p> }] },
        ],
      },
    ],
    { initialEntries: [path] },
  )
  render(<RouterProvider router={router} />)
  return router
}

const signedInAs = (...roles) =>
  useAuthStore.setState({ status: 'signedIn', reason: null, user: { id: 1, fullName: 'X', email: 'x@demo.slotforge.test', roles } })

describe('ProtectedRoute', () => {
  beforeEach(() => useAuthStore.setState({ status: 'loading', user: null, reason: null }))

  it('waits while the session is being checked', () => {
    renderAt('/')

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument()
  })

  it('sends signed-out visitors to sign-in and remembers where they were going', () => {
    useAuthStore.setState({ status: 'signedOut' })
    const router = renderAt('/system?tab=db')

    expect(screen.getByText('Login screen')).toBeInTheDocument()
    expect(router.state.location.state).toEqual({ from: '/system?tab=db' })
  })

  it('forgets the last page after a deliberate sign-out', () => {
    useAuthStore.setState({ status: 'signedOut', reason: 'signedOut' })
    const router = renderAt('/system')

    expect(router.state.location.state).toBeNull()
  })

  it('refuses a page the role cannot open, without leaving it', () => {
    signedInAs('STUDENT')
    const router = renderAt('/system')

    expect(screen.getByRole('heading', { level: 1, name: 'No access' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/system')
  })

  it('opens the page for an allowed role', () => {
    signedInAs('ADMIN')
    renderAt('/system')

    expect(screen.getByText('System')).toBeInTheDocument()
  })
})
