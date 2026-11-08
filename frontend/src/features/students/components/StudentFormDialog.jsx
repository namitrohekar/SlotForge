import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import Select from '../../../components/common/Select.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required, validEmail } from '../../../utils/validators.js'
import { createStudent, updateStudent } from '../services/studentsApi.js'

export default function StudentFormDialog({ student, batches, defaultBatchId, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(student)

  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      batchId: String(student?.batchId ?? defaultBatchId ?? ''),
      rollNo: student?.rollNo ?? '',
      fullName: student?.fullName ?? '',
      email: student?.email ?? '',
    },
    validate: (v) => ({
      batchId: v.batchId ? undefined : 'Choose a batch',
      rollNo: required(v.rollNo, 'Roll number') ?? (/^[A-Za-z0-9][A-Za-z0-9/-]*$/.test(v.rollNo.trim()) ? undefined : 'Use letters, digits, hyphens and slashes only'),
      fullName: required(v.fullName, 'Name'),
      email: validEmail(v.email),
    }),
    onSubmit: async (v) => {
      const body = { batchId: Number(v.batchId), rollNo: v.rollNo.trim(), fullName: v.fullName.trim(), email: v.email.trim() }
      const saved = editing ? await updateStudent(student.id, body) : await createStudent(body)
      onSaved(editing ? 'Student updated' : `${saved.fullName} was added`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit student' : 'Add student'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add student'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Full name" required error={errors.fullName}>
          <Input {...field('fullName')} maxLength={150} autoComplete="off" />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Roll number" required error={errors.rollNo}>
            <Input {...field('rollNo')} maxLength={30} spellCheck={false} autoComplete="off" />
          </FormField>
          <FormField label="Batch" required error={errors.batchId}>
            <Select {...field('batchId')}>
              <option value="">Choose a batch</option>
              {batches.map((batch) => (
                <option key={batch.id} value={batch.id}>
                  {batch.courseCode} · {batch.name}
                </option>
              ))}
            </Select>
          </FormField>
        </div>
        <FormField label="Email" required error={errors.email}>
          <Input {...field('email')} type="email" inputMode="email" maxLength={254} autoComplete="off" />
        </FormField>
      </form>
    </Dialog>
  )
}
