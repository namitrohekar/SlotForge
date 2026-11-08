import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatClock } from '../../../utils/format.js'
import { fetchHealth } from '../services/systemApi.js'

const loadHealth = () => fetchHealth().then((health) => ({ ...health, checkedAt: new Date() }))

const layout = 'grid gap-x-16 gap-y-8 lg:grid-cols-[minmax(0,20rem)_minmax(0,1fr)]'

function Fact({ label, value, note }) {
  return (
    <div className="flex items-baseline justify-between gap-6 border-b border-line py-3.5">
      <dt className="shrink-0 text-callout text-ink-secondary">{label}</dt>
      <dd className="min-w-0 text-right">
        <span data-numeric className="block text-body font-medium text-ink">
          {value}
        </span>
        {note && <span className="block text-footnote text-ink-tertiary">{note}</span>}
      </dd>
    </div>
  )
}

function Status({ health, checking, onCheck }) {
  const behind = health.schemaVersion < health.requiredSchemaVersion

  return (
    <div className="flex flex-col gap-8">
      {behind && (
        <Alert tone="danger" title="The database is behind the code">
          Schema version {health.schemaVersion} is applied, but this code needs {health.requiredSchemaVersion}.
        </Alert>
      )}
      <div className={layout}>
        <section aria-label="Status">
          <p className="flex items-center gap-2.5 font-serif text-title-1 font-medium">
            <span aria-hidden="true" className="size-2 shrink-0 rounded-full bg-success" />
            {health.status === 'UP' ? 'Connected' : health.status}
          </p>
          <p className="mt-1 text-footnote text-ink-tertiary">
            Checked at <time data-numeric>{formatClock(health.checkedAt)}</time>
          </p>
          <Button variant="secondary" loading={checking} onClick={onCheck} className="mt-5">
            Check again
          </Button>
        </section>

        <dl className="border-t border-line">
          <Fact label="Database" value={health.database} />
          <Fact label="PostgreSQL" value={health.postgresVersion} />
          <Fact
            label="Schema version"
            value={health.schemaVersion}
            note={`Code needs ${health.requiredSchemaVersion}`}
          />
        </dl>
      </div>
    </div>
  )
}

function Loading() {
  return (
    <div role="status" aria-label="Loading" className={layout}>
      <div>
        <Skeleton className="h-8 w-40" />
        <Skeleton className="mt-2 h-4.5 w-32" />
      </div>
      <div className="flex flex-col gap-4 border-t border-line pt-4">
        {[1, 2, 3].map((row) => (
          <Skeleton key={row} className="h-5 w-full" />
        ))}
      </div>
    </div>
  )
}

export default function SystemStatusPage() {
  const { data, error, loading, reload } = useApi(loadHealth)

  return (
    <>
      <PageHeader title="System" />
      {error && <ErrorState title="The system is not responding" message={error.message} onRetry={reload} />}
      {!error && !data && <Loading />}
      {!error && data && <Status health={data} checking={loading} onCheck={reload} />}
    </>
  )
}
