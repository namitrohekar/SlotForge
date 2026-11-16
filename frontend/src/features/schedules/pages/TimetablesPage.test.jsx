import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listPublishedTimetables } from '../services/schedulesApi.js'
import TimetablesPage from './TimetablesPage.jsx'

vi.mock('../services/schedulesApi.js', () => ({ listPublishedTimetables: vi.fn() }))

const timetable = {
  sessionId: 7, sessionName: 'End Semester Examination', startDate: '2026-12-07', endDate: '2026-12-18', publishedAt: '2026-11-20T10:00:00+05:30',
  papers: [
    { date: '2026-12-07', start: '10:00:00', end: '13:00:00', paperCode: 'MA101', paperName: 'Engineering Mathematics I', halls: ['AUD-1', 'EE-LH1'] },
    { date: '2026-12-07', start: '10:00:00', end: '13:00:00', paperCode: 'MCA301', paperName: 'Machine Learning', halls: ['MB-101'] },
  ],
}

const renderPage = () =>
  render(
    <MemoryRouter>
      <TimetablesPage />
    </MemoryRouter>,
  )

describe('TimetablesPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lists each published session with its papers and every hall of a split paper', async () => {
    listPublishedTimetables.mockResolvedValue([timetable])
    renderPage()

    expect(await screen.findByRole('heading', { name: 'End Semester Examination' })).toBeInTheDocument()
    expect(screen.getByText('Engineering Mathematics I')).toBeInTheDocument()
    expect(screen.getByText('AUD-1, EE-LH1')).toBeInTheDocument()
    expect(screen.getByText('Machine Learning')).toBeInTheDocument()
    expect(screen.getByText(/^2 papers · published/)).toBeInTheDocument()
  })

  it('says so when nothing is published', async () => {
    listPublishedTimetables.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('No timetable has been published yet')).toBeInTheDocument()
  })
})
