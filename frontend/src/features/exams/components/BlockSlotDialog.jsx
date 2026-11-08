import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { formatDay, formatTime } from '../../../utils/format.js'
import { required } from '../../../utils/validators.js'
import { changeSlotStatus } from '../services/examsApi.js'

export default function BlockSlotDialog({ slot, onClose, onBlocked }) {
  const formId = useId()
  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: { reason: '' },
    validate: (v) => ({ reason: required(v.reason, 'A reason') }),
    onSubmit: async (v) => {
      await changeSlotStatus(slot.id, { status: 'BLOCKED', reason: v.reason.trim() })
      onBlocked()
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title="Block this slot"
      description={`${formatDay(slot.date)}, ${formatTime(slot.start)} to ${formatTime(slot.end)}. No paper will be placed here.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            Block slot
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit}>
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Reason" required error={errors.reason}>
          <Input {...field('reason')} maxLength={255} placeholder="Public holiday" />
        </FormField>
      </form>
    </Dialog>
  )
}
