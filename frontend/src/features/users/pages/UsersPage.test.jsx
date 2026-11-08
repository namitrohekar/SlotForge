import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../../store/authStore.js'
import { createUser, listUsers } from '../services/usersApi.js'
import UsersPage from './UsersPage.jsx'

vi.mock('../services/usersApi.js', () => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
  renameUser: vi.fn(),
  changeRoles: vi.fn(),
  deactivateUser: vi.fn(),
  activateUser: vi.fn(),
}))

const page = (items) => ({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 })
const hod = {
  id: 3,
  fullName: 'Asha Rao',
  email: 'asha@vjti.ac.in',
  roles: ['HOD'],
  status: 'ACTIVE',
  lastLoginAt: null,
  createdAt: '2026-11-08T10:00:00Z',
}

const student = {
  id: 8,
  fullName: 'Sam Patel',
  email: 'sam@vjti.ac.in',
  roles: ['STUDENT'],
  status: 'DEACTIVATED',
  lastLoginAt: null,
  createdAt: '2026-11-08T10:00:00Z',
}

const menuLabels = (name) =>
  within(screen.getByRole('menu', { name, hidden: true }))
    .getAllByRole('menuitem', { hidden: true })
    .map((item) => item.textContent)

function renderPage(path = '/users') {
  const router = createMemoryRouter([{ path: '/users', element: <UsersPage /> }], { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('UsersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ status: 'signedIn', user: { id: 1, fullName: 'Admin', email: 'a@x.test', roles: ['ADMIN'] } })
    listUsers.mockResolvedValue(page([hod]))
  })

  it('lists users with their roles and status, and passes URL filters to the API', async () => {
    renderPage('/users?role=HOD')

    expect(await screen.findByText('Asha Rao')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Head of department')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Active')).toBeInTheDocument()
    expect(screen.getByText('Never')).toBeInTheDocument()
    expect(listUsers).toHaveBeenCalledWith({ role: 'HOD' })
  })

  it('adds a user after checking the form', async () => {
    createUser.mockResolvedValue({ ...hod, id: 7 })
    renderPage()
    await screen.findByText('Asha Rao')

    await userEvent.click(screen.getByRole('button', { name: 'Add user' }))
    const dialog = screen.getByRole('dialog', { name: 'Add user' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add user' }))

    expect(within(dialog).getByText('Name is required')).toBeInTheDocument()
    expect(within(dialog).getByText('Enter a valid email address')).toBeInTheDocument()
    expect(within(dialog).getByText('Choose at least one role')).toBeInTheDocument()
    expect(createUser).not.toHaveBeenCalled()

    await userEvent.type(within(dialog).getByLabelText(/Full name/), 'Ravi Kumar')
    await userEvent.type(within(dialog).getByLabelText(/Email/), 'ravi@vjti.ac.in')
    await userEvent.click(within(dialog).getByLabelText('Invigilator'))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add user' }))

    expect(createUser).toHaveBeenCalledWith({ fullName: 'Ravi Kumar', email: 'ravi@vjti.ac.in', roles: ['INVIGILATOR'] })
    await waitFor(() => expect(listUsers).toHaveBeenCalledTimes(2))
  })

  it('keeps a deactivated account read-only, offering only details and reactivation', async () => {
    listUsers.mockResolvedValue(page([hod, student]))
    renderPage()
    await screen.findByText('Sam Patel')

    expect(menuLabels('Actions for Sam Patel')).toEqual(['View details', 'Reactivate'])
    expect(menuLabels('Actions for Asha Rao')).toEqual(['Edit', 'Deactivate'])
    expect(screen.getByText('Sam Patel').closest('tr')).not.toHaveClass('hover:bg-sunken')
    expect(screen.getByText('Asha Rao').closest('tr')).toHaveClass('hover:bg-sunken')
  })

  it('only offers roles that can be held together', async () => {
    renderPage()
    await screen.findByText('Asha Rao')

    await userEvent.click(screen.getByRole('button', { name: 'Add user' }))
    const dialog = screen.getByRole('dialog', { name: 'Add user' })
    await userEvent.click(within(dialog).getByLabelText('Administrator'))

    expect(within(dialog).getByLabelText('Student')).toBeDisabled()
    expect(within(dialog).getByLabelText('Exam coordinator')).toBeDisabled()
    expect(within(dialog).getByLabelText('Head of department')).toBeDisabled()
    expect(within(dialog).getByLabelText('Faculty')).toBeEnabled()
    expect(within(dialog).getByLabelText('Invigilator')).toBeEnabled()
  })
})
