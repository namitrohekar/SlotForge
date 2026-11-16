import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../store/authStore.js'
import AppLayout from './AppLayout.jsx'

vi.mock('../../features/notifications/services/notificationsApi.js', () => ({
  fetchNotifications: vi.fn().mockResolvedValue({ unread: 2, items: [] }),
}))
import PageHeader from './PageHeader.jsx'

function renderLayout() {
  const router = createMemoryRouter([
    {
      path: '/',
      Component: AppLayout,
      children: [
        { index: true, element: <PageHeader title="Dashboard" /> },
        { path: 'users', element: <PageHeader title="Users" /> },
      ],
    },
  ])
  render(<RouterProvider router={router} />)
}

describe('AppLayout', () => {
  beforeEach(() => {
    useAuthStore.setState({
      status: 'signedIn',
      user: { id: 1, fullName: 'Demo Admin', email: 'admin@demo.slotforge.test', roles: ['ADMIN'] },
    })
  })

  it('has the landmarks and skip link that keyboard users rely on', () => {
    renderLayout()

    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main')
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main')
    expect(screen.getAllByRole('navigation', { name: 'Main' }).length).toBeGreaterThan(0)
  })

  it('offers the theme choice', () => {
    renderLayout()

    expect(screen.getByRole('menuitem', { name: 'Dark theme', hidden: true })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'System theme (current)', hidden: true })).toBeInTheDocument()
  })

  it('leaves focus alone on first load, so Tab can reach the skip link', () => {
    renderLayout()

    expect(screen.getByRole('heading', { level: 1, name: 'Dashboard' })).not.toHaveFocus()
    expect(document.title).toBe('Dashboard · SlotForge')
  })

  it('moves focus to the new page title after navigating', async () => {
    renderLayout()

    await userEvent.click(screen.getAllByRole('link', { name: 'Users' })[0])

    expect(await screen.findByRole('heading', { level: 1, name: 'Users' })).toHaveFocus()
    expect(document.title).toBe('Users · SlotForge')
  })

  it('shows who is signed in, with any new notifications', async () => {
    renderLayout()

    expect(screen.getByText('Demo Admin')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Account, 2 new notifications' })).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Notifications, 2 new', hidden: true })).toBeInTheDocument()
  })
})
