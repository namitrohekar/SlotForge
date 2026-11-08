import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DataTable from './DataTable.jsx'

const columns = [
  { key: 'code', header: 'Code', primary: true, sortable: true },
  { key: 'building', header: 'Building' },
  { key: 'floor', header: 'Floor', priority: 'low' },
]
const rows = [{ id: 1, code: 'CH-101', building: 'Main', floor: 2 }]

function usePhoneWidth(matches) {
  vi.stubGlobal('matchMedia', () => ({ matches, addEventListener() {}, removeEventListener() {} }))
}

describe('DataTable', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('renders a real table with a caption on wide screens', () => {
    render(<DataTable caption="Halls" columns={columns} rows={rows} />)

    expect(screen.getByRole('table', { name: 'Halls' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Building' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'CH-101' })).toBeInTheDocument()
  })

  it('renders a card list on phones and leaves out low-priority columns', () => {
    usePhoneWidth(true)
    render(<DataTable caption="Halls" columns={columns} rows={rows} />)

    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    const card = within(screen.getByRole('list', { name: 'Halls' })).getByRole('listitem')
    expect(card).toHaveTextContent('CH-101')
    expect(card).toHaveTextContent('Building')
    expect(card).not.toHaveTextContent('Floor')
  })

  it('reports the sort direction and toggles it', async () => {
    const onSort = vi.fn()
    render(<DataTable caption="Halls" columns={columns} rows={rows} sort="code,asc" onSort={onSort} />)

    expect(screen.getByRole('columnheader', { name: 'Code' })).toHaveAttribute('aria-sort', 'ascending')

    await userEvent.click(screen.getByRole('button', { name: 'Code' }))
    expect(onSort).toHaveBeenCalledWith('code,desc')
  })

  it('shows a loading state before the first rows arrive', () => {
    render(<DataTable caption="Halls" columns={columns} loading />)

    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument()
  })

  it('shows the empty state supplied by the page', () => {
    render(<DataTable caption="Halls" columns={columns} empty={<p>No halls yet</p>} />)

    expect(screen.getByText('No halls yet')).toBeInTheDocument()
  })

  it('shows an error with a retry', async () => {
    const onRetry = vi.fn()
    render(<DataTable caption="Halls" columns={columns} error={{ message: 'Database down' }} onRetry={onRetry} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Database down')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(onRetry).toHaveBeenCalled()
  })
})
