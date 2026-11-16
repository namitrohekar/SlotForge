import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../store/authStore.js'
import { formatDuration, formatTime } from '../../utils/format.js'
import ExaminationsTab from './components/ExaminationsTab.jsx'
import SlotsTab from './components/SlotsTab.jsx'
import { listExaminations, listSlots } from './services/examsApi.js'
import { nextStep, sessionSteps } from './sessionSteps.js'
import { countRequestedSlots } from './slotPreview.js'

vi.mock('./services/examsApi.js', () => ({
  listSlots: vi.fn(),
  generateSlots: vi.fn(),
  changeSlotStatus: vi.fn(),
  deleteSlot: vi.fn(),
  listExaminations: vi.fn(),
  createFromCurriculum: vi.fn(),
  updateExamination: vi.fn(),
  deleteExamination: vi.fn(),
  listEnrolments: vi.fn(),
  addEnrolments: vi.fn(),
  removeEnrolment: vi.fn(),
}))
vi.mock('../academics/services/academicsApi.js', () => ({ listAllBatches: vi.fn() }))
vi.mock('../students/services/studentsApi.js', () => ({ listStudents: vi.fn() }))

const session = { id: 1, name: 'S', startDate: '2026-12-07', endDate: '2026-12-13', status: 'PLANNING', progress: { slotsTooShort: 1, shortestPaperMinutes: 180 } }

function renderTab(Tab, canEdit) {
  const context = { session, reloadSession: vi.fn(), canEdit }
  const router = createMemoryRouter([{ path: '/', element: <Outlet context={context} />, children: [{ index: true, element: <Tab /> }] }])
  render(<RouterProvider router={router} />)
}

