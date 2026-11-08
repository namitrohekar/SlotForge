import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import Select from '../../../components/common/Select.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required } from '../../../utils/validators.js'
import { createCourse, updateCourse } from '../services/academicsApi.js'

const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9-]*$/

function semestersError(value) {
  const count = Number(value)
  if (String(value).trim() === '') return 'Semesters is required'
  return Number.isInteger(count) && count >= 1 && count <= 12 ? undefined : 'Enter a whole number from 1 to 12'
}

export default function CourseFormDialog({ course, departments, defaultDepartmentId, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(course)

  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      departmentId: String(course?.departmentId ?? defaultDepartmentId ?? ''),
      code: course?.code ?? '',
      name: course?.name ?? '',
      totalSemesters: String(course?.totalSemesters ?? ''),
    },
    validate: (v) => ({
      departmentId: v.departmentId ? undefined : 'Choose a department',
      code: required(v.code, 'Code') ?? (CODE_PATTERN.test(v.code.trim()) ? undefined : 'Use letters, digits and hyphens only'),
      name: required(v.name, 'Name'),
      totalSemesters: semestersError(v.totalSemesters),
    }),
    onSubmit: async (v) => {
      const body = {
        departmentId: Number(v.departmentId),
        code: v.code.trim(),
        name: v.name.trim(),
        totalSemesters: Number(v.totalSemesters),
      }
      const saved = editing ? await updateCourse(course.id, body) : await createCourse(body)
      onSaved(editing ? 'Programme updated' : `${saved.name} was added`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit programme' : 'Add programme'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add programme'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={150} placeholder="B.Tech Computer Engineering" />
        </FormField>
        <FormField label="Department" required error={errors.departmentId}>
          <Select {...field('departmentId')}>
            <option value="">Choose a department</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.name}
              </option>
            ))}
          </Select>
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Code" required hint="Such as BT-CE or MCA." error={errors.code}>
            <Input {...field('code')} maxLength={20} autoCapitalize="characters" spellCheck={false} className="uppercase" />
          </FormField>
          <FormField label="Semesters" required hint="8 for B.Tech, 6 for a diploma, 4 for M.Tech and MCA." error={errors.totalSemesters}>
            <NumberInput {...field('totalSemesters')} />
          </FormField>
        </div>
      </form>
    </Dialog>
  )
}
