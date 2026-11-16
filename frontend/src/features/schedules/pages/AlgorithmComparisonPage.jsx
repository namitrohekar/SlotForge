import DataTable from '../../../components/common/DataTable.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatNumber } from '../../../utils/format.js'
import { fetchAlgorithmComparison } from '../services/schedulesApi.js'

const days = (value) => `${value.toFixed(1)} days`
const time = (value) => `${value.toFixed(1)} ms`

const columnsFor = (papers) => [
  { key: 'examDays', header: 'Exam days', primary: true, render: (row) => `${row.examDays} days` },
  { key: 'dsaturPlaced', header: 'Placed, DSATUR', numeric: true, render: (row) => `${row.dsatur.placed} of ${papers}` },
  { key: 'greedyPlaced', header: 'Placed, first-fit', numeric: true, render: (row) => `${row.greedy.placed} of ${papers}` },
  { key: 'dsaturPairs', header: 'Back-to-back, DSATUR', numeric: true, render: (row) => formatNumber(row.dsatur.consecutiveDayPairs) },
  { key: 'greedyPairs', header: 'Back-to-back, first-fit', numeric: true, render: (row) => formatNumber(row.greedy.consecutiveDayPairs) },
  { key: 'dsaturGap', header: 'Average gap, DSATUR', numeric: true, render: (row) => days(row.dsatur.averageGapDays) },
  { key: 'greedyGap', header: 'Average gap, first-fit', numeric: true, render: (row) => days(row.greedy.averageGapDays) },
  { key: 'steps', header: 'DSATUR steps', numeric: true, priority: 'low', render: (row) => formatNumber(row.dsatur.searchSteps) },
  { key: 'dsaturTime', header: 'Time, DSATUR', numeric: true, priority: 'low', render: (row) => time(row.dsatur.milliseconds) },
  { key: 'greedyTime', header: 'Time, first-fit', numeric: true, priority: 'low', render: (row) => time(row.greedy.milliseconds) },
]

const fewest = (value) => value ?? 'not reached'

function Dataset({ dataset }) {
  const breaks = dataset.scenarios.reduce((total, { dsatur, greedy }) => total + dsatur.ruleBreaks + greedy.ruleBreaks, 0)

  return (
    <section aria-label={dataset.name}>
      <h2 className="text-title-2">{dataset.name}</h2>
      <p className="mt-1 max-w-(--reading-max) text-callout text-ink-secondary">{dataset.description}</p>
      <p data-numeric className="mt-1 text-footnote text-ink-tertiary">
        {dataset.papers} papers · {formatNumber(dataset.students)} students · {dataset.halls} halls
      </p>
      <p data-numeric className="mt-4 mb-3 text-callout font-medium text-ink">
        Fewest exam days that placed every paper: DSATUR {fewest(dataset.dsaturFewestDays)}, first-fit {fewest(dataset.greedyFewestDays)}
      </p>
      <DataTable caption={`${dataset.name}, by number of exam days`} columns={columnsFor(dataset.papers)} rows={dataset.scenarios} rowKey="examDays" />
      {breaks === 0 ? (
        <p className="mt-3 text-footnote text-ink-tertiary">Neither algorithm broke a hard rule in any row</p>
      ) : (
        <p role="alert" data-numeric className="mt-3 text-footnote text-danger">
          {breaks} hard rule {breaks === 1 ? 'break' : 'breaks'} found
        </p>
      )}
    </section>
  )
}

function Notes({ searchLimit }) {
  const notes = [
    ['DSATUR', `Places the paper with the fewest options left first, and backs up when it gets stuck. Here it stops after ${formatNumber(searchLimit)} search steps.`],
    ['First-fit', 'Takes papers in the order they were entered and gives each the earliest slot that keeps every hard rule. It never backs up.'],
    ['Hard rules', 'One paper per student per day, heavy papers more than one day apart, one paper per hall per slot, and a seat for every student.'],
    ['Back-to-back', 'How many times a student has papers on two days in a row. Lower is better.'],
    ['Average gap', 'Average days between one of a student\'s papers and their next. Higher gives more time to prepare.'],
    ['Time', 'Changes from one machine to another. Every other number is the same on every run.'],
  ]

  return (
    <section aria-labelledby="comparison-notes">
      <h2 id="comparison-notes" className="text-title-2">
        How to read this
      </h2>
      <dl className="mt-3 border-t border-line">
        {notes.map(([term, meaning]) => (
          <div key={term} className="grid gap-x-6 gap-y-0.5 border-b border-line py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
            <dt className="text-callout font-medium text-ink">{term}</dt>
            <dd className="text-callout text-ink-secondary">{meaning}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

// The evidence for choosing DSATUR: both methods, the same fixed data, the same measurements.
export default function AlgorithmComparisonPage() {
  const { data, error, reload } = useApi(fetchAlgorithmComparison)

  return (
    <>
      <PageHeader title="Algorithm comparison" description="DSATUR against a simple first-fit method on fixed test data. Running this changes nothing in your sessions." />
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {!data && !error && <Skeleton className="h-96 w-full rounded-lg" />}
      {data && (
        <div className="space-y-14">
          {data.datasets.map((dataset) => (
            <Dataset key={dataset.name} dataset={dataset} />
          ))}
          <Notes searchLimit={data.searchLimit} />
        </div>
      )}
    </>
  )
}
