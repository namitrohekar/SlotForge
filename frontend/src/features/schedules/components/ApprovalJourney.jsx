import { formatDateTime } from '../../../utils/format.js'
import { approverLabel } from '../../exams/sessionSteps.js'

const COORDINATOR = 'Exam coordinator'

function stagesOf(version, progress) {
  const sentAt = version.submittedAt
  return [
    { label: 'Generated', at: version.createdAt, by: version.generatedBy, expected: COORDINATOR },
    { label: 'Sent for approval', at: sentAt, by: version.sentBy, expected: COORDINATOR },
    { label: 'Approved', at: version.reviewedAt, by: version.reviewedBy, expected: approverLabel(progress) },
    { label: 'Published', at: version.publishedAt, by: version.publishedBy, expected: COORDINATOR },
    { label: 'Invigilators told', at: version.publishedAt, by: `${version.invigilatorCount} on My duties`, expected: 'When published' },
  ]
}

// The whole approval on one line: who did each step, when, and whose turn it is.
export default function ApprovalJourney({ version, progress }) {
  const stages = stagesOf(version, progress)
  const current = stages.findIndex((stage) => !stage.at)

  return (
    <ol aria-label="Approval journey" className="mb-6 grid border-y border-line sm:grid-cols-5">
      {stages.map((stage, index) => (
        <li
          key={stage.label}
          className={`min-w-0 border-line py-3 not-first:border-t sm:px-3 sm:first:pl-0 sm:not-first:border-t-0 sm:not-first:border-l ${
            index === current ? 'shadow-[inset_0_-2px_0_var(--sf-tick)]' : ''
          }`}
        >
          <span className={`block text-callout font-medium ${stage.at || index === current ? 'text-ink' : 'text-ink-tertiary'}`}>{stage.label}</span>
          <span className="block text-footnote text-ink-secondary">{stage.at ? stage.by : stage.expected}</span>
          {stage.at && <span className="block font-mono text-caption text-ink-tertiary">{formatDateTime(stage.at)}</span>}
        </li>
      ))}
    </ol>
  )
}
