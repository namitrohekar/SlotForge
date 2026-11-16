const REASONS = {
  SLOT_CLOSED: 'blocked or already past',
  DURATION: 'too short for this paper',
  WINDOW: 'outside its date window',
  STUDENT_CLASH: 'a student already has a paper',
  DAILY_LIMIT: 'a student is at the daily limit',
  HEAVY_GAP: 'too close to a student’s other heavy paper',
  CAPACITY: 'not enough hall seats',
  STAFFING: 'not enough invigilators free',
}

const plural = (count) => `${count} ${count === 1 ? 'slot' : 'slots'}`

// Says exactly what stopped each paper, so the coordinator knows what to change.
export default function UnplacedPanel({ unplaced }) {
  if (unplaced.length === 0) return null

  return (
    <section aria-labelledby="unplaced-heading" className="mb-8 border-y border-line">
      <h3 id="unplaced-heading" className="border-b border-line py-3 text-title-3">
        {unplaced.length === 1 ? '1 paper could not be placed' : `${unplaced.length} papers could not be placed`}
      </h3>
      <ul>
        {unplaced.map((paper) => (
          <li key={paper.examId} className="border-b border-line py-3 last:border-b-0">
            <p className="text-callout font-medium text-ink">{paper.code}</p>
            {paper.blockers.length === 0 ? (
              <p className="mt-0.5 text-footnote text-ink-secondary">The session has no slots to try</p>
            ) : (
              <ul className="mt-0.5">
                {paper.blockers.map((blocker) => (
                  <li key={blocker.constraint} className="text-footnote text-ink-secondary">
                    {plural(blocker.slots)}: {REASONS[blocker.constraint] ?? blocker.constraint}
                    {blocker.example ? ` (${blocker.example})` : ''}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
