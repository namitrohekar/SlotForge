import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../store/authStore.js'
import TimetableTab from './components/TimetableTab.jsx'
import { approveVersion, generateTimetable, listEntries, listVersions, listViolations } from './services/schedulesApi.js'

vi.mock('./services/schedulesApi.js', () => ({
  generateTimetable: vi.fn(),
  listVersions: vi.fn(),
  listEntries: vi.fn(),
  listViolations: vi.fn(),
  deleteVersion: vi.fn(),
  approveVersion: vi.fn(),
  returnVersion: vi.fn(),
  publishVersion: vi.fn(),
}))

const session = {
  id: 1, startDate: '2026-12-07', endDate: '2026-12-19', slotCount: 12, examinationCount: 3, status: 'PLANNING',
  progress: { papersWithoutStudents: [], activeApprovers: 1 },
}
const version = {
  id: 5, versionNo: 2, status: 'DRAFT', createdAt: '2026-11-08T10:00:00Z', examsTotal: 3, examsScheduled: 2, slotsUsed: 2, searchNodes: 1240, backtracks: 4, durationMs: 86,
  unplaced: [{ examId: 3, code: 'MCA301 Machine Learning', blockers: [{ constraint: 'CAPACITY', slots: 2, example: 'needs 90 seats, 60 free' }] }],
}
const entry = (examinationId, subjectName, date, conflicts) => ({
  examinationId, subjectCode: `C${examinationId}`, subjectName, heavy: false, departmentCode: 'MCA', durationMinutes: 180, slotId: examinationId,
  date, start: '10:00:00', end: '13:00:00', students: 60,
  halls: [{ code: 'MB-101', name: 'Main 101', building: 'Main', seats: 60, capacity: 60 }], conflicts,
})
const maths = entry(1, 'Mathematics', '2026-12-07', [{ examinationId: 2, sharedStudents: 12 }])
const physics = entry(2, 'Physics', '2026-12-14', [{ examinationId: 1, sharedStudents: 12 }])

function renderTab(context = { session, canEdit: true }) {
  const router = createMemoryRouter([{ path: '/', element: <Outlet context={context} />, children: [{ index: true, element: <TimetableTab /> }] }])
  render(<RouterProvider router={router} />)
}

