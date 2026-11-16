import { useEffect } from 'react'
import { Bell } from 'lucide-react'
import { Link } from 'react-router'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { formatDateTime } from '../../../utils/format.js'
import { fetchNotifications, markNotificationsRead } from '../services/notificationsApi.js'

// What has happened that concerns this person. Opening the page is what marks them read.
export default function NotificationsPage() {
  const { data, error, reload } = useApi(fetchNotifications)
  const unread = data?.unread ?? 0

  useEffect(() => {
    // The list already fetched keeps its "New" marks for this visit, so the person can see what arrived.
    if (unread > 0) markNotificationsRead().catch(() => {})
  }, [unread])

  return (
    <>
      <PageHeader title="Notifications" description="What has happened that concerns you" />
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {!data && !error && <Skeleton className="h-40 w-full rounded-lg" />}
      {data?.items.length === 0 && <EmptyState icon={Bell} title="Nothing yet" description="You are told here when a timetable needs you, or when you are given a duty" />}
      {data?.items.length > 0 && (
        <ul className="border-y border-line">
          {data.items.map((item) => (
            <li key={item.id} className="grid gap-x-6 gap-y-1 border-b border-line py-3 last:border-b-0 sm:grid-cols-[12rem_minmax(0,1fr)]">
              <p className="font-mono text-caption text-ink-tertiary">
                {formatDateTime(item.createdAt)}
                {!item.read && <span className="ml-2 font-sans text-footnote font-medium text-ink">New</span>}
              </p>
              <div className="min-w-0">
                {item.link ? (
                  <Link to={item.link} className="rounded-sm text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
                    {item.title}
                  </Link>
                ) : (
                  <p className="text-callout font-medium text-ink">{item.title}</p>
                )}
                {item.body && <p className="mt-0.5 text-footnote text-ink-secondary">{item.body}</p>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
