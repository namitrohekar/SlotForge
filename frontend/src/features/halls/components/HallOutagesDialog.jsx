import { useCallback, useState } from 'react'
import { Trash2 } from 'lucide-react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import IconButton from '../../../components/common/IconButton.jsx'
import Input from '../../../components/common/Input.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useForm } from '../../../hooks/useForm.js'
import { toApiError } from '../../../services/apiError.js'
import { formatDay, todayIso } from '../../../utils/format.js'
import { required } from '../../../utils/validators.js'
import { addHallOutage, listHallOutages, removeHallOutage } from '../services/hallsApi.js'

const range = (outage) => (outage.toDate === outage.fromDate ? formatDay(outage.fromDate) : `${formatDay(outage.fromDate)} – ${formatDay(outage.toDate)}`)

// Dates a hall is closed. No paper is placed there on those dates, and a timetable that already uses it is flagged for repair.
export default function HallOutagesDialog({ hall, canEdit, onClose }) {
  const { data, error, reload } = useApi(useCallback(() => listHallOutages(hall.id), [hall.id]))
  const [removing, setRemoving] = useState(null)
  const [failure, setFailure] = useState(null)
  const today = todayIso()

  const { values, errors, formError, submitting, field, handleSubmit, setValue } = useForm({
    initial: { fromDate: '', toDate: '', reason: '' },
    validate: (v) => ({
      fromDate: required(v.fromDate, 'From'),
      toDate: required(v.toDate, 'To') ?? (v.toDate < v.fromDate ? 'Must be on or after the start date' : undefined),
      reason: required(v.reason, 'Reason'),
    }),
    onSubmit: async (v) => {
      await addHallOutage(hall.id, { fromDate: v.fromDate, toDate: v.toDate, reason: v.reason.trim() })
      Object.keys(v).forEach((name) => setValue(name, ''))
      reload()
    },
  })

  const remove = async (outage) => {
    setRemoving(outage.id)
    setFailure(null)
    try {
      await removeHallOutage(hall.id, outage.id)
      reload()
    } catch (problem) {
      setFailure(toApiError(problem).message)
    } finally {
      setRemoving(null)
    }
  }

  const affected = data?.reduce((total, outage) => total + outage.papersAffected, 0) ?? 0

  return (
    <Dialog
      open
      onClose={onClose}
      title={`Dates ${hall.name} is unavailable`}
      description="No paper is placed here on these dates"
      footer={
        <Button variant="secondary" onClick={onClose}>
          Done
        </Button>
      }
    >
      <div className="space-y-5">
        {(formError || failure) && <Alert tone="danger">{formError || failure}</Alert>}
        {error && <Alert tone="danger">{error.message}</Alert>}
        {affected > 0 && (
          <Alert tone="warning" title={`${affected} ${affected === 1 ? 'paper is' : 'papers are'} placed here on these dates`}>
            Open the session’s timetable and choose Repair. Only those papers change hall.
          </Alert>
        )}
        {!data && !error && <Skeleton className="h-16 w-full rounded-md" />}
        {data?.length === 0 && <p className="text-callout text-ink-secondary">No unavailable dates recorded</p>}
        {data?.length > 0 && (
          <ul aria-label="Unavailable dates" className="border-y border-line">
            {data.map((outage) => (
              <li key={outage.id} className="flex items-center justify-between gap-4 border-b border-line py-2.5 last:border-b-0">
                <span className="min-w-0">
                  <span data-numeric className="block text-callout text-ink">
                    {range(outage)}
                  </span>
                  <span className="block text-footnote text-ink-tertiary">{outage.reason}</span>
                </span>
                {canEdit && <IconButton label={`Remove ${outage.reason}`} icon={Trash2} disabled={removing === outage.id} onClick={() => remove(outage)} />}
              </li>
            ))}
          </ul>
        )}
        {canEdit && (
          <form noValidate onSubmit={handleSubmit} className="grid items-start gap-3 sm:grid-cols-2">
            <FormField label="From" required error={errors.fromDate}>
              <Input {...field('fromDate')} type="date" min={today} />
            </FormField>
            <FormField label="To" required error={errors.toDate}>
              <Input {...field('toDate')} type="date" min={values.fromDate || today} />
            </FormField>
            <div className="sm:col-span-2">
              <FormField label="Reason" required error={errors.reason}>
                <Input {...field('reason')} maxLength={255} placeholder="Electrical repairs" />
              </FormField>
            </div>
            <Button type="submit" variant="secondary" loading={submitting} className="sm:col-span-2 sm:justify-self-start">
              Add dates
            </Button>
          </form>
        )}
      </div>
    </Dialog>
  )
}
