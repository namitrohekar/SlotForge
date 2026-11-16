import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, Outlet, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../store/authStore.js'
import { demoLogin, fetchAuthOptions, fetchDemoUsers } from '../auth/services/authApi.js'
import TimetableTab from './components/TimetableTab.jsx'
import {
  approveVersion, fetchPreflight, generateTimetable, listCandidateSlots, listEntries, listVersions, listViolations, moveExam, repairVersion, submitVersion,
} from './services/schedulesApi.js'

vi.mock('../auth/services/authApi.js', () => ({
  fetchAuthOptions: vi.fn(),
  fetchDemoUsers: vi.fn(),
  demoLogin: vi.fn(),
  fetchCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('./services/schedulesApi.js', () => ({
  generateTimetable: vi.fn(),
  fetchPreflight: vi.fn().mockResolvedValue([]),
  listCandidateSlots: vi.fn(),
  moveExam: vi.fn(),
  setExamLock: vi.fn(),
  repairVersion: vi.fn(),
  listVersions: vi.fn(),
  listEntries: vi.fn(),
  listViolations: vi.fn(),
  deleteVersion: vi.fn(),
  submitVersion: vi.fn(),
  approveVersion: vi.fn(),
  returnVersion: vi.fn(),
  publishVersion: vi.fn(),
}))

const session = {
  id: 1, startDate: '2026-12-07', endDate: '2026-12-19', slotCount: 12, examinationCount: 3, status: 'PLANNING',
  progress: { papersWithoutStudents: [], approverPost: 'Head of MCA', approverName: 'Dr Rao', approverIds: [1] },
}
const version = {
  id: 5, versionNo: 2, status: 'DRAFT', createdAt: '2026-11-08T10:00:00Z', examsTotal: 3, examsScheduled: 2, slotsUsed: 2, searchNodes: 1240, backtracks: 4, durationMs: 86,
  unplaced: [{ examId: 3, code: 'MCA301 Machine Learning', blockers: [{ constraint: 'CAPACITY', slots: 2, example: 'needs 90 seats, 60 free' }] }],
}
const entry = (examinationId, subjectName, date, conflicts) => ({
  examinationId, subjectCode: `C${examinationId}`, subjectName, heavy: false, departmentCode: 'MCA', durationMinutes: 180, slotId: examinationId,
  date, start: '10:00:00', end: '13:00:00', students: 60,
  halls: [{ code: 'MB-101', name: 'Main 101', building: 'Main', seats: 60, capacity: 60, invigilators: ['A Rao', 'B Shah'] }], conflicts,
})
const maths = entry(1, 'Mathematics', '2026-12-07', [{ examinationId: 2, sharedStudents: 12 }])
const physics = entry(2, 'Physics', '2026-12-14', [{ examinationId: 1, sharedStudents: 12 }])

function renderTab(given = { session, canEdit: true }) {
  // The session page always supplies reloadSession, so every test does too.
  const context = { reloadSession: vi.fn(), ...given }
  const router = createMemoryRouter([{ path: '/', element: <Outlet context={context} />, children: [{ index: true, element: <TimetableTab /> }] }])
  render(<RouterProvider router={router} />)
}

describe('Timetable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ status: 'signedIn', user: null })
    listEntries.mockResolvedValue([maths, physics])
    listViolations.mockResolvedValue([])
    fetchPreflight.mockResolvedValue([])
    listCandidateSlots.mockResolvedValue([])
    fetchAuthOptions.mockResolvedValue({ demoLoginEnabled: false })
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

  it('offers the slots a paper may move to, says why the others are refused, and moves it', async () => {
    listVersions.mockResolvedValue([unsent])
    listCandidateSlots.mockResolvedValue([
      { slotId: 11, date: '2026-12-07', start: '10:00:00', end: '13:00:00', current: true, reason: null, example: null },
      { slotId: 12, date: '2026-12-14', start: '10:00:00', end: '13:00:00', current: false, reason: 'STUDENT_CLASH', example: 'PH101, 12 shared students' },
      { slotId: 13, date: '2026-12-16', start: '10:00:00', end: '13:00:00', current: false, reason: null, example: null },
    ])
    moveExam.mockResolvedValue({ run: { versionId: 9 }, level: 1, papersMoved: 1 })
    renderTab()

    await userEvent.click(await screen.findByRole('button', { name: /Mathematics/ }))
    const dialog = screen.getByRole('dialog', { name: 'Mathematics' })
    expect(await within(dialog).findByText('Not possible: a student already has a paper (PH101, 12 shared students)')).toBeInTheDocument()
    expect(within(dialog).getAllByRole('radio')).toHaveLength(2)
    expect(within(dialog).getByRole('radio', { name: /14 Dec/ })).toBeDisabled()
    expect(within(dialog).getByRole('button', { name: 'Move here' })).toBeDisabled()

    await userEvent.click(within(dialog).getByRole('radio', { name: /16 Dec/ }))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Move here' }))

    expect(moveExam).toHaveBeenCalledWith(5, 1, 13)
    await waitFor(() => expect(listVersions).toHaveBeenCalledTimes(2))
  })

  it('does not offer to move a paper once the timetable has been sent', async () => {
    listVersions.mockResolvedValue([complete])
    renderTab()

    await userEvent.click(await screen.findByRole('button', { name: /Mathematics/ }))

    expect(within(screen.getByRole('dialog', { name: 'Mathematics' })).queryByText('Move to another slot')).not.toBeInTheDocument()
    expect(listCandidateSlots).not.toHaveBeenCalled()
  })

  it('repairs a draft with problems, keeping what is still valid', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([unsent])
    listViolations.mockResolvedValue(['MB-101 can no longer take MCA301.'])
    repairVersion.mockResolvedValue({ run: { versionId: 9 }, level: 1, papersMoved: 2 })
    renderTab()

    await userEvent.click(await screen.findByRole('button', { name: 'Repair' }))

    expect(repairVersion).toHaveBeenCalledWith(5)
    await waitFor(() => expect(listVersions).toHaveBeenCalledTimes(2))
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
  const unsent = { ...version, examsScheduled: 3, unplaced: [] }
  const complete = { ...unsent, submittedAt: '2026-11-08T11:00:00Z' }

  it('offers a head of department approval only when every paper is placed', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([version])
    renderTab({ session, canEdit: false })

    expect(await screen.findByText('Every paper must be placed before it can be approved')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Send back' })).not.toBeInTheDocument()
  })

  it('lets a coordinator send a complete draft for approval', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([unsent])
    submitVersion.mockResolvedValue()
    renderTab({ session, canEdit: true, reloadSession: vi.fn() })

    expect(await screen.findByText('Not sent for approval yet')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Send for approval' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Send version 2 for approval?' })).getByRole('button', { name: 'Send' }))

    await waitFor(() => expect(submitVersion).toHaveBeenCalledWith(5))
  })

  it('gives a head of department nothing to act on until the draft is sent', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([unsent])
    renderTab({ session, canEdit: false })

    expect(await screen.findByText('Not sent for approval yet')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Send back' })).not.toBeInTheDocument()
  })

  it('stops offering Send once a draft has been sent or has gone out of date', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([complete])
    renderTab()
    await screen.findByText('Waiting for Dr Rao, Head of MCA to approve it')
    expect(screen.queryByRole('button', { name: 'Send for approval' })).not.toBeInTheDocument()
    cleanup()

    listVersions.mockResolvedValue([unsent])
    listViolations.mockResolvedValue(['MCA302 is outside its allowed dates.'])
    renderTab()
    await screen.findByText('MCA302 is outside its allowed dates.')
    expect(screen.queryByRole('button', { name: 'Send for approval' })).not.toBeInTheDocument()
  })

  it('shows who did each step and whose turn it is', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([{ ...complete, generatedBy: 'Asha', sentBy: 'Bela' }])
    renderTab()

    const journey = within(await screen.findByRole('list', { name: 'Approval journey' }))
    expect(journey.getByText('Asha')).toBeInTheDocument()
    expect(journey.getByText('Bela')).toBeInTheDocument()
    expect(journey.getByText('Dr Rao, Head of MCA')).toBeInTheDocument()
  })

  it('in a demo, lets you become whoever the flow is waiting for', async () => {
    signInAs('EXAM_COORDINATOR')
    fetchAuthOptions.mockResolvedValue({ demoLoginEnabled: true })
    fetchDemoUsers.mockResolvedValue([{ id: 3, fullName: 'Demo Head', email: 'h@demo.slotforge.test', roles: ['HOD'] }])
    demoLogin.mockResolvedValue({ id: 3, fullName: 'Demo Head', email: 'h@demo.slotforge.test', roles: ['HOD'] })
    listVersions.mockResolvedValue([complete])
    renderTab({ session: { ...session, progress: { ...session.progress, approverIds: [3] } }, canEdit: true })

    await userEvent.click(await screen.findByRole('button', { name: 'Continue as Demo Head' }))

    await waitFor(() => expect(demoLogin).toHaveBeenCalledWith(3))
  })

  it('opens on the version under review and names it, and protects it from deletion', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([{ ...unsent, id: 6, versionNo: 3 }, complete])
    renderTab()

    expect(await screen.findByText('Waiting for Dr Rao, Head of MCA to approve it')).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Version 2 · Waiting for approval' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete version 2' })).not.toBeInTheDocument()
  })

  it('warns about a published timetable that needs attention', async () => {
    listVersions.mockResolvedValue([{ ...complete, status: 'PUBLISHED', publishedAt: '2026-11-08T12:00:00Z', invigilatorCount: 3 }])
    listViolations.mockResolvedValue(['A Rao can no longer invigilate MCA101 in MB-101 on Mon, 7 Dec.'])
    renderTab()

    expect(await screen.findByText('A Rao can no longer invigilate MCA101 in MB-101 on Mon, 7 Dec.')).toBeInTheDocument()
    expect(screen.getByText(/This published timetable needs attention/)).toBeInTheDocument()
  })

  it('gives a different head of department nothing to act on', async () => {
    signInAs('HOD')
    listVersions.mockResolvedValue([complete])
    renderTab({ session: { ...session, progress: { ...session.progress, approverIds: [99] } }, canEdit: false })

    expect(await screen.findByText('Waiting for Dr Rao, Head of MCA to approve it')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Send back' })).not.toBeInTheDocument()
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
    expect(screen.getByText('Repair it, or fix the cause and generate again')).toBeInTheDocument()
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

    expect(await screen.findByText('Waiting for Dr Rao, Head of MCA to approve it')).toBeInTheDocument()
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
    renderTab({ session: { ...session, progress: { papersWithoutStudents: ['MCA311 Big Data', 'MCA312 Mobile'], approverPost: 'Head of MCA', approverName: 'Dr Rao', approverIds: [1] } }, canEdit: true })

    expect(await screen.findByText('2 papers have no students, so they are not in this timetable')).toBeInTheDocument()
    expect(screen.getByText(/MCA311 Big Data, MCA312 Mobile/)).toBeInTheDocument()
  })

  it('warns when nobody is able to approve', async () => {
    signInAs('EXAM_COORDINATOR')
    listVersions.mockResolvedValue([complete])
    renderTab({ session: { ...session, progress: { papersWithoutStudents: [], approverPost: 'Head of MCA', approverName: null, approverIds: [] } }, canEdit: true })

    expect(await screen.findByText('There is no Head of MCA. Ask an administrator to appoint one.')).toBeInTheDocument()
  })

  it('says what is missing rather than offering Generate', async () => {
    listVersions.mockResolvedValue([])
    renderTab({ session: { ...session, slotCount: 0 }, canEdit: true })

    expect(await screen.findByText('This session needs time slots first')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Generate timetable' })).not.toBeInTheDocument()
  })
})
