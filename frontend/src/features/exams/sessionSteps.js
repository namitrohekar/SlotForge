import { formatDuration } from '../../utils/format.js'

/** "Dr Rao, Head of MCA", or just the post while it is vacant. */
export const approverLabel = ({ approverName, approverPost }) => (approverName ? `${approverName}, ${approverPost}` : approverPost)

function timetableStep(progress) {
  const { latestVersionNo: version, latestVersionStatus: status, latestPlaced: placed, latestTotal: total, inReviewStatus: inReview, latestProblems: problems } = progress
  if (version === 0) return { state: 'todo', detail: 'Not generated' }
  if (inReview === 'REVIEWED') return { state: 'current', detail: 'Approved, ready to publish' }
  if (inReview === 'DRAFT') return { state: 'current', detail: 'Waiting for approval' }
  if (status === 'PUBLISHED' || status === 'SUPERSEDED') return { state: 'done', detail: 'Published' }
  if (placed === total && problems > 0) return { state: 'attention', detail: `${problems} ${problems === 1 ? 'problem' : 'problems'} to fix` }
  return placed === total ? { state: 'current', detail: 'Ready to send' } : { state: 'attention', detail: `${placed} of ${total} placed` }
}

/** The three stages of a session, each saying how far it has got. */
export function sessionSteps(session, canSeeTimetable) {
  const { openSlots, readyPapers, papersWithoutStudents } = session.progress
  const steps = [
    { to: 'slots', label: 'Time slots', state: openSlots ? 'done' : 'todo', detail: openSlots ? `${openSlots} open` : 'None yet' },
    {
      to: 'examinations',
      label: 'Papers',
      state: !readyPapers ? 'todo' : papersWithoutStudents.length ? 'attention' : 'done',
      detail: !readyPapers
        ? 'None ready'
        : papersWithoutStudents.length
          ? `${readyPapers} ready, ${papersWithoutStudents.length} without students`
          : `${readyPapers} ready`,
    },
  ]
  return canSeeTimetable ? [...steps, { to: 'timetable', label: 'Timetable', ...timetableStep(session.progress) }] : steps
}

/** The one thing this person should do next, or null when it is someone else's move. */
export function nextStep(session, { isCoordinator, userId }) {
  if (session.status !== 'PLANNING') return null
  const { openSlots, slotsTooShort, shortestPaperMinutes, readyPapers, latestVersionNo, latestVersionStatus, latestPlaced, latestTotal, inReviewStatus, latestProblems, approverIds } = session.progress
  if (isCoordinator) {
    if (openSlots === 0) return { to: 'slots', message: 'Start by adding the time slots for this session', action: 'Go to time slots' }
    if (readyPapers === 0) return { to: 'examinations', message: 'Next, add the papers students will sit', action: 'Go to papers' }
    if (slotsTooShort === openSlots) {
      return { to: 'slots', message: `None of the open slots can hold a ${formatDuration(shortestPaperMinutes)} paper`, action: 'Go to time slots' }
    }
    if (latestVersionNo === 0) return { to: 'timetable', message: 'Slots and papers are ready. Generate the timetable.', action: 'Go to timetable' }
    if (inReviewStatus === 'REVIEWED') return { to: 'timetable', message: 'The timetable is approved and ready to publish', action: 'Go to timetable' }
    if (!inReviewStatus && latestVersionStatus === 'DRAFT' && latestPlaced === latestTotal) {
      return latestProblems > 0
        ? { to: 'timetable', message: 'The latest timetable has problems to fix before it can be sent', action: 'Go to timetable' }
        : { to: 'timetable', message: 'The timetable is ready to send for approval', action: 'Go to timetable' }
    }
    return null
  }
  if (inReviewStatus === 'DRAFT' && approverIds.includes(userId)) {
    return { to: 'timetable', message: 'A timetable is waiting for your approval', action: 'Review it' }
  }
  return null
}
