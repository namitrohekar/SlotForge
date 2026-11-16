import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listPublishedTimetables } from '../services/schedulesApi.js'
import NoticePrintPage from './NoticePrintPage.jsx'

vi.mock('../services/schedulesApi.js', () => ({ listPublishedTimetables: vi.fn() }))

const paper = (date, paperCode, paperName) => ({ date, start: '10:00:00', end: '13:00:00', paperCode, paperName, halls: ['AUD-1'] })
const timetable = {
  sessionId: 7, sessionName: 'End Semester Examination', startDate: '2026-12-07', endDate: '2026-12-18', publishedAt: '2026-11-20T10:00:00+05:30',
  papers: [paper('2026-12-07', 'MA101', 'Mathematics'), paper('2026-12-07', 'MCA301', 'Machine Learning'), paper('2026-12-08', 'PH101', 'Physics')],
}

function renderAt(sessionId) {
  const router = createMemoryRouter([{ path: '/print/notice/:sessionId', element: <NoticePrintPage /> }], {
    initialEntries: [`/print/notice/${sessionId}`],
  })
  render(<RouterProvider router={router} />)
}

describe('NoticePrintPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listPublishedTimetables.mockResolvedValue([timetable])
  })

  it('prints one session as a notice, naming each day once', async () => {
    renderAt(7)

    expect(await screen.findByRole('heading', { level: 1, name: 'End Semester Examination' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Print' })).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(4)
    expect(screen.getAllByText('Mon, 7 Dec')).toHaveLength(1)
    expect(screen.getByText('Tue, 8 Dec')).toBeInTheDocument()
  })

  it('says so when that session has no published timetable', async () => {
    renderAt(99)

    expect(await screen.findByText('Nothing to print')).toBeInTheDocument()
  })
})
