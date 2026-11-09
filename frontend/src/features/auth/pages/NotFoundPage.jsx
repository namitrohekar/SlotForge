import { Compass } from 'lucide-react'
import { Link } from 'react-router'
import EmptyState from '../../../components/common/EmptyState.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'

export default function NotFoundPage() {
  return (
    <>
      <PageHeader title="Page not found" />
      <EmptyState
        icon={Compass}
        title="Nothing lives at this address"
        description="The link may be old, or the address mistyped"
        action={<Link to="/" className="text-callout font-medium text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">Go to the dashboard</Link>}
      />
    </>
  )
}
