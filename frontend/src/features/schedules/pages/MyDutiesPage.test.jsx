import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { addUnavailability, listMyDuties, listMyUnavailability, removeUnavailability } from '../services/schedulesApi.js'
import MyDutiesPage from './MyDutiesPage.jsx'

vi.mock('../services/schedulesApi.js', () => ({
  listMyDuties: vi.fn(),
  listMyUnavailability: vi.fn(),
  addUnavailability: vi.fn(),
  removeUnavailability: vi.fn(),
}))

const duty = {
  date: '2026-12-07', start: '10:00:00', end: '13:00:00', hallCode: 'CE-LH1', hallName: 'Lecture Hall 1', building: 'CE Building',
  paperCode: 'CE101', paperName: 'Programming', students: 75, sessionName: 'End Semester',
}

function renderPage() {
  const router = createMemoryRouter([{ path: '/', element: <MyDutiesPage /> }])
  render(<RouterProvider router={router} />)
}

describe('MyDutiesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listMyDuties.mockResolvedValue([duty])
    listMyUnavailability.mockResolvedValue([])
  })

  it('says when and where the duty is', async () => {
    renderPage()

    expect(await screen.findByText('Lecture Hall 1 (CE-LH1)')).toBeInTheDocument()
    expect(screen.getByText(/CE101 Programming · 75 students/)).toBeInTheDocument()
  })

  it('flags a duty on a date marked unavailable', async () => {
    listMyDuties.mockResolvedValue([{ ...duty, unavailable: true }])
    renderPage()

    expect(await screen.findByText(/You marked this date as unavailable/)).toBeInTheDocument()
  })

  it('explains an empty list', async () => {
    listMyDuties.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('No duties yet')).toBeInTheDocument()
  })

  it('needs an end date on or after the start', async () => {
    renderPage()
    await screen.findByText('Lecture Hall 1 (CE-LH1)')

    await userEvent.type(screen.getByLabelText(/^From/), '2030-01-10')
    await userEvent.type(screen.getByLabelText(/^To/), '2030-01-09')
    await userEvent.type(screen.getByLabelText(/^Reason/), 'Leave')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(await screen.findByText('Must be on or after the start date')).toBeInTheDocument()
    expect(addUnavailability).not.toHaveBeenCalled()
  })

  it('saves dates and lets them be removed', async () => {
    addUnavailability.mockResolvedValue({})
    removeUnavailability.mockResolvedValue()
    listMyUnavailability.mockResolvedValue([{ id: 7, fromDate: '2030-01-10', toDate: '2030-01-10', reason: 'Leave' }])
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Remove Leave' }))
    await waitFor(() => expect(removeUnavailability).toHaveBeenCalledWith(7))

    await userEvent.type(screen.getByLabelText(/^From/), '2030-02-01')
    await userEvent.type(screen.getByLabelText(/^To/), '2030-02-02')
    await userEvent.type(screen.getByLabelText(/^Reason/), 'Conference')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    await waitFor(() => expect(addUnavailability).toHaveBeenCalledWith({ fromDate: '2030-02-01', toDate: '2030-02-02', reason: 'Conference' }))
  })
})
