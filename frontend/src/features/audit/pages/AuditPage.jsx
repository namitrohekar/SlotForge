import { ScrollText } from 'lucide-react'
import DataTable from '../../../components/common/DataTable.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { usePagedList } from '../../../hooks/usePagedList.js'
import { formatDateTime } from '../../../utils/format.js'
import { listAuditLogs } from '../services/auditApi.js'

// "SCHEDULE_GENERATED" reads as "Schedule generated".
const sentence = (code) => code.charAt(0).toUpperCase() + code.slice(1).toLowerCase().replaceAll('_', ' ')

const columns = [
  { key: 'at', header: 'When', mono: true, render: (entry) => formatDateTime(entry.at) },
  { key: 'action', header: 'What', primary: true, render: (entry) => sentence(entry.action) },
  { key: 'actorName', header: 'Who', render: (entry) => entry.actorName ?? 'Removed account' },
  {
    key: 'entity',
    header: 'On',
    render: (entry) => `${sentence(entry.entityType)}${entry.entityId ? ` ${entry.entityId}` : ''}`,
  },
  { key: 'details', header: 'Details', mono: true, priority: 'low', render: (entry) => entry.details ?? '' },
]

// Who did what and when. It is written by the system alone and cannot be edited from anywhere in the app.
export default function AuditPage() {
  const { data, error, loading, reload, query, setFilter, setPage } = usePagedList(listAuditLogs)

  return (
    <>
      <PageHeader title="Audit log" description="Every change made through SlotForge, newest first. It cannot be edited" />
      <div className="mb-4">
        <SearchInput label="Search an action or a person" value={query.q} onSearch={(value) => setFilter('q', value)} />
      </div>
      <DataTable
        caption="Audit log"
        columns={columns}
        rows={data?.items}
        loading={loading}
        error={error}
        onRetry={reload}
        empty={<EmptyState icon={ScrollText} title={query.q ? 'Nothing matches' : 'Nothing recorded yet'} />}
      />
      {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onChange={setPage} />}
    </>
  )
}
