import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchAlgorithmComparison } from '../services/schedulesApi.js'
import AlgorithmComparisonPage from './AlgorithmComparisonPage.jsx'

vi.mock('../services/schedulesApi.js', () => ({ fetchAlgorithmComparison: vi.fn() }))

const measures = { placed: 40, ruleBreaks: 0, consecutiveDayPairs: 1400, averageGapDays: 3.88, searchSteps: 120, backtracks: 0, milliseconds: 2.5 }
const dataset = {
  name: 'Open electives',
  description: 'Groups choose their papers.',
  papers: 40,
  students: 400,
  halls: 10,
  dsaturFewestDays: 9,
  greedyFewestDays: 12,
  scenarios: [{ examDays: 12, dsatur: measures, greedy: { ...measures, consecutiveDayPairs: 3900 } }],
}

const renderPage = () =>
  render(
    <MemoryRouter>
      <AlgorithmComparisonPage />
    </MemoryRouter>,
  )

describe('AlgorithmComparisonPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows each dataset with its result and both algorithms', async () => {
    fetchAlgorithmComparison.mockResolvedValue({ searchLimit: 100000, datasets: [dataset] })
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Open electives' })).toBeInTheDocument()
    expect(screen.getByText('Fewest exam days that placed every paper: DSATUR 9, first-fit 12')).toBeInTheDocument()
    expect(screen.getByText('Neither algorithm broke a hard rule in any row')).toBeInTheDocument()
    expect(screen.getByText('1,400')).toBeInTheDocument()
    expect(screen.getByText('3,900')).toBeInTheDocument()
  })

  it('says when an algorithm never placed every paper, and flags a broken rule', async () => {
    const broken = { ...measures, placed: 30, ruleBreaks: 2 }
    fetchAlgorithmComparison.mockResolvedValue({
      searchLimit: 100000,
      datasets: [{ ...dataset, greedyFewestDays: null, scenarios: [{ examDays: 7, dsatur: measures, greedy: broken }] }],
    })
    renderPage()

    expect(await screen.findByText('Fewest exam days that placed every paper: DSATUR 9, first-fit not reached')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('2 hard rule breaks found')
  })
})
