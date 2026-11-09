import { Check } from 'lucide-react'
import { formatDateTime } from '../../../utils/format.js'
import { approverLabel, STEP_MARKS } from '../../exams/sessionSteps.js'

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
    <ol aria-label="Approval journey" className="mb-4 grid gap-3 sm:grid-cols-5">
      {stages.map((stage, index) => {
        const state = stage.at ? 'done' : index === current ? 'current' : 'todo'
        return (
          <li key={stage.label} className="flex min-w-0 items-start gap-2.5">
            <span aria-hidden="true" data-numeric className={`grid size-6 shrink-0 place-items-center rounded-full text-caption font-medium ${STEP_MARKS[state]}`}>
              {stage.at ? <Check size={14} /> : index + 1}
            </span>
            <span className="min-w-0">
              <span className="block text-callout font-medium text-ink">{stage.label}</span>
              <span className="block text-footnote text-ink-secondary">{stage.at ? stage.by : stage.expected}</span>
              {stage.at && (
                <span data-numeric className="block text-footnote text-ink-tertiary">
                  {formatDateTime(stage.at)}
                </span>
              )}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
