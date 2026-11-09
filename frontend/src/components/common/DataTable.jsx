import { ChevronDown, ChevronsUpDown, ChevronUp } from 'lucide-react'
import { useMediaQuery } from '../../hooks/useMediaQuery.js'
import EmptyState from './EmptyState.jsx'
import ErrorState from './ErrorState.jsx'
import Skeleton from './Skeleton.jsx'

const LOADING_ROWS = 5

const cellValue = (column, row) => (column.render ? column.render(row) : row[column.key])

const parseSort = (sort = '') => {
  const [key, direction = 'asc'] = sort.split(',')
  return { key, direction }
}

function SortButton({ column, sort, onSort }) {
  const { key, direction } = parseSort(sort)
  const active = key === column.key
  const Icon = !active ? ChevronsUpDown : direction === 'asc' ? ChevronUp : ChevronDown

  return (
    <button
      type="button"
      onClick={() => onSort(`${column.key},${active && direction === 'asc' ? 'desc' : 'asc'}`)}
      className="group inline-flex items-center gap-1 rounded-sm hover:text-ink"
    >
      {column.header}
      <Icon size={14} aria-hidden="true" className={active ? '' : 'opacity-0 transition-opacity duration-(--duration-fast) group-hover:opacity-100 group-focus-visible:opacity-100'} />
    </button>
  )
}

function ariaSort(column, sort) {
  if (!column.sortable) return undefined
  const { key, direction } = parseSort(sort)
  if (key !== column.key) return 'none'
  return direction === 'asc' ? 'ascending' : 'descending'
}

function Table({ caption, columns, rows, rowKey, loading, sort, onSort, actions, inactiveRow }) {
  const hideOnTablet = (column) => (column.priority === 'low' ? 'hidden lg:table-cell' : '')

  return (
    <div className="overflow-x-auto">
      <table aria-busy={loading || undefined} className="w-full border-collapse text-left text-callout">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr className="border-b border-ink text-caption text-ink-tertiary">
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                aria-sort={ariaSort(column, sort)}
                className={`px-3 pb-2 font-normal ${column.numeric ? 'text-right' : ''} ${hideOnTablet(column)}`}
              >
                {column.sortable ? <SortButton column={column} sort={sort} onSort={onSort} /> : column.header}
              </th>
            ))}
            {actions && (
              <th scope="col" className="w-12 px-2 pb-2">
                <span className="sr-only">Actions</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {rows.map((row) => (
            <tr
              key={row[rowKey]}
              className={inactiveRow(row) ? '' : 'transition-colors duration-(--duration-fast) hover:bg-sunken'}
            >
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`px-3 py-3 ${column.numeric ? 'text-right' : ''} ${column.mono ? 'font-mono text-caption' : ''} ${column.primary ? 'font-medium text-ink' : 'text-ink-secondary'} ${hideOnTablet(column)}`}
                >
                  {cellValue(column, row)}
                </td>
              ))}
              {actions && <td className="px-2 py-1 text-right">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// Designed for phones rather than shrunk: one line that identifies the record, then its key facts.
function CardList({ caption, columns, rows, rowKey, actions }) {
  const primary = columns.find((column) => column.primary) ?? columns[0]
  const facts = columns.filter((column) => column !== primary && column.priority !== 'low')

  return (
    <ul aria-label={caption} className="divide-y divide-line">
      {rows.map((row) => (
        <li key={row[rowKey]} className="flex items-start gap-2 py-3">
          <div className="min-w-0 flex-1">
            <p className="text-body font-medium text-ink">{cellValue(primary, row)}</p>
            <dl className="mt-1.5 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-footnote">
              {facts.map((column) => (
                <div key={column.key} className="col-span-2 grid grid-cols-subgrid items-center">
                  <dt className="text-ink-tertiary">{column.header}</dt>
                  <dd className={`min-w-0 text-ink-secondary ${column.mono ? 'font-mono text-caption' : ''}`}>{cellValue(column, row)}</dd>
                </div>
              ))}
            </dl>
          </div>
          {actions?.(row)}
        </li>
      ))}
    </ul>
  )
}

function LoadingRows() {
  return (
    <div role="status" aria-label="Loading" className="divide-y divide-line">
      {Array.from({ length: LOADING_ROWS }, (_, index) => (
        <div key={index} className="flex items-center gap-4 py-3.5">
          <Skeleton className="h-4 w-1/3" />
          <Skeleton className="h-4 w-1/4" />
          <Skeleton className="ml-auto h-4 w-16" />
        </div>
      ))}
    </div>
  )
}

export default function DataTable({
  caption,
  columns,
  rows = [],
  rowKey = 'id',
  loading = false,
  error = null,
  onRetry,
  empty,
  sort,
  onSort,
  actions,
  // Rows that cannot be worked on, such as deactivated accounts, get no hover highlight.
  inactiveRow = () => false,
}) {
  const isPhone = useMediaQuery('(max-width: 47.99rem)')

  if (error) return <ErrorState message={error.message} onRetry={onRetry} />
  if (loading && rows.length === 0) return <LoadingRows />
  if (rows.length === 0) return empty ?? <EmptyState title="Nothing here yet" />

  const shared = { caption, columns, rows, rowKey, actions }
  return isPhone ? <CardList {...shared} /> : <Table {...shared} loading={loading} sort={sort} onSort={onSort} inactiveRow={inactiveRow} />
}
