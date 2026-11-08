import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Textarea from '../../../components/common/Textarea.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required } from '../../../utils/validators.js'
import { returnVersion } from '../services/schedulesApi.js'

export default function ReturnDialog({ version, onClose, onReturned }) {
  const formId = useId()
  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: { comment: '' },
    validate: (v) => ({ comment: required(v.comment, 'A comment') }),
    onSubmit: async (v) => {
      await returnVersion(version.id, v.comment.trim())
      onReturned()
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={`Send back version ${version.versionNo}`}
      description="The coordinator sees your comment and can generate a new version."
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            Send back
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit}>
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="What needs to change" required error={errors.comment}>
          <Textarea {...field('comment')} maxLength={500} />
        </FormField>
      </form>
    </Dialog>
  )
}
