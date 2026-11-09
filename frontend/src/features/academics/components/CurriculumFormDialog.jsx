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
import { createCurriculum, updateCurriculum } from '../services/academicsApi.js'

export default function CurriculumFormDialog({ curriculum, courses, defaultCourseId, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(curriculum)
  const [thisYear] = useState(() => new Date().getFullYear())

  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      courseId: String(curriculum?.courseId ?? defaultCourseId ?? ''),
      name: curriculum?.name ?? '',
      effectiveFromYear: String(curriculum?.effectiveFromYear ?? thisYear),
    },
    validate: (v) => ({
      courseId: v.courseId ? undefined : 'Choose a programme',
      name: required(v.name, 'Name'),
      effectiveFromYear: validYear(v.effectiveFromYear),
    }),
    onSubmit: async (v) => {
      const body = { courseId: Number(v.courseId), name: v.name.trim(), effectiveFromYear: Number(v.effectiveFromYear) }
      const saved = editing ? await updateCurriculum(curriculum.id, body) : await createCurriculum(body)
      onSaved(editing ? 'Curriculum updated' : `${saved.name} was added`, saved)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit curriculum' : 'Add curriculum'}
      description="A curriculum is one version of a programme's syllabus. Batches admitted under it keep it."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add curriculum'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField
          label="Programme"
          required
          hint={editing ? 'A curriculum stays with its programme' : undefined}
          error={errors.courseId}
        >
          <Select {...field('courseId')} disabled={editing}>
            <option value="">Choose a programme</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </Select>
        </FormField>
        <div className="grid gap-5 sm:grid-cols-[minmax(0,1fr)_9rem]">
          <FormField label="Name" required error={errors.name}>
            <Input {...field('name')} maxLength={80} placeholder="2024 scheme" />
          </FormField>
          <FormField label="In force from" required error={errors.effectiveFromYear}>
            <NumberInput {...field('effectiveFromYear')} />
          </FormField>
        </div>
      </form>
    </Dialog>
  )
}