describe('Exam setup', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ status: 'signedIn', user: { id: 1, fullName: 'X', email: 'x@vjti.ac.in', roles: ['FACULTY'] } })
  })

  it('counts the slots a template asks for, using Monday as day 1 and Sunday as 7', () => {
    const times = [{ start: '10:00', end: '13:00' }, { start: '14:00', end: '17:00' }]
    expect(countRequestedSlots({ from: '2026-12-07', to: '2026-12-13', weekdays: [1, 2, 3, 4, 5, 6], times })).toBe(12)
    expect(countRequestedSlots({ from: '2026-12-07', to: '2026-12-13', weekdays: [7], times })).toBe(2)
    expect(countRequestedSlots({ from: '2026-12-13', to: '2026-12-07', weekdays: [7], times })).toBe(0)
  })

  it('states durations the way a timetable does', () => {
    expect(formatDuration(180)).toBe('3 h')
    expect(formatDuration(90)).toBe('1 h 30 min')
    expect(formatDuration(45)).toBe('45 min')
    expect(formatTime('14:00:00')).toBe('2:00 pm')
    expect(formatTime('00:30')).toBe('12:30 am')
  })

  it('counts only what would really be added, skipping times that overlap existing slots', () => {
    const times = [{ start: '10:00', end: '13:00' }, { start: '14:00', end: '17:00' }]
    const existing = [
      { date: '2026-12-07', start: '10:00:00', end: '13:00:00' },
      { date: '2026-12-07', start: '15:00:00', end: '17:00:00' },
      { date: '2026-12-08', start: '13:00:00', end: '14:00:00' },
    ]
    expect(countRequestedSlots({ from: '2026-12-07', to: '2026-12-08', weekdays: [1, 2], times, existing })).toBe(2)
  })

  describe('where a session stands', () => {
    const progress = {
      openSlots: 12, slotsTooShort: 0, shortestPaperMinutes: 180, readyPapers: 3, papersWithoutStudents: [],
      latestVersionNo: 0, latestVersionStatus: null, latestPlaced: null, latestTotal: null, inReviewStatus: null, latestProblems: 0, approverIds: [3],
    }
    const at = (changes) => ({ status: 'PLANNING', progress: { ...progress, ...changes } })
    const coordinator = { isCoordinator: true }

    it('walks a coordinator through the job in order', () => {
      expect(nextStep(at({ openSlots: 0 }), coordinator).to).toBe('slots')
      expect(nextStep(at({ readyPapers: 0 }), coordinator).to).toBe('examinations')
      expect(nextStep(at({ slotsTooShort: 12 }), coordinator).message).toContain('3 h')
      expect(nextStep(at({}), coordinator).to).toBe('timetable')
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3 }), coordinator).message).toContain('send for approval')
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3, latestProblems: 2 }), coordinator).message).toContain('problems to fix')
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3, inReviewStatus: 'DRAFT' }), coordinator)).toBeNull()
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'REVIEWED', latestPlaced: 3, latestTotal: 3, inReviewStatus: 'REVIEWED' }), coordinator).message).toContain('publish')
    })

    it('asks a head of department to act only on a complete draft', () => {
      const hod = { userId: 3 }
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3, inReviewStatus: 'DRAFT' }), hod).action).toBe('Review it')
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3 }), hod)).toBeNull()
      expect(nextStep(at({ latestVersionNo: 1, latestVersionStatus: 'DRAFT', latestPlaced: 3, latestTotal: 3, inReviewStatus: 'DRAFT' }), { userId: 9 })).toBeNull()
    })

    it('flags papers left without students, and hides the timetable from staff who cannot see drafts', () => {
      const steps = sessionSteps(at({ papersWithoutStudents: ['MCA311 Big Data'] }), true)
      expect(steps[1]).toMatchObject({ state: 'attention', detail: '3 ready, 1 without students' })
      expect(sessionSteps(at({}), false).map((step) => step.label)).toEqual(['Time slots', 'Papers'])
    })
  })

  it('groups slots by day and explains a blocked one', async () => {
    listSlots.mockResolvedValue([
      { id: 1, date: '2026-12-07', start: '10:00:00', end: '13:00:00', status: 'OPEN', blockReason: null, usageCount: 0 },
      { id: 2, date: '2026-12-07', start: '14:00:00', end: '17:00:00', status: 'BLOCKED', blockReason: 'Convocation', usageCount: 0 },
      { id: 3, date: '2026-12-08', start: '15:00:00', end: '17:00:00', status: 'OPEN', blockReason: null, usageCount: 0 },
    ])
    renderTab(SlotsTab, true)

    expect(await screen.findByText('3 slots over 2 days')).toBeInTheDocument()
    expect(screen.getByText('2:00 pm – 5:00 pm')).toBeInTheDocument()
    expect(screen.getByText('Convocation')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate slots' })).toBeInTheDocument()
    expect(screen.getByText(/1 open slot is shorter than the shortest paper/)).toBeInTheDocument()
    expect(screen.getByText('Too short')).toBeInTheDocument()
  })

  it('does not offer generation when the session cannot be edited', async () => {
    listSlots.mockResolvedValue([])
    renderTab(SlotsTab, false)

    expect(await screen.findByText('No time slots yet')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Generate slots' })).not.toBeInTheDocument()
  })

  it('shows papers, and hides who is enrolled from faculty', async () => {
    listExaminations.mockResolvedValue({
      items: [{ id: 1, subjectId: 1, subjectCode: 'MCA101', subjectName: 'Mathematics', departmentCode: 'MATH', heavy: true, durationMinutes: 180, earliestDate: null, latestDate: null, status: 'READY', enrolled: 60 }],
      page: 0, size: 20, totalItems: 1, totalPages: 1,
    })
    renderTab(ExaminationsTab, false)

    const table = await screen.findByRole('table')
    expect(within(table).getByText('3 h')).toBeInTheDocument()
    expect(within(table).getByText('60')).toBeInTheDocument()
    expect(within(table).queryByRole('button', { name: /student/ })).not.toBeInTheDocument()
    expect(within(table).getByRole('tooltip', { hidden: true })).toHaveTextContent('rest days')
  })
})
