import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchHealth } from '../services/systemApi.js'
import SystemStatusPage from './SystemStatusPage.jsx'

vi.mock('../services/systemApi.js', () => ({ fetchHealth: vi.fn() }))

const healthy = { status: 'UP', database: 'slotforge', postgresVersion: '18.6', schemaVersion: 2, requiredSchemaVersion: 2 }

const renderPage = () =>
  render(
    <MemoryRouter>
      <SystemStatusPage />
    </MemoryRouter>,
  )

describe('SystemStatusPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows live database details', async () => {
    fetchHealth.mockResolvedValue(healthy)
    renderPage()

    expect(await screen.findByText('Connected')).toBeInTheDocument()
    expect(screen.getByText('slotforge')).toBeInTheDocument()
    expect(screen.getByText('18.6')).toBeInTheDocument()
    expect(screen.getByText('Code needs 2')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('warns when the database is behind the code', async () => {
    fetchHealth.mockResolvedValue({ ...healthy, schemaVersion: 1 })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('behind the code')
  })

  it('shows the API error and can retry', async () => {
    fetchHealth.mockRejectedValueOnce({
      response: { data: { code: 'DATABASE_UNAVAILABLE', detail: 'The database is not available right now.' } },
    })
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('not available')

    fetchHealth.mockResolvedValue(healthy)
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('Connected')).toBeInTheDocument()
  })
})
