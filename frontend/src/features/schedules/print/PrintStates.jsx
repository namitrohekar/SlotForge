import ErrorState from '../../../components/common/ErrorState.jsx'
import PageLoading from '../../../components/common/PageLoading.jsx'

/** Loading, failed or missing. Returns null once there is something to print. */
export default function PrintStates({ data, error, reload, missing }) {
  if (error) return <ErrorState message={error.message} onRetry={reload} />
  if (!data) return <PageLoading />
  if (missing) return <ErrorState title="Nothing to print" message="This timetable is no longer available." />
  return null
}
