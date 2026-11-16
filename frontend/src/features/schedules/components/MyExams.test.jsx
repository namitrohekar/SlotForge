import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchMyExams } from '../services/schedulesApi.js'
import MyExams from './MyExams.jsx'

vi.mock('../services/schedulesApi.js', () => ({ fetchMyExams: vi.fn() }))

const paper = (date, paperCode, paperName) => ({
  sessionName: 'End Semester', date, start: '10:00:00', end: '13:00:00', paperCode, paperName,
  hallCode: 'AUD-1', hallName: 'Auditorium', building: 'Main Building',
})
const student = { rollNo: '26BCE001', fullName: 'Aarav Nair', programme: 'B.Tech Computer Engineering', batch: '2026-30' }

const renderExams = () =>
  render(
    <MemoryRouter>
      <MyExams />
    </MemoryRouter>,
  )

describe('MyExams', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows the hall for each coming paper and keeps finished ones apart', async () => {
    fetchMyExams.mockResolvedValue({ ...student, exams: [paper('2020-01-06', 'PH101', 'Physics'), paper('2099-01-05', 'MA101', 'Mathematics')] })
    renderExams()

    const coming = await screen.findByRole('list', { name: 'Coming papers' })
    expect(within(coming).getByText('Mathematics')).toBeInTheDocument()
    expect(within(coming).getByText('Auditorium (AUD-1) · Main Building')).toBeInTheDocument()
    expect(within(coming).queryByText('Physics')).not.toBeInTheDocument()
    expect(within(screen.getByRole('list', { name: 'Finished papers' })).getByText('Physics')).toBeInTheDocument()
    expect(screen.getByText('26BCE001')).toBeInTheDocument()
  })

  it('says so when nothing is published for this student yet', async () => {
    fetchMyExams.mockResolvedValue({ ...student, exams: [] })
    renderExams()

    expect(await screen.findByText('No papers coming up')).toBeInTheDocument()
  })

  it('explains an account that is not linked to a roll number', async () => {
    fetchMyExams.mockResolvedValue({ rollNo: null, fullName: null, programme: null, batch: null, exams: [] })
    renderExams()

    expect(await screen.findByText('This account is not on the student roster')).toBeInTheDocument()
  })
})
