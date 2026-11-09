import { useState } from 'react'
import { Lock, MapPin, PencilLine, Plus, SlidersHorizontal, Split, Trash2, Users } from 'lucide-react'
import Alert from '../../../components/common/Alert.jsx'
import Badge from '../../../components/common/Badge.jsx'
import Button from '../../../components/common/Button.jsx'
import Checkbox from '../../../components/common/Checkbox.jsx'
import ConfirmDialog from '../../../components/common/ConfirmDialog.jsx'
import DataTable from '../../../components/common/DataTable.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import Dropdown from '../../../components/common/Dropdown.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import FilterSheet from '../../../components/common/FilterSheet.jsx'
import FormField from '../../../components/common/FormField.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Input from '../../../components/common/Input.jsx'
import Meter from '../../../components/common/Meter.jsx'
import Pagination from '../../../components/common/Pagination.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import SegmentedControl from '../../../components/common/SegmentedControl.jsx'
import Select from '../../../components/common/Select.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import Spinner from '../../../components/common/Spinner.jsx'
import StatTile from '../../../components/common/StatTile.jsx'
import StatusBadge from '../../../components/common/StatusBadge.jsx'
import Switch from '../../../components/common/Switch.jsx'
import Tabs from '../../../components/common/Tabs.jsx'
import Textarea from '../../../components/common/Textarea.jsx'
import Tooltip from '../../../components/common/Tooltip.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useToastStore } from '../../../store/toastStore.js'

const statuses = ['DRAFT', 'REVIEWED', 'PUBLISHED', 'SUPERSEDED', 'AVAILABLE', 'MAINTENANCE', 'BLOCKED', 'PARTIAL', 'FAILED']

const type = [
  ['Display', 'text-display', '28 to 34'],
  ['Title 1', 'text-title-1', '24 to 28'],
  ['Title 2', 'text-title-2', '20 to 22'],
  ['Title 3', 'text-title-3', '17'],
  ['Body', 'text-body', '15'],
  ['Callout', 'text-callout', '14'],
  ['Footnote', 'text-footnote', '13'],
  ['Caption', 'text-caption', '12'],
]

const halls = [
  { id: 1, code: 'CH-101', name: 'Central Hall 101', building: 'Main', capacity: 60, status: 'AVAILABLE' },
  { id: 2, code: 'CH-204', name: 'Central Hall 204', building: 'Main', capacity: 120, status: 'AVAILABLE' },
  { id: 3, code: 'LB-12', name: 'Library Annexe 12', building: 'Library', capacity: 45, status: 'MAINTENANCE' },
]

const columns = [
  { key: 'code', header: 'Code', primary: true, sortable: true },
  { key: 'name', header: 'Name', sortable: true },
  { key: 'building', header: 'Building', priority: 'low' },
  { key: 'capacity', header: 'Capacity', numeric: true, sortable: true },
  { key: 'status', header: 'Status', render: (hall) => <StatusBadge status={hall.status} /> },
]

function Section({ title, note, children }) {
  return (
    <section className="grid gap-x-10 gap-y-4 border-t border-line pt-5 lg:grid-cols-[11rem_minmax(0,1fr)]">
      <div>
        <h2 className="text-title-3">{title}</h2>
        {note && <p className="mt-1 text-footnote text-ink-tertiary">{note}</p>}
      </div>
      <div className="flex min-w-0 flex-col gap-5">{children}</div>
    </section>
  )
}

function Row({ children }) {
  return <div className="flex flex-wrap items-center gap-3">{children}</div>
}

