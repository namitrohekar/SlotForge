import { useId, useState } from 'react'
import { Plus, X } from 'lucide-react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Input from '../../../components/common/Input.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { todayIso } from '../../../utils/format.js'
import { generateSlots } from '../services/examsApi.js'
import { countRequestedSlots } from '../slotPreview.js'

const WEEKDAYS = [
  [1, 'Mon', 'Monday'],
  [2, 'Tue', 'Tuesday'],
  [3, 'Wed', 'Wednesday'],
  [4, 'Thu', 'Thursday'],
  [5, 'Fri', 'Friday'],
]

const EXAM_DAY_START = '09:30'
const EXAM_DAY_END = '17:00'

// Two three-hour papers a day, Monday to Friday. Weekends are always a gap.
const STANDARD_TIMES = [
  { start: '10:00', end: '13:00' },
  { start: '14:00', end: '17:00' },
]

function timesProblem(times) {
  if (times.some(({ start, end }) => !start || !end || end <= start)) return 'Each slot must end after it starts'
  if (times.some(({ start, end }) => start < EXAM_DAY_START || end > EXAM_DAY_END)) return 'Exams run between 9:30 am and 5:00 pm'
  const sorted = [...times].sort((a, b) => a.start.localeCompare(b.start))
  return sorted.some((time, index) => index > 0 && time.start < sorted[index - 1].end) ? 'These times overlap each other' : undefined
}

export default function GenerateSlotsDialog({ session, existing, onClose, onGenerated }) {
  const formId = useId()
  const [times, setTimes] = useState(STANDARD_TIMES)
  const today = todayIso()
  const firstDay = session.startDate > today ? session.startDate : today
  const { values, errors, formError, submitting, field, setValue, handleSubmit } = useForm({
    initial: { from: firstDay, to: session.endDate, weekdays: [1, 2, 3, 4, 5] },
    validate: (v) => ({
      from: v.from >= firstDay && v.from <= session.endDate ? undefined : 'Choose today or later, inside the session',
      to: v.to < v.from || v.to > session.endDate ? 'Choose a date inside the session' : undefined,
      weekdays: v.weekdays.length ? undefined : 'Choose at least one day',
      times: timesProblem(times),
    }),
    onSubmit: async (v) => onGenerated(await generateSlots(session.id, { from: v.from, to: v.to, weekdays: v.weekdays, times })),
  })

  const toggleDay = (day, checked) =>
    setValue('weekdays', checked ? [...values.weekdays, day] : values.weekdays.filter((held) => held !== day))
  const changeTime = (index, key, value) =>
    setTimes((current) => current.map((time, position) => (position === index ? { ...time, [key]: value } : time)))

  const requested = countRequestedSlots({ ...values, times, existing })

  return (
    <Dialog
      open
      onClose={onClose}
      title="Generate time slots"
      description="Slots that would overlap an existing one are skipped, so this is safe to run again"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting} disabled={requested === 0}>
            {requested === 0 ? 'Nothing new to add' : requested === 1 ? 'Add 1 slot' : `Add ${requested} slots`}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="From" required error={errors.from}>
            <Input {...field('from')} type="date" min={firstDay} max={session.endDate} />
          </FormField>
          <FormField label="To" required error={errors.to}>
            <Input {...field('to')} type="date" min={values.from || firstDay} max={session.endDate} />
          </FormField>
        </div>

        <fieldset>
          <legend className="text-footnote font-medium text-ink">Days, weekends are always a gap</legend>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {WEEKDAYS.map(([day, short, full]) => (
              <label
                key={day}
                className="sf-press flex h-control min-w-12 cursor-pointer items-center justify-center rounded-md border border-line px-2 text-callout text-ink-secondary [--press-scale:0.96] has-checked:border-accent has-checked:bg-accent-subtle has-checked:font-medium has-checked:text-ink has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-(--sf-focus)"
              >
                <input
                  type="checkbox"
                  aria-label={full}
                  checked={values.weekdays.includes(day)}
                  onChange={(event) => toggleDay(day, event.target.checked)}
                  className="sr-only"
                />
                <span aria-hidden="true">{short}</span>
              </label>
            ))}
          </div>
          {errors.weekdays && <p className="mt-1 text-footnote text-danger">{errors.weekdays}</p>}
        </fieldset>

        <fieldset>
          <legend className="text-footnote font-medium text-ink">Times each day, between 9:30 am and 5:00 pm</legend>
          <ul className="mt-2 space-y-2">
            {times.map((time, index) => (
              <li key={index} className="flex items-center gap-2">
                <div className="min-w-0 flex-1">
                  <Input
                    type="time"
                    aria-label={`Slot ${index + 1} starts`}
                    min={EXAM_DAY_START}
                    max={EXAM_DAY_END}
                    value={time.start}
                    onChange={(event) => changeTime(index, 'start', event.target.value)}
                  />
                </div>
                <span aria-hidden="true" className="text-ink-tertiary">
                  –
                </span>
                <div className="min-w-0 flex-1">
                  <Input
                    type="time"
                    aria-label={`Slot ${index + 1} ends`}
                    min={EXAM_DAY_START}
                    max={EXAM_DAY_END}
                    value={time.end}
                    onChange={(event) => changeTime(index, 'end', event.target.value)}
                  />
                </div>
                <IconButton
                  label={`Remove slot ${index + 1}`}
                  icon={X}
                  disabled={times.length === 1}
                  onClick={() => setTimes((current) => current.filter((_, position) => position !== index))}
                />
              </li>
            ))}
          </ul>
          {errors.times && <p className="mt-1 text-footnote text-danger">{errors.times}</p>}
          <Button
            variant="quiet"
            icon={Plus}
            className="mt-2"
            disabled={times.length >= 6}
            onClick={() => setTimes((current) => [...current, { start: '', end: '' }])}
          >
            Add a time
          </Button>
        </fieldset>
      </form>
    </Dialog>
  )
}
