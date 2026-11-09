import { useState } from 'react'
import { CalendarCheck, Trash2 } from 'lucide-react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import EmptyState from '../../../components/common/EmptyState.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import FormField from '../../../components/common/FormField.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Input from '../../../components/common/Input.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import PageHeader from '../../../components/layout/PageHeader.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useForm } from '../../../hooks/useForm.js'
import { toApiError } from '../../../services/apiError.js'
import { useToastStore } from '../../../store/toastStore.js'
import { formatDay, formatTime, todayIso } from '../../../utils/format.js'
import { required } from '../../../utils/validators.js'
import { addUnavailability, listMyDuties, listMyUnavailability, removeUnavailability } from '../services/schedulesApi.js'

function DutyList({ duties }) {
  if (duties.length === 0) {
    return <EmptyState icon={CalendarCheck} title="No duties yet" description="Your invigilation duties appear here once a timetable is published." />
  }
  return (
    <ul className="overflow-hidden rounded-lg border border-line">
      {duties.map((duty) => (
        <li
          key={`${duty.date}-${duty.start}-${duty.hallCode}`}
          className="grid gap-x-6 gap-y-1 border-b border-line px-4 py-3 last:border-b-0 sm:grid-cols-[12rem_minmax(0,1fr)]"
        >
          <div data-numeric>
            <p className="text-callout font-medium text-ink">{formatDay(duty.date)}</p>
            <p className="text-footnote text-ink-tertiary">
              {formatTime(duty.start)} – {formatTime(duty.end)}
            </p>
          </div>
          <div className="min-w-0">
            <p className="text-callout font-medium text-ink">
              {duty.hallName} ({duty.hallCode})
            </p>
            <p data-numeric className="text-footnote text-ink-tertiary">
              {duty.paperCode} {duty.paperName} · {duty.students} students · {duty.building} · {duty.sessionName}
            </p>
            {duty.unavailable && <p className="mt-1 text-footnote text-danger">You marked this date as unavailable. The exam office has been alerted.</p>}
          </div>
        </li>
      ))}
    </ul>
  )
}

function UnavailableDates({ onSaved }) {
  const toast = useToastStore((state) => state.push)
  const { data, error, reload } = useApi(listMyUnavailability)
  const [removing, setRemoving] = useState(null)
  const today = todayIso()

  const { values, errors, formError, submitting, field, handleSubmit, setValue } = useForm({
    initial: { fromDate: '', toDate: '', reason: '' },
    validate: (v) => ({
      fromDate: required(v.fromDate, 'From'),
      toDate: required(v.toDate, 'To') ?? (v.toDate < v.fromDate ? 'Must be on or after the start date' : undefined),
      reason: required(v.reason, 'Reason'),
    }),
    onSubmit: async (v) => {
      const saved = await addUnavailability({ fromDate: v.fromDate, toDate: v.toDate, reason: v.reason.trim() })
      toast(
        saved.dutiesAffected
          ? { tone: 'warning', message: `Dates saved. You still have ${saved.dutiesAffected} published ${saved.dutiesAffected === 1 ? 'duty' : 'duties'} on them, and the exam office has been alerted.` }
          : { message: 'Dates saved' },
      )
      Object.keys(v).forEach((name) => setValue(name, ''))
      reload()
      onSaved()
    },
  })

  const remove = async (entry) => {
    setRemoving(entry.id)
    try {
      await removeUnavailability(entry.id)
      reload()
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
    } finally {
      setRemoving(null)
    }
  }

  return (
    <section aria-labelledby="unavailable" className="mt-10">
      <h2 id="unavailable" className="font-serif text-title-2 font-medium">
        Dates you cannot invigilate
      </h2>
      <p className="mt-1 mb-4 text-callout text-ink-secondary">Nobody will be given a duty on these dates in any timetable generated from now on.</p>

      <form noValidate onSubmit={handleSubmit} className="grid items-start gap-3 sm:grid-cols-[10rem_10rem_minmax(0,1fr)_auto]">
        <FormField label="From" required error={errors.fromDate}>
          <Input {...field('fromDate')} type="date" min={today} />
        </FormField>
        <FormField label="To" required error={errors.toDate}>
          <Input {...field('toDate')} type="date" min={values.fromDate || today} />
        </FormField>
        <FormField label="Reason" required error={errors.reason}>
          <Input {...field('reason')} maxLength={255} placeholder="Medical leave" />
        </FormField>
        <Button type="submit" loading={submitting} className="sm:mt-6">
          Add
        </Button>
      </form>
      {formError && (
        <div className="mt-3">
          <Alert tone="danger">{formError}</Alert>
        </div>
      )}

      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data?.length > 0 && (
        <ul className="mt-4 overflow-hidden rounded-lg border border-line">
          {data.map((entry) => (
            <li key={entry.id} className="flex items-center justify-between gap-4 border-b border-line px-4 py-2.5 last:border-b-0">
              <span className="min-w-0">
                <span data-numeric className="block text-callout text-ink">
                  {formatDay(entry.fromDate)}
                  {entry.toDate !== entry.fromDate && ` – ${formatDay(entry.toDate)}`}
                </span>
                <span className="block text-footnote text-ink-tertiary">{entry.reason}</span>
              </span>
              <IconButton label={`Remove ${entry.reason}`} icon={Trash2} disabled={removing === entry.id} onClick={() => remove(entry)} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default function MyDutiesPage() {
  const { data, error, reload } = useApi(listMyDuties)

  return (
    <>
      <PageHeader title="My duties" description="Where and when you are invigilating, from published timetables." />
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {!data && !error && <Skeleton className="h-40 w-full rounded-lg" />}
      {data && <DutyList duties={data} />}
      <UnavailableDates onSaved={reload} />
    </>
  )
}
