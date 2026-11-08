import { useId, useState } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import Select from '../../../components/common/Select.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required, validYear } from '../../../utils/validators.js'
import { createBatch, updateBatch } from '../services/academicsApi.js'

export default function BatchFormDialog({ batch, curricula, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(batch)
  const [thisYear] = useState(() => new Date().getFullYear())
  const semestersOf = (id) => curricula.find((curriculum) => String(curriculum.id) === id)?.totalSemesters

  const { values, errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      curriculumId: String(batch?.curriculumId ?? ''),
      name: batch?.name ?? '',
      admissionYear: String(batch?.admissionYear ?? thisYear),
      currentSemester: String(batch?.currentSemester ?? 1),
    },
    validate: (v) => {
      const last = semestersOf(v.curriculumId) ?? 12
      const semester = Number(v.currentSemester)
      return {
        curriculumId: v.curriculumId ? undefined : 'Choose a curriculum',
        name: required(v.name, 'Name'),
        admissionYear: validYear(v.admissionYear) ?? (Number(v.admissionYear) > thisYear + 1 ? `Enter ${thisYear + 1} or earlier` : undefined),
        currentSemester: Number.isInteger(semester) && semester >= 1 && semester <= last ? undefined : `Enter a semester from 1 to ${last}`,
      }
    },
    onSubmit: async (v) => {
      const body = {
        curriculumId: Number(v.curriculumId),
        name: v.name.trim(),
        admissionYear: Number(v.admissionYear),
        currentSemester: Number(v.currentSemester),
      }
      const saved = editing ? await updateBatch(batch.id, body) : await createBatch(body)
      onSaved(editing ? 'Batch updated' : `${saved.name} was added`)
    },
  })

  const last = semestersOf(values.curriculumId)

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit batch' : 'Add batch'}
      description="A batch is the students admitted together. Its current semester decides which papers it sits."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add batch'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField
          label="Curriculum"
          required
          hint={editing ? 'A batch stays with its curriculum.' : undefined}
          error={errors.curriculumId}
        >
          <Select {...field('curriculumId')} disabled={editing}>
            <option value="">Choose a curriculum</option>
            {curricula.map((curriculum) => (
              <option key={curriculum.id} value={curriculum.id}>
                {curriculum.courseCode} · {curriculum.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={50} placeholder="2026-28" />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Admitted in" required error={errors.admissionYear}>
            <NumberInput {...field('admissionYear')} />
          </FormField>
          <FormField
            label="Current semester"
            required
            hint={last ? `This programme has ${last} semesters.` : undefined}
            error={errors.currentSemester}
          >
            <NumberInput {...field('currentSemester')} />
          </FormField>
        </div>
      </form>
    </Dialog>
  )
}