describe('Timetable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ status: 'signedIn', user: null })
    listEntries.mockResolvedValue([maths, physics])
    listViolations.mockResolvedValue([])
  })

  it('shows what was placed, how, and exactly why the rest were not', async () => {
    listVersions.mockResolvedValue([version])
    renderTab()

    expect(await screen.findByRole('heading', { name: '2 of 3 ready papers placed' })).toBeInTheDocument()
    expect(screen.getByText(/2 slots used · generated/)).toBeInTheDocument()
    expect(screen.getByText('MCA301 Machine Learning')).toBeInTheDocument()
    expect(screen.getByText('2 slots: not enough hall seats (needs 90 seats, 60 free)')).toBeInTheDocument()
    expect(within(await screen.findByRole('table')).getByText('Mathematics')).toBeInTheDocument()
  })

  it('reveals papers that share students, in words as well as colour', async () => {
    listVersions.mockResolvedValue([version])
    renderTab()

    const card = await screen.findByRole('button', { name: /Mathematics/ })
    await userEvent.hover(card)

    expect(screen.getByRole('button', { name: /Physics/ })).toHaveTextContent('Shares 12 students')
  })

  it('opens a paper to show its halls and the papers it must avoid', async () => {
    listVersions.mockResolvedValue([version])
    renderTab()

    await userEvent.click(await screen.findByRole('button', { name: /Mathematics/ }))

    const dialog = screen.getByRole('dialog', { name: 'Mathematics' })
    expect(within(dialog).getByText('60 of 60 seats')).toBeInTheDocument()
    expect(within(dialog).getByText('Shares students with')).toBeInTheDocument()
    expect(within(dialog).getByText('Physics')).toBeInTheDocument()
  })

  it('generates the first timetable and opens it', async () => {
    listVersions.mockResolvedValueOnce([]).mockResolvedValue([version])
    generateTimetable.mockResolvedValue({ versionId: 5, unplaced: [], examsScheduled: 3, examsTotal: 3 })
    renderTab()

    await userEvent.click(await screen.findByRole('button', { name: 'Generate timetable' }))

    expect(generateTimetable).toHaveBeenCalledWith(1)
    expect(await screen.findByRole('heading', { name: '2 of 3 ready papers placed' })).toBeInTheDocument()
    await waitFor(() => expect(listEntries).toHaveBeenCalledWith(5))
  })

  const signInAs = (...roles) => useAuthStore.setState({ user: { id: 1, fullName: 'X', email: 'x@vjti.ac.in', roles } })
  const complete = { ...version, examsScheduled: 3, unplaced: [] }

  it('offers a head of department approval only when every paper is placed', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([version])
    renderTab({ session, canEdit: false })

    expect(await screen.findByText('Every paper must be placed before it can be approved.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send back' })).toBeInTheDocument()
  })

  it('lets a head of department approve a complete draft', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([complete])
    approveVersion.mockResolvedValue()
    renderTab({ session, canEdit: false })

    await userEvent.click(await screen.findByRole('button', { name: 'Approve' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Approve version 2?' })).getByRole('button', { name: 'Approve' }))

    await waitFor(() => expect(approveVersion).toHaveBeenCalledWith(5))
  })

  it('refuses approval of an out-of-date draft and says exactly what changed', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([complete])
    listViolations.mockResolvedValue(['MCA301 has 1 student without a seat.'])
    renderTab({ session, canEdit: false })

    expect(await screen.findByText('MCA301 has 1 student without a seat.')).toBeInTheDocument()
    expect(screen.getByText('Out of date. It has to be generated again.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send back' })).toBeInTheDocument()
  })

  it('takes Publish away when an approved timetable goes out of date', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([{ ...complete, status: 'REVIEWED', reviewedBy: 'Dr Rao' }])
    listViolations.mockResolvedValue(['MCA302 is in a slot that is now blocked (Fri, 4 Dec).'])
    renderTab()

    expect(await screen.findByText('MCA302 is in a slot that is now blocked (Fri, 4 Dec).')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument()
  })

  it('does not re-check an incomplete version, which already lists its missing papers', async () => {
    listVersions.mockResolvedValue([version])
    renderTab()

    await screen.findByRole('heading', { name: '2 of 3 ready papers placed' })
    expect(listViolations).not.toHaveBeenCalled()
  })

  it('makes a coordinator wait for approval, then offers Publish', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([complete])
    renderTab()

    expect(await screen.findByText('Waiting for a head of department to approve it.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Publish' })).not.toBeInTheDocument()
    cleanup()

    listVersions.mockResolvedValue([{ ...complete, status: 'REVIEWED', reviewedBy: 'Dr Rao' }])
    renderTab()

    expect(await screen.findByText('Approved by Dr Rao. Ready to publish.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument()
  })

  it('shows the coordinator why a draft was sent back', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([{ ...complete, reviewComment: 'Move Physics earlier' }])
    renderTab()

    expect(await screen.findByText('Move Physics earlier')).toBeInTheDocument()
  })

  it('says plainly when papers were left out for having no students', async () => {
    listVersions.mockResolvedValue([version])
    renderTab({ session: { ...session, progress: { papersWithoutStudents: ['MCA311 Big Data', 'MCA312 Mobile'], activeApprovers: 1 } }, canEdit: true })

    expect(await screen.findByText('2 papers have no students, so they are not in this timetable')).toBeInTheDocument()
    expect(screen.getByText(/MCA311 Big Data, MCA312 Mobile/)).toBeInTheDocument()
  })

  it('warns when nobody is able to approve', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([complete])
    renderTab({ session: { ...session, progress: { papersWithoutStudents: [], activeApprovers: 0 } }, canEdit: true })

    expect(await screen.findByText(/No head of department can sign in/)).toBeInTheDocument()
  })

  it('says what is missing rather than offering Generate', async () => {
    listVersions.mockResolvedValue([])
    renderTab({ session: { ...session, slotCount: 0 }, canEdit: true })

    expect(await screen.findByText('This session needs time slots first.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Generate timetable' })).not.toBeInTheDocument()
  })
})
