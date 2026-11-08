import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { usePagedList } from './usePagedList.js'

function Harness({ load }) {
  const list = usePagedList(load)
  const location = useLocation()

  return (
    <>
      <p data-testid="url">{location.search}</p>
      <button type="button" onClick={() => list.setPage(2)}>
        Page 3
      </button>
      <button type="button" onClick={() => list.setFilter('q', 'hall')}>
        Search
      </button>
      <button type="button" onClick={() => list.setSort('code,desc')}>
        Sort
      </button>
    </>
  )
}

function setup(start = '/halls') {
  const load = vi.fn().mockResolvedValue({ items: [] })
  render(
    <MemoryRouter initialEntries={[start]}>
      <Harness load={load} />
    </MemoryRouter>,
  )
  return load
}

describe('usePagedList', () => {
  it('reads list state from the URL', async () => {
    const load = setup('/halls?q=main&page=1')
    await screen.findByTestId('url')

    expect(load).toHaveBeenCalledWith({ q: 'main', page: '1' })
  })

  it('writes the page to the URL and reloads', async () => {
    const load = setup()

    await userEvent.click(screen.getByRole('button', { name: 'Page 3' }))

    expect(screen.getByTestId('url')).toHaveTextContent('?page=2')
    expect(load).toHaveBeenLastCalledWith({ page: '2' })
  })

  it('returns to the first page when a filter or sort changes', async () => {
    setup('/halls?page=4')

    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.getByTestId('url')).toHaveTextContent('?q=hall')

    await userEvent.click(screen.getByRole('button', { name: 'Sort' }))
    expect(screen.getByTestId('url')).toHaveTextContent('q=hall&sort=code%2Cdesc')
  })
})
