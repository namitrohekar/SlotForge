import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Select from '../../../components/common/Select.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { useForm } from '../../../hooks/useForm.js'
import { listUsers } from '../../users/services/usersApi.js'
import { appointHead } from '../services/academicsApi.js'

const loadCandidates = () => listUsers({ role: 'HOD', status: 'ACTIVE', size: 100, sort: 'fullName,asc' })

export default function AppointHeadDialog({ department, onClose, onSaved }) {
  const formId = useId()
  const candidates = useApi(loadCandidates)

  const { formError, submitting, field, handleSubmit } = useForm({
    initial: { headId: department.headUserId ?? '' },
    validate: () => ({}),
    onSubmit: async (v) => {
      await appointHead(department.id, v.headId ? Number(v.headId) : null)
      onSaved(v.headId ? `Head of ${department.code} appointed` : `${department.code} has no head now`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={`Head of ${department.code}`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            Save
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        {candidates.error && <Alert tone="danger">{candidates.error.message}</Alert>}
        {candidates.data?.items.length === 0 && (
          <Alert tone="warning">No active account has the Head of department role. Add one in Users first.</Alert>
        )}
        <FormField label="Head of department" hint="Only this person can approve timetables for students of this department.">
          <Select {...field('headId')}>
            <option value="">No head appointed</option>
            {candidates.data?.items.map((user) => (
              <option key={user.id} value={user.id}>
                {user.fullName}
              </option>
            ))}
          </Select>
        </FormField>
      </form>
    </Dialog>
  )
}
