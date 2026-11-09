import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required } from '../../../utils/validators.js'
import { createDepartment, updateDepartment } from '../services/academicsApi.js'

const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9-]*$/

// Mounted only while open, so every opening starts from the department's current values.
export default function DepartmentFormDialog({ department, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(department)

  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: { code: department?.code ?? '', name: department?.name ?? '' },
    validate: (v) => ({
      code: required(v.code, 'Code') ?? (CODE_PATTERN.test(v.code.trim()) ? undefined : 'Use letters, digits and hyphens only'),
      name: required(v.name, 'Name'),
    }),
    onSubmit: async (v) => {
      const body = { code: v.code.trim(), name: v.name.trim() }
      const saved = editing ? await updateDepartment(department.id, body) : await createDepartment(body)
      onSaved(editing ? 'Department updated' : `${saved.name} was added`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit department' : 'Add department'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add department'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={150} placeholder="Computer Engineering & Information Technology" />
        </FormField>
        <FormField label="Code" required hint="The short form used on timetables, such as CEIT" error={errors.code}>
          <Input {...field('code')} maxLength={20} autoCapitalize="characters" spellCheck={false} className="uppercase" />
        </FormField>
      </form>
    </Dialog>
  )
}
