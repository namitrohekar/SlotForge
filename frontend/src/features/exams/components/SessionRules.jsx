const count = (number, one, many = `${one}s`) => `${number} ${number === 1 ? one : many}`

function mandatory(session) {
  return [
    'No student sits two papers at the same time',
    `No student sits more than ${count(session.maxExamsPerStudentPerDay, 'paper')} in one day`,
    session.heavyGapDays
      ? `A student’s heavy papers have at least ${count(session.heavyGapDays, 'rest day')} between them`
      : 'Heavy papers need no rest day between them',
    'A paper goes only in an open slot that is long enough and inside its allowed dates',
    'A hall holds one paper at a time, and never more students than its exam seats',
    'Every enrolled student has a seat',
    `Each hall has one invigilator for every ${count(session.studentsPerInvigilator, 'student')}, and at least one`,
    `Nobody invigilates two halls at once, on a date they are away, or more than ${count(session.maxDutiesPerInvigilator, 'duty', 'duties')}`,
    'A hall, invigilator or student busy in another published session is not used at the same time',
  ]
}

const PREFERENCES = [
  'Keep each student’s papers as far apart as the dates allow',
  'Share duties evenly among invigilators',
  'Fill the halls in use rather than open more',
]

// The guide's ask in one place: what must hold, and what is only preferred, with this session's own numbers.
export default function SessionRules({ session }) {
  return (
    <details className="mb-6 border-y border-line">
      <summary className="cursor-pointer py-3 text-callout font-medium text-ink -outline-offset-2">Rules for this session</summary>
      <div className="grid gap-x-10 gap-y-6 pb-5 md:grid-cols-2">
        <section aria-labelledby="rules-mandatory">
          <h3 id="rules-mandatory" className="text-footnote text-ink-tertiary">
            Mandatory. A timetable that breaks one cannot be approved
          </h3>
          <ol className="mt-2 list-decimal space-y-1 pl-5 text-callout text-ink-secondary">
            {mandatory(session).map((rule) => (
              <li key={rule}>{rule}</li>
            ))}
          </ol>
        </section>
        <section aria-labelledby="rules-preferred">
          <h3 id="rules-preferred" className="text-footnote text-ink-tertiary">
            Preferred. Used to choose between timetables that keep every rule
          </h3>
          <ol className="mt-2 list-decimal space-y-1 pl-5 text-callout text-ink-secondary">
            {PREFERENCES.map((rule) => (
              <li key={rule}>{rule}</li>
            ))}
          </ol>
        </section>
      </div>
    </details>
  )
}
