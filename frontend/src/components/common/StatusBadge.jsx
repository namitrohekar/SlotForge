import Badge from './Badge.jsx'

// A normal state stays plain text. Only something that needs a person to act gets a dot.
const statuses = {
  ACTIVE: { label: 'Active' },
  DEACTIVATED: { label: 'Deactivated' },
  AVAILABLE: { label: 'Available' },
  MAINTENANCE: { label: 'Maintenance', tone: 'warning' },
  RETIRED: { label: 'Retired' },
  PLANNING: { label: 'Planning' },
  OPEN: { label: 'Open' },
  BLOCKED: { label: 'Blocked', tone: 'danger' },
  READY: { label: 'Ready' },
  COMPLETED: { label: 'Completed' },
  CANCELLED: { label: 'Cancelled' },
  DRAFT: { label: 'Draft' },
  NO_STUDENTS: { label: 'No students', tone: 'warning' },
  REVIEWED: { label: 'Approved' },
  PUBLISHED: { label: 'Published' },
  SUPERSEDED: { label: 'Superseded' },
  RUNNING: { label: 'Running' },
  SUCCEEDED: { label: 'Succeeded' },
  PARTIAL: { label: 'Partial', tone: 'warning' },
  FAILED: { label: 'Failed', tone: 'danger' },
}

export default function StatusBadge({ status }) {
  const { label, tone } = statuses[status] ?? { label: status }
  return <Badge tone={tone}>{label}</Badge>
}
