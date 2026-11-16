import { useCallback } from 'react'
import { ChevronLeft } from 'lucide-react'
import { Link, useOutletContext, useSearchParams } from 'react-router'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import StatTile from '../../../components/common/StatTile.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatDay, formatNumber, formatTime } from '../../../utils/format.js'
import { fetchBaseline, listEntries, listVersions } from '../services/schedulesApi.js'
import ClashPicture from './ClashPicture.jsx'

const percent = (part) => `${Math.round(part * 100)}%`
const days = (value) => `${value.toFixed(1)} days`

const IDEA = [
  ['Clash', 'Two papers clash when at least one student sits both. Clashing papers can never share a slot.'],
  ['Order', 'The engine always places next the paper with the fewest slots still open to it. Left for later, that paper is the one that runs out of room.'],
  ['Choice', 'Among the slots a paper may take, it picks the one that keeps each student’s papers furthest apart.'],
]

function Section({ id, title, intro, children }) {
  return (
    <section aria-labelledby={id} className="mt-12">
      <h3 id={id} className="text-title-2">
        {title}
      </h3>
      {intro && <p className="mt-1 mb-4 max-w-(--reading-max) text-callout text-ink-secondary">{intro}</p>}
      {children}
    </section>
  )
}

function Quality({ quality }) {
  return (
    <div className="grid gap-y-6 sm:grid-cols-2 lg:grid-cols-4">
      <StatTile label="Quality score" value={`${quality.score.toFixed(1)} of 100`} context="Half spacing, three tenths duty sharing, two tenths hall use" />
      <StatTile
        label="Spacing"
        value={percent(quality.spacing)}
        context={`${formatNumber(quality.backToBackPairs)} back-to-back days · ${days(quality.averageGapDays)} between papers`}
      />
      <StatTile label="Duty sharing" value={percent(quality.dutyBalance)} context={`${quality.fewestDuties} to ${quality.mostDuties} duties a person`} />
      <StatTile label="Hall use" value={percent(quality.utilisation)} context="Seats filled in the halls in use" />
    </div>
  )
}

