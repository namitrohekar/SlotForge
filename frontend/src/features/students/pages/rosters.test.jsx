import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../../store/authStore.js'
import HallsPage from '../../halls/pages/HallsPage.jsx'
import { listHalls } from '../../halls/services/hallsApi.js'
import { listAllBatches } from '../../academics/services/academicsApi.js'
import { listStudents } from '../services/studentsApi.js'
import StudentsPage from './StudentsPage.jsx'
import { menuNamed } from '../../../test/menus.js'

vi.mock('../../halls/services/hallsApi.js', () => ({
  listHalls: vi.fn(),
  createHall: vi.fn(),
  updateHall: vi.fn(),
  deleteHall: vi.fn(),
}))
vi.mock('../services/studentsApi.js', () => ({
  listStudents: vi.fn(),
  createStudent: vi.fn(),
  updateStudent: vi.fn(),
  deleteStudent: vi.fn(),
}))
vi.mock('../../academics/services/academicsApi.js', () => ({ listAllBatches: vi.fn() }))

const page = (items) => ({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 })
const signInAs = (...roles) =>
  useAuthStore.setState({ status: 'signedIn', user: { id: 1, fullName: 'X', email: 'x@vjti.ac.in', roles } })

function renderPage(Page) {
  render(<RouterProvider router={createMemoryRouter([{ path: '/', element: <Page /> }])} />)
}

describe('Rosters', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listAllBatches.mockResolvedValue([])
    listHalls.mockResolvedValue(
      page([{ id: 1, code: 'AUD-1', name: 'Auditorium', building: 'Main', floor: 0, capacity: 120, status: 'AVAILABLE', allocationCount: 0 }]),
    )
    listStudents.mockResolvedValue(
      page([
        {
          id: 1, rollNo: '25MCA001', fullName: 'Asha Rao', email: 'a@vjti.ac.in', batchId: 1, batchName: '2025-27',
          courseCode: 'MCA', courseName: 'MCA', currentSemester: 3, enrolmentCount: 5,
        },
      ]),
    )
  })

  it('shows halls read-only to invigilators', async () => {
    signInAs('INVIGILATOR')
    renderPage(HallsPage)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('Main, floor 0')).toBeInTheDocument()
    expect(within(table).getByText('Available')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add hall' })).not.toBeInTheDocument()
  })

  it('keeps Delete off for a student with exam enrolment', async () => {
    signInAs('EXAM_COORDINATOR')
    renderPage(StudentsPage)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('MCA · 2025-27')).toBeInTheDocument()
    const menu = menuNamed('Actions for Asha Rao')
    expect(within(menu).getByRole('menuitem', { name: 'Delete', hidden: true })).toBeDisabled()
  })
})
