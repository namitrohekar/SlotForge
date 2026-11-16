import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { addDaysIso, isWeekend, todayIso } from '../../../utils/format.js'
import { required, wholeNumberBetween } from '../../../utils/validators.js'
import { createSession, updateSession } from '../services/examsApi.js'

const MAX_SESSION_DAYS = 120

export default function SessionFormDialog({ session, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(session)
  const today = todayIso()

  const { values, errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      name: session?.name ?? '',
      startDate: session?.startDate ?? '',
      endDate: session?.endDate ?? '',
      maxExamsPerStudentPerDay: String(session?.maxExamsPerStudentPerDay ?? 1),
      heavyGapDays: String(session?.heavyGapDays ?? 1),
      studentsPerInvigilator: String(session?.studentsPerInvigilator ?? 30),
      maxDutiesPerInvigilator: String(session?.maxDutiesPerInvigilator ?? 8),
    },
    validate: (v) => ({
      name: required(v.name, 'Name'),
      startDate: !v.startDate
        ? 'Choose a start date'
        : v.startDate !== session?.startDate && v.startDate < today
          ? 'The session cannot start in the past'
          : v.startDate !== session?.startDate && isWeekend(v.startDate)
            ? 'Choose a weekday. The college is closed at weekends'
            : undefined,
      endDate: !v.endDate
        ? 'Choose an end date'
        : v.startDate && v.endDate < v.startDate
          ? 'The end cannot be before the start'
          : v.endDate !== session?.endDate && v.endDate < today
            ? 'The session cannot end in the past'
            : v.endDate !== session?.endDate && isWeekend(v.endDate)
              ? 'Choose a weekday. The college is closed at weekends'
            : v.startDate && v.endDate > addDaysIso(v.startDate, MAX_SESSION_DAYS)
              ? `A session can run for at most ${MAX_SESSION_DAYS} days`
              : undefined,
      maxExamsPerStudentPerDay: wholeNumberBetween(v.maxExamsPerStudentPerDay, 1, 4),
      heavyGapDays: wholeNumberBetween(v.heavyGapDays, 0, 14),
      studentsPerInvigilator: wholeNumberBetween(v.studentsPerInvigilator, 1, 200),
      maxDutiesPerInvigilator: wholeNumberBetween(v.maxDutiesPerInvigilator, 1, 40),
    }),
    onSubmit: async (v) => {
      const body = {
        name: v.name.trim(),
        startDate: v.startDate,
        endDate: v.endDate,
        maxExamsPerStudentPerDay: Number(v.maxExamsPerStudentPerDay),
        heavyGapDays: Number(v.heavyGapDays),
        studentsPerInvigilator: Number(v.studentsPerInvigilator),
        maxDutiesPerInvigilator: Number(v.maxDutiesPerInvigilator),
      }
      const saved = editing ? await updateSession(session.id, body) : await createSession(body)
      onSaved(editing ? 'Session updated' : `${saved.name} was added`, saved)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit session details' : 'New exam session'}
      description={
        editing
          ? 'Slots, papers and timetables are managed inside the session and are not changed here. New rules apply the next time you generate.'
          : 'You will add its slots and papers next'
      }
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Create session'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={120} placeholder="End Semester, November 2026" />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="First day" required error={errors.startDate}>
            <Input {...field('startDate')} type="date" min={today} />
          </FormField>
          <FormField label="Last day" required error={errors.endDate}>
            <Input {...field('endDate')} type="date" min={values.startDate || today} />
          </FormField>
          <FormField label="Papers per student per day" required error={errors.maxExamsPerStudentPerDay}>
            <NumberInput {...field('maxExamsPerStudentPerDay')} />
          </FormField>
          <FormField label="Rest days between heavy papers" required error={errors.heavyGapDays}>
            <NumberInput {...field('heavyGapDays')} />
          </FormField>
          <FormField label="Students per invigilator" required hint="One more invigilator for every started group" error={errors.studentsPerInvigilator}>
            <NumberInput {...field('studentsPerInvigilator')} />
          </FormField>
          <FormField label="Most duties per invigilator" required error={errors.maxDutiesPerInvigilator}>
            <NumberInput {...field('maxDutiesPerInvigilator')} />
          </FormField>
        </div>
      </form>
    </Dialog>
  )
}
