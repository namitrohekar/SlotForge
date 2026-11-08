import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Checkbox from '../../../components/common/Checkbox.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import Select from '../../../components/common/Select.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required } from '../../../utils/validators.js'
import { createSubject, updateSubject } from '../services/academicsApi.js'

const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9-]*$/

export default function SubjectFormDialog({ subject, departments, defaultDepartmentId, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(subject)

  const { values, errors, formError, submitting, field, setValue, handleSubmit } = useForm({
    initial: {
      departmentId: String(subject?.departmentId ?? defaultDepartmentId ?? ''),
      code: subject?.code ?? '',
      name: subject?.name ?? '',
      heavy: subject?.heavy ?? false,
    },
    validate: (v) => ({
      departmentId: v.departmentId ? undefined : 'Choose a department',
      code: required(v.code, 'Code') ?? (CODE_PATTERN.test(v.code.trim()) ? undefined : 'Use letters, digits and hyphens only'),
      name: required(v.name, 'Name'),
    }),
    onSubmit: async (v) => {
      const body = { departmentId: Number(v.departmentId), code: v.code.trim(), name: v.name.trim(), heavy: v.heavy }
      const saved = editing ? await updateSubject(subject.id, body) : await createSubject(body)
      onSaved(editing ? 'Subject updated' : `${saved.code} was added`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit subject' : 'Add subject'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add subject'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={150} placeholder="Data Structures and Algorithms" />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Code" required hint="As printed on the syllabus." error={errors.code}>
            <Input {...field('code')} maxLength={20} autoCapitalize="characters" spellCheck={false} className="uppercase" />
          </FormField>
          <FormField label="Taught by" required error={errors.departmentId}>
            <Select {...field('departmentId')}>
              <option value="">Choose a department</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </Select>
          </FormField>
        </div>
        <div>
          <Checkbox
            label="Heavy paper"
            checked={values.heavy}
            onChange={(event) => setValue('heavy', event.target.checked)}
          />
          <p className="text-footnote text-ink-tertiary">
            A student never sits two heavy papers on consecutive days. Mark demanding papers such as mathematics.
          </p>
        </div>
      </form>
    </Dialog>
  )
}