function Baseline({ sessionId }) {
  const { data, error, reload } = useApi(useCallback(() => fetchBaseline(sessionId), [sessionId]))

  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <Skeleton className="h-40 w-full rounded-lg" />

  const rows = [
    ['Papers placed', (run) => `${run.placed} of ${data.papers}`],
    ['Hard rules broken', (run) => run.ruleBreaks],
    ['Back-to-back days for students', (run) => formatNumber(run.consecutiveDayPairs)],
    ['Average gap between a student’s papers', (run) => days(run.averageGapDays)],
    ['Search steps', (run) => formatNumber(run.searchSteps)],
    ['Times it backed up', (run) => formatNumber(run.backtracks)],
  ]

  return (
    <table className="w-full border-collapse text-left text-callout">
      <caption className="sr-only">DSATUR compared with first-fit on this session</caption>
      <thead>
        <tr className="border-b border-ink text-caption text-ink-tertiary">
          <th scope="col" className="pb-2 font-normal">
            Measure
          </th>
          <th scope="col" className="px-3 pb-2 text-right font-normal">
            DSATUR
          </th>
          <th scope="col" className="pb-2 pl-3 text-right font-normal">
            First-fit
          </th>
        </tr>
      </thead>
      <tbody className="divide-y divide-line">
        {rows.map(([label, value]) => (
          <tr key={label}>
            <th scope="row" className="py-3 font-normal text-ink-secondary">
              {label}
            </th>
            <td data-numeric className="px-3 py-3 text-right font-medium text-ink">
              {value(data.dsatur)}
            </td>
            <td data-numeric className="py-3 pl-3 text-right text-ink-secondary">
              {value(data.firstFit)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function PlacementOrder({ trace, entries }) {
  const byExam = new Map(entries.map((entry) => [entry.examinationId, entry]))
  const steps = trace.filter((step) => byExam.has(step.examId))

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-callout">
        <caption className="sr-only">The order papers were placed in</caption>
        <thead>
          <tr className="border-b border-ink text-caption text-ink-tertiary">
            {['Order', 'Paper', 'Students', 'Clashes with', 'Slots its clashes held', 'Slots still open', 'Placed on'].map((header, index) => (
              <th key={header} scope="col" className={`px-3 pb-2 font-normal first:pl-0 last:pr-0 ${index > 1 && index < 6 ? 'text-right' : ''}`}>
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {steps.map((step, index) => {
            const entry = byExam.get(step.examId)
            return (
              <tr key={step.examId}>
                <td data-numeric className="py-3 pr-3 text-ink-tertiary">
                  {index + 1}
                </td>
                <td className="px-3 py-3">
                  <span className="font-mono text-caption text-ink-tertiary">{entry.subjectCode}</span>{' '}
                  <span className="font-medium text-ink">{entry.subjectName}</span>
                </td>
                <td data-numeric className="px-3 py-3 text-right text-ink-secondary">
                  {entry.students}
                </td>
                <td data-numeric className="px-3 py-3 text-right text-ink-secondary">
                  {step.conflicts} {step.conflicts === 1 ? 'paper' : 'papers'}
                </td>
                <td data-numeric className="px-3 py-3 text-right text-ink-secondary">
                  {step.saturation}
                </td>
                <td data-numeric className="px-3 py-3 text-right text-ink-secondary">
                  {step.legalSlots}
                </td>
                <td className="py-3 pl-3 text-ink-secondary">
                  {formatDay(entry.date)}, <span className="font-mono text-caption">{formatTime(entry.start)}</span>
                  {step.slotId !== entry.slotId && <span className="block text-footnote text-ink-tertiary">Moved afterwards to spread papers</span>}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

// The engine's reasoning laid out for a person: what clashes, what was placed when and why, and how good the result is.
export default function ExplainedTab() {
  const { session } = useOutletContext()
  const [params] = useSearchParams()
  const versions = useApi(useCallback(() => listVersions(session.id), [session.id]))
  const version = versions.data?.find((candidate) => String(candidate.id) === params.get('version')) ?? versions.data?.[0]
  const versionId = version?.id
  const entries = useApi(useCallback(() => (versionId ? listEntries(versionId) : Promise.resolve(null)), [versionId]))

  if (versions.error) return <ErrorState message={versions.error.message} onRetry={versions.reload} />
  if (entries.error) return <ErrorState message={entries.error.message} onRetry={entries.reload} />
  if (!versions.data || (versionId && !entries.data)) return <Skeleton className="h-96 w-full rounded-lg" />

  const back = (
    <Link to={`../timetable${version ? `?version=${version.id}` : ''}`} className="sf-press mb-4 inline-flex items-center gap-1 rounded-sm text-footnote text-ink-secondary hover:text-ink">
      <ChevronLeft size={16} aria-hidden="true" />
      Timetable
    </Link>
  )
  if (!version) {
    return (
      <>
        {back}
        <p className="text-callout text-ink-secondary">Generate a timetable first. How it was built appears here.</p>
      </>
    )
  }

  return (
    <>
      {back}
      <h2 className="text-title-1">How version {version.versionNo} was built</h2>
      <p data-numeric className="mt-1 text-footnote text-ink-tertiary">
        DSATUR with backtracking · {formatNumber(version.searchNodes ?? 0)} search steps · backed up {formatNumber(version.backtracks ?? 0)} times ·{' '}
        {version.durationMs ?? 0} ms · {version.examsScheduled} of {version.examsTotal} papers placed in {version.slotsUsed} slots
      </p>

      <dl className="mt-6 border-t border-line">
        {IDEA.map(([term, meaning]) => (
          <div key={term} className="grid gap-x-6 gap-y-0.5 border-b border-line py-3 sm:grid-cols-[6rem_minmax(0,1fr)]">
            <dt className="text-callout font-medium text-ink">{term}</dt>
            <dd className="text-callout text-ink-secondary">{meaning}</dd>
          </div>
        ))}
      </dl>

      {version.quality && (
        <Section id="explained-quality" title="How good it is" intro="Every rule is kept. These measure what is left to choose: comfort for students, fairness for staff, and use of rooms.">
          <Quality quality={version.quality} />
        </Section>
      )}

      <Section
        id="explained-baseline"
        title="Against a simpler method"
        intro="First-fit takes papers in the order they were entered and gives each the earliest slot that keeps the rules. Both are run now on this session’s current papers and slots. Nothing is saved."
      >
        <Baseline sessionId={session.id} />
      </Section>

      <Section id="explained-clashes" title="What clashes with what" intro="One column for each slot in use.">
        <ClashPicture entries={entries.data} />
      </Section>

      <Section
        id="explained-order"
        title="The order papers were placed in"
        intro="A paper rises to the front as its clashing papers take up slots it can no longer use."
      >
        {version.trace.length > 0 ? (
          <PlacementOrder trace={version.trace} entries={entries.data} />
        ) : (
          <p className="text-callout text-ink-secondary">This version was generated before the order was recorded. Generate again to see it.</p>
        )}
      </Section>

      <p className="mt-12 text-footnote text-ink-secondary">
        <Link to="/comparison" className="rounded-sm underline decoration-line-strong underline-offset-2 hover:text-ink">
          The same two methods on three fixed test datasets
        </Link>
      </p>
    </>
  )
}
