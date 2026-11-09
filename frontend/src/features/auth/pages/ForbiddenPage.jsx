import { Lock } from 'lucide-react'
import { Link } from 'react-router'
import EmptyState from '../../../components/common/EmptyState.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'

export default function ForbiddenPage() {
  return (
    <>
      <PageHeader title="No access" />
      <EmptyState
        icon={Lock}
        title="Your role cannot open this page"
        description="If you need it, ask an administrator to change your roles"
        action={<Link to="/" className="text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">Go to the dashboard</Link>}
      />
    </>
  )
}
