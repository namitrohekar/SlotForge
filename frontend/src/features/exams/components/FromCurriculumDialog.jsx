import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ChoiceList from '../../../components/common/ChoiceList.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useForm } from '../../../hooks/useForm.js'
import { validDuration } from '../../../utils/validators.js'
import { listAllBatches } from '../../academics/services/academicsApi.js'
import { createFromCurriculum } from '../services/examsApi.js'

export default function FromCurriculumDialog({ session, onClose, onCreated }) {
  const formId = useId()
  const { data: batches, error } = useApi(listAllBatches)
  const { values, errors, formError, submitting, field, setValue, handleSubmit } = useForm({
    initial: { batchIds: [], durationMinutes: '180' },
    validate: (v) => ({
      batchIds: v.batchIds.length ? undefined : 'Choose at least one batch',
      durationMinutes: validDuration(v.durationMinutes),
    }),
    onSubmit: async (v) => onCreated(await createFromCurriculum(session.id, { batchIds: v.batchIds, durationMinutes: Number(v.durationMinutes) })),
  })

  const toggle = (id, checked) => setValue('batchIds', checked ? [...values.batchIds, id] : values.batchIds.filter((held) => held !== id))

  return (
    <Dialog
      open
      onClose={onClose}
      title="Add papers from the curriculum"
      description="Each batch gets the papers of its current semester. Core papers enrol the whole batch. Electives wait for their students."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            Add papers
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        {error && <Alert tone="danger">{error.message}</Alert>}
        <fieldset>
          <legend className="text-footnote font-medium text-ink">Batches</legend>
          {!batches && !error && <Skeleton className="mt-2 h-32 w-full rounded-md" />}
          {batches?.length === 0 && <p className="mt-2 text-callout text-ink-tertiary">No batches yet. Add them under Academics.</p>}
          {batches?.length > 0 && (
            <div className="mt-2">
              <ChoiceList
                options={batches.map((batch) => ({
                  value: batch.id,
                  label: `${batch.courseCode} · ${batch.name}`,
                  detail: `Semester ${batch.currentSemester} · ${batch.studentCount} students`,
                }))}
                isChosen={(id) => values.batchIds.includes(id)}
                onToggle={toggle}
              />
            </div>
          )}
          {errors.batchIds && <p className="mt-1 text-footnote text-danger">{errors.batchIds}</p>}
        </fieldset>
        <FormField label="Duration in minutes" required hint="You can change it for any single paper afterwards" error={errors.durationMinutes}>
          <NumberInput {...field('durationMinutes')} />
        </FormField>
      </form>
    </Dialog>
  )
}
