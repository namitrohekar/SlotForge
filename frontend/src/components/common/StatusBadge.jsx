import {
  Archive,
  BadgeCheck,
  Ban,
  CircleCheck,
  CircleDashed,
  CircleX,
  Clock,
  Globe,
  LoaderCircle,
  PencilLine,
  TriangleAlert,
  UserX,
  Wrench,
} from 'lucide-react'
import Badge from './Badge.jsx'

// Every status carries an icon and text, so meaning never depends on colour alone.
const statuses = {
  ACTIVE: { label: 'Active', tone: 'success', icon: CircleCheck },
  DEACTIVATED: { label: 'Deactivated', tone: 'neutral', icon: Ban },
  AVAILABLE: { label: 'Available', tone: 'success', icon: CircleCheck },
  MAINTENANCE: { label: 'Maintenance', tone: 'warning', icon: Wrench },
  RETIRED: { label: 'Retired', tone: 'neutral', icon: Archive },
  PLANNING: { label: 'Planning', tone: 'info', icon: Clock },
  OPEN: { label: 'Open', tone: 'success', icon: CircleCheck },
  BLOCKED: { label: 'Blocked', tone: 'danger', icon: Ban },
  READY: { label: 'Ready', tone: 'success', icon: CircleCheck },
  COMPLETED: { label: 'Completed', tone: 'neutral', icon: BadgeCheck },
  CANCELLED: { label: 'Cancelled', tone: 'neutral', icon: Ban },
  DRAFT: { label: 'Draft', tone: 'neutral', icon: PencilLine },
  NO_STUDENTS: { label: 'No students', tone: 'warning', icon: UserX },
  REVIEWED: { label: 'Approved', tone: 'info', icon: BadgeCheck },
  PUBLISHED: { label: 'Published', tone: 'success', icon: Globe },
  SUPERSEDED: { label: 'Superseded', tone: 'neutral', icon: Archive },
  RUNNING: { label: 'Running', tone: 'info', icon: LoaderCircle },
  SUCCEEDED: { label: 'Succeeded', tone: 'success', icon: CircleCheck },
  PARTIAL: { label: 'Partial', tone: 'warning', icon: TriangleAlert },
  FAILED: { label: 'Failed', tone: 'danger', icon: CircleX },
}

export default function StatusBadge({ status }) {
  const { label, tone, icon } = statuses[status] ?? { label: status, tone: 'neutral', icon: CircleDashed }
  return (
    <Badge tone={tone} icon={icon}>
      {label}
    </Badge>
  )
}
