import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import NumberInput from '../../../components/common/NumberInput.jsx'
import Select from '../../../components/common/Select.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { required, wholeNumberBetween } from '../../../utils/validators.js'
import { createHall, updateHall } from '../services/hallsApi.js'

const CODE_PATTERN = /^[A-Za-z0-9][A-Za-z0-9-]*$/

export default function HallFormDialog({ hall, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(hall)

  const { errors, formError, submitting, field, handleSubmit } = useForm({
    initial: {
      code: hall?.code ?? '',
      name: hall?.name ?? '',
      building: hall?.building ?? '',
      floor: hall?.floor == null ? '' : String(hall.floor),
      capacity: hall ? String(hall.capacity) : '',
      status: hall?.status ?? 'AVAILABLE',
    },
    validate: (v) => ({
      code: required(v.code, 'Code') ?? (CODE_PATTERN.test(v.code.trim()) ? undefined : 'Use letters, digits and hyphens only'),
      name: required(v.name, 'Name'),
      building: required(v.building, 'Building'),
      floor: v.floor === '' ? undefined : wholeNumberBetween(v.floor, 0, 50),
      capacity: wholeNumberBetween(v.capacity, 1, 2000),
    }),
    onSubmit: async (v) => {
      const body = {
        code: v.code.trim(),
        name: v.name.trim(),
        building: v.building.trim(),
        floor: v.floor === '' ? null : Number(v.floor),
        capacity: Number(v.capacity),
        status: v.status,
      }
      const saved = editing ? await updateHall(hall.id, body) : await createHall(body)
      onSaved(editing ? 'Hall updated' : `${saved.name} was added`)
    },
  })

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit hall' : 'Add hall'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add hall'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <FormField label="Name" required error={errors.name}>
          <Input {...field('name')} maxLength={120} />
        </FormField>
        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Code" required hint="The short form used on timetables." error={errors.code}>
            <Input {...field('code')} maxLength={20} autoCapitalize="characters" spellCheck={false} className="uppercase" />
          </FormField>
          <FormField label="Exam seats" required hint="Seats usable in an exam, not the room's full capacity." error={errors.capacity}>
            <NumberInput {...field('capacity')} />
          </FormField>
          <FormField label="Building" required error={errors.building}>
            <Input {...field('building')} maxLength={80} />
          </FormField>
          <FormField label="Floor" error={errors.floor}>
            <NumberInput {...field('floor')} />
          </FormField>
        </div>
        <FormField label="Status" hint="Only available halls are used when a timetable is generated." error={errors.status}>
          <Select {...field('status')}>
            <option value="AVAILABLE">Available</option>
            <option value="MAINTENANCE">Maintenance</option>
            <option value="RETIRED">Retired</option>
          </Select>
        </FormField>
      </form>
    </Dialog>
  )
}