// Both themes side by side, each scoped with data-theme, so this is the real thing in either mode.
function ThemePair() {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {['light', 'dark'].map((theme) => (
        <div key={theme} data-theme={theme} className="rounded-lg border border-line bg-canvas p-3 text-ink">
          <div className="rounded-md border border-line bg-surface p-4">
            <p className="text-footnote text-ink-tertiary">{theme === 'light' ? 'Light' : 'Dark'}</p>
            <p className="mt-1 text-title-2">MCA301</p>
            <p className="text-callout text-ink-secondary">Monday, morning</p>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <Button>Publish</Button>
              <StatusBadge status="REVIEWED" />
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

export default function DesignGalleryPage() {
  const push = useToastStore((state) => state.push)
  const [dialog, setDialog] = useState(null)
  const [tab, setTab] = useState('slots')
  const [view, setView] = useState('grid')
  const [sort, setSort] = useState('code,asc')
  const [notify, setNotify] = useState(true)
  const close = () => setDialog(null)

  return (
    <>
      <PageHeader title="Design system" />

      <div className="flex flex-col gap-10">
        <Section title="Themes" note="Elevation comes from lighter surfaces in dark.">
          <ThemePair />
        </Section>

        <Section title="Type" note="Serif names things. Sans does the work.">
          <div>
            {type.map(([name, className, size]) => (
              <div key={name} className="flex items-baseline justify-between gap-4 border-b border-line py-2">
                <p className={`min-w-0 ${className}`}>{name}</p>
                <p data-numeric className="shrink-0 text-footnote text-ink-tertiary">
                  {size}
                </p>
              </div>
            ))}
          </div>
        </Section>

        <Section title="Press" note="Answers on pointer-down, and springs back.">
          <Row>
            <Button icon={Plus}>Create exam</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="quiet">Quiet</Button>
            <Button variant="danger" icon={Trash2}>
              Delete
            </Button>
          </Row>
          <Row>
            <Button loading>Saving</Button>
            <Button disabled>Disabled</Button>
            <IconButton label="Filters" icon={SlidersHorizontal} />
            <Tooltip text="Add to the session">
              <IconButton label="Add" icon={Plus} />
            </Tooltip>
            <Dropdown
              label="Row actions"
              items={[
                { label: 'Edit', icon: PencilLine, onSelect: () => push({ message: 'Edit chosen' }) },
                { label: 'Lock', icon: Lock, onSelect: () => push({ message: 'Lock chosen' }) },
                { label: 'Delete', icon: Trash2, danger: true, onSelect: () => setDialog('confirm') },
              ]}
            />
          </Row>
        </Section>

        <Section title="Movement" note="Interrupt these mid-way. They continue from where they are.">
          <SegmentedControl
            label="View"
            value={view}
            onChange={setView}
            options={[
              { value: 'grid', label: 'Grid' },
              { value: 'list', label: 'List' },
              { value: 'agenda', label: 'Agenda' },
            ]}
          />
          <Tabs
            label="Exam session"
            value={tab}
            onChange={setTab}
            tabs={[
              { id: 'slots', label: 'Time slots' },
              { id: 'exams', label: 'Examinations' },
              { id: 'enrolment', label: 'Enrolment' },
              { id: 'conflicts', label: 'Conflicts' },
            ]}
          >
            <p className="text-callout text-ink-secondary">Selected: {tab}</p>
          </Tabs>
        </Section>

        <Section title="Overlays" note="On a phone, drag a sheet down to dismiss it.">
          <Row>
            <Button variant="secondary" onClick={() => setDialog('form')}>
              Open dialog
            </Button>
            <Button variant="secondary" onClick={() => setDialog('confirm')}>
              Open confirmation
            </Button>
            <Button variant="secondary" onClick={() => setDialog('filters')}>
              Open filters
            </Button>
          </Row>
        </Section>

        <Section title="Status" note="Icon and text, never colour alone.">
          <Row>
            {statuses.map((status) => (
              <StatusBadge key={status} status={status} />
            ))}
          </Row>
          <Row>
            <Badge icon={MapPin}>Hall A, 60 seats</Badge>
            <Badge icon={Split}>3 halls</Badge>
            <Badge icon={Users}>3 of 3 invigilators</Badge>
            <Badge tone="info" icon={PencilLine}>
              Manual
            </Badge>
            <Badge icon={Lock}>Locked</Badge>
          </Row>
        </Section>

        <Section title="Forms">
          <form noValidate className="grid gap-4 md:grid-cols-2">
            <FormField label="Hall name" required hint="As shown on the door">
              <Input defaultValue="Central Hall 101" />
            </FormField>
            <FormField label="Capacity" required error="Capacity must be greater than 0">
              <Input type="number" inputMode="numeric" defaultValue="0" />
            </FormField>
            <FormField label="Building">
              <Select defaultValue="main">
                <option value="main">Main building</option>
                <option value="library">Library</option>
              </Select>
            </FormField>
            <FormField label="Disabled">
              <Input disabled value="Cannot be changed" readOnly />
            </FormField>
            <div className="md:col-span-2">
              <FormField label="Notes">
                <Textarea placeholder="Anything the coordinator should know" />
              </FormField>
            </div>
            <Row>
              <Checkbox label="Can invigilate" defaultChecked />
              <Switch label="Notify me" checked={notify} onChange={(event) => setNotify(event.target.checked)} />
            </Row>
          </form>
        </Section>

        <Section title="Feedback">
          <Alert tone="info" title="Preflight found two warnings">
            Scheduling can still run.
          </Alert>
          <Alert tone="success" title="Timetable published" />
          <Alert tone="warning" title="1 invigilator short">
            Central Hall 204, Monday morning.
          </Alert>
          <Alert tone="danger" title="Clash: 12 students">
            MCA301 and CS204 share a slot.
          </Alert>
          <Row>
            <Button variant="secondary" onClick={() => push({ message: 'Hall saved' })}>
              Show success toast
            </Button>
            <Button variant="secondary" onClick={() => push({ tone: 'danger', message: 'The hall could not be saved' })}>
              Show error toast
            </Button>
          </Row>
        </Section>

        <Section title="Figures" note="Sample values, for this page only.">
          <div className="grid grid-cols-2 gap-x-4 gap-y-6 lg:grid-cols-4">
            <StatTile label="Examinations" value="41" context="1 unscheduled" />
            <StatTile label="Students" value="1,284" />
            <StatTile label="Halls" value="12" context="1 in maintenance" />
            <StatTile label="Quality score" value="78.4" />
          </div>
          <div className="flex flex-col gap-4">
            <Meter label="Central Hall 204" value={92} />
            <Meter label="Central Hall 101" value={64} />
          </div>
        </Section>

        <Section title="Table" note="Becomes a card list on phones.">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="sm:w-72">
              <SearchInput label="Search halls" onSearch={() => {}} />
            </div>
          </div>
          <div className="overflow-hidden rounded-lg border border-line">
            <DataTable
              caption="Examination halls"
              columns={columns}
              rows={halls}
              sort={sort}
              onSort={setSort}
              actions={(hall) => (
                <Dropdown
                  label={`Actions for ${hall.code}`}
                  items={[{ label: 'Edit', icon: PencilLine, onSelect: () => {} }]}
                />
              )}
            />
          </div>
          <Pagination page={0} totalPages={3} totalItems={48} onChange={() => {}} />
        </Section>

        <Section title="States" note="Every view handles all three.">
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="overflow-hidden rounded-lg border border-line">
              <DataTable caption="Loading" columns={columns} loading />
            </div>
            <EmptyState
              title="No halls yet"
              description="Add a hall to start allocating examinations."
              action={<Button icon={Plus}>Add hall</Button>}
            />
            <ErrorState message="The database is not available right now." onRetry={() => {}} />
          </div>
          <Row>
            <span className="inline-flex items-center gap-2 text-callout text-ink-secondary">
              <Spinner size={16} /> Loading
            </span>
            <Skeleton className="h-4 w-32" />
          </Row>
        </Section>
      </div>

      <Dialog
        open={dialog === 'form'}
        onClose={close}
        title="Add hall"
        description="Halls can be used as soon as they are saved."
        footer={
          <>
            <Button variant="secondary" onClick={close}>
              Cancel
            </Button>
            <Button onClick={close}>Save hall</Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <FormField label="Hall name" required>
            <Input />
          </FormField>
          <FormField label="Capacity" required>
            <Input type="number" inputMode="numeric" />
          </FormField>
        </div>
      </Dialog>

      <ConfirmDialog
        open={dialog === 'confirm'}
        onClose={close}
        onConfirm={close}
        title="Delete this hall?"
        message="Central Hall 101 will be removed. This cannot be undone."
        confirmLabel="Delete hall"
      />

      <FilterSheet open={dialog === 'filters'} onClose={close}>
        <FormField label="Status">
          <Select defaultValue="">
            <option value="">Any status</option>
            <option value="AVAILABLE">Available</option>
          </Select>
        </FormField>
      </FilterSheet>
    </>
  )
}
