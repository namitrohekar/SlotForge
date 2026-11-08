import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../../store/authStore.js'
import {
  getCurriculum,
  listAllCourses,
  listAllCurricula,
  listAllDepartments,
  listBatches,
  listCourses,
  listCurriculumSubjects,
  listDepartments,
  replaceCurriculumSubjects,
} from '../services/academicsApi.js'
import CurriculumDetailPage from '../pages/CurriculumDetailPage.jsx'
import BatchesTab from './BatchesTab.jsx'
import CoursesTab from './CoursesTab.jsx'
import DepartmentsTab from './DepartmentsTab.jsx'

vi.mock('../services/academicsApi.js', () => ({
  listDepartments: vi.fn(),
  listAllDepartments: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
  listCourses: vi.fn(),
  listAllCurricula: vi.fn(),
  listBatches: vi.fn(),
  createBatch: vi.fn(),
  updateBatch: vi.fn(),
  deleteBatch: vi.fn(),
  listAllCourses: vi.fn(),
  getCurriculum: vi.fn(),
  listCurriculumSubjects: vi.fn(),
  replaceCurriculumSubjects: vi.fn(),
  listSubjects: vi.fn(),
  createCourse: vi.fn(),
  updateCourse: vi.fn(),
  deleteCourse: vi.fn(),
}))

const page = (items) => ({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 })
const ceit = { id: 3, code: 'CEIT', name: 'Computer Engineering & Information Technology', courseCount: 2, subjectCount: 0 }
const maths = { id: 10, code: 'MATH', name: 'Mathematics', courseCount: 0, subjectCount: 4 }
const mca = {
  id: 7,
  code: 'MCA',
  name: 'Master of Computer Applications',
  totalSemesters: 4,
  departmentId: 4,
  departmentCode: 'MCA',
  departmentName: 'Master in Computer Applications',
  curriculumCount: 1,
}

const signInAs = (...roles) =>
  useAuthStore.setState({ status: 'signedIn', user: { id: 1, fullName: 'X', email: 'x@vjti.ac.in', roles } })

function renderAt(path, Component, pattern = path.split('?')[0]) {
  const router = createMemoryRouter([{ path: pattern, element: <Component /> }], { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('Academic structure', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listDepartments.mockResolvedValue(page([ceit, maths]))
    listAllDepartments.mockResolvedValue([ceit, maths])
    listCourses.mockResolvedValue(page([mca]))
  })

  it('lists departments with a link to their programmes, and lets a coordinator add one', async () => {
    signInAs('EXAM_COORDINATOR')
    renderAt('/academics/departments', DepartmentsTab)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('Computer Engineering & Information Technology')).toBeInTheDocument()
    expect(within(table).getByRole('link', { name: '2' })).toHaveAttribute('href', '/academics/courses?departmentId=3')
    expect(screen.getByRole('button', { name: 'Add department' })).toBeInTheDocument()
  })

  it('is read-only for faculty', async () => {
    signInAs('FACULTY')
    renderAt('/academics/departments', DepartmentsTab)

    await screen.findByRole('table')
    expect(screen.queryByRole('button', { name: 'Add department' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Actions for/ })).not.toBeInTheDocument()
  })

  it('shows programme length the way VJTI states it, and filters by department from the URL', async () => {
    signInAs('ADMIN')
    renderAt('/academics/courses?departmentId=4', CoursesTab)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('4 semesters · 2 years')).toBeInTheDocument()
    expect(listCourses).toHaveBeenCalledWith({ departmentId: '4' })
  })

  it('shows how far a batch has reached and keeps Delete off while it has students', async () => {
    signInAs('EXAM_COORDINATOR')
    listAllCourses.mockResolvedValue([])
    listAllCurricula.mockResolvedValue([])
    listBatches.mockResolvedValue(
      page([
        {
          id: 1, name: '2025-27', admissionYear: 2025, currentSemester: 3, curriculumId: 9, curriculumName: '2024 scheme',
          courseId: 7, courseCode: 'MCA', courseName: 'Master of Computer Applications', totalSemesters: 4, studentCount: 60,
        },
      ]),
    )
    renderAt('/academics/batches', BatchesTab)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('3 of 4')).toBeInTheDocument()
    const menu = screen.getByRole('menu', { name: 'Actions for 2025-27', hidden: true })
    expect(within(menu).getByRole('menuitem', { name: 'Delete', hidden: true })).toBeDisabled()
  })

  it('offers Delete only for a department nothing refers to', async () => {
    signInAs('EXAM_COORDINATOR')
    renderAt('/academics/departments', DepartmentsTab)
    await screen.findByRole('table')

    const deleteIn = (name) =>
      within(screen.getByRole('menu', { name: `Actions for ${name}`, hidden: true })).getByRole('menuitem', {
        name: 'Delete',
        hidden: true,
      })
    expect(deleteIn('Mathematics')).toBeDisabled()
    expect(deleteIn('Computer Engineering & Information Technology')).toBeDisabled()
  })

  it('lays a curriculum out semester by semester and saves the whole placement on removal', async () => {
    signInAs('EXAM_COORDINATOR')
    getCurriculum.mockResolvedValue({
      id: 9, name: '2024 scheme', courseName: 'Master of Computer Applications', courseCode: 'MCA',
      effectiveFromYear: 2024, totalSemesters: 4, subjectCount: 2, batchCount: 0,
    })
    listCurriculumSubjects.mockResolvedValue([
      { subjectId: 1, code: 'MCA101', name: 'Mathematical Foundations', heavy: true, departmentCode: 'MATH', semesterNo: 1, elective: false },
      { subjectId: 2, code: 'MCA311', name: 'Big Data Analytics', heavy: false, departmentCode: 'MCA', semesterNo: 3, elective: true },
    ])
    replaceCurriculumSubjects.mockResolvedValue([])
    renderAt('/academics/curricula/9', CurriculumDetailPage, '/academics/curricula/:id')

    expect(await screen.findByRole('heading', { name: '2024 scheme' })).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(4)
    const third = screen.getByRole('region', { name: /Semester 3/ })
    expect(within(third).getByText('Elective')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Remove Mathematical Foundations from semester 1' }))

    await waitFor(() =>
      expect(replaceCurriculumSubjects).toHaveBeenCalledWith('9', [{ subjectId: 2, semesterNo: 3, elective: true }]),
    )
  })
})
