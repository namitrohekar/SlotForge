import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { isWeekend } from '../../../utils/format.js'
import { validDuration } from '../../../utils/validators.js'
import { updateExamination } from '../services/examsApi.js'

export default function ExaminationFormDialog({ exam, session, onClose, onSaved }) {
  const formId = useId()
  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      durationMinutes: String(exam.durationMinutes),
      earliestDate: exam.earliestDate ?? '',
      latestDate: exam.latestDate ?? '',
    },
    validate: (v) => ({
      durationMinutes: validDuration(v.durationMinutes),
      earliestDate: v.earliestDate && isWeekend(v.earliestDate) ? 'Choose a weekday' : undefined,
      latestDate:
        v.latestDate && isWeekend(v.latestDate)
          ? 'Choose a weekday'
          : v.earliestDate && v.latestDate && v.latestDate < v.earliestDate
            ? 'This cannot be before the earliest day'
            : undefined,
    }),
    onSubmit: async (v) => {
      await updateExamination(exam.id, {
        durationMinutes: Number(v.durationMinutes),
        earliestDate: v.earliestDate || null,
        latestDate: v.latestDate || null,
      })
      onSaved()
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={exam.subjectName}
      description={exam.subjectCode}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            Save changes
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Duration in minutes" required hint="A paper only fits slots at least this long" error={errors.durationMinutes}>
          <NumberInput {...field('durationMinutes')} />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Earliest day" hint="Optional" error={errors.earliestDate}>
            <Input {...field('earliestDate')} type="date" min={session.startDate} max={session.endDate} />
          </FormField>
          <FormField label="Latest day" hint="Optional" error={errors.latestDate}>
            <Input {...field('latestDate')} type="date" min={session.startDate} max={session.endDate} />
          </FormField>
        </div>
      </form>
    </Dialog>
  )
}
