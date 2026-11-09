import { useId } from 'react'
import Alert from '../../../components/common/Alert.jsx'
import Button from '../../../components/common/Button.jsx'
import ChoiceList from '../../../components/common/ChoiceList.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import FormField from '../../../components/common/FormField.jsx'
import Input from '../../../components/common/Input.jsx'
import { useForm } from '../../../hooks/useForm.js'
import { canAddRole, ROLE_LABELS, roleConflict } from '../../../utils/roles.js'
import { required, validEmail } from '../../../utils/validators.js'
import { changeRoles, createUser, renameUser } from '../services/usersApi.js'

const ROLES = Object.keys(ROLE_LABELS)
const sameRoles = (a, b) => a.length === b.length && a.every((role) => b.includes(role))

// Mounted only while open, so every opening starts from the user's current values.
export default function UserFormDialog({ user, isSelf, onClose, onSaved }) {
  const formId = useId()
  const editing = Boolean(user)

  const { values, errors, formError, submitting, field, setValue, handleSubmit } = useForm({
    initial: { fullName: user?.fullName ?? '', email: user?.email ?? '', roles: user?.roles ?? [] },
    validate: (v) => ({
      fullName: required(v.fullName, 'Name'),
      email: editing ? undefined : validEmail(v.email),
      roles: v.roles.length ? roleConflict(v.roles) : 'Choose at least one role',
    }),
    onSubmit: async (v) => {
      const fullName = v.fullName.trim()
      if (!editing) {
        onSaved(`${fullName} was added`, await createUser({ fullName, email: v.email.trim(), roles: v.roles }))
        return
      }
      if (fullName !== user.fullName) await renameUser(user.id, fullName)
      if (!sameRoles(v.roles, user.roles)) await changeRoles(user.id, v.roles)
      onSaved('Changes saved')
    },
  })

  const toggleRole = (role, checked) =>
    setValue('roles', checked ? [...values.roles, role] : values.roles.filter((held) => held !== role))

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? 'Edit user' : 'Add user'}
      description={editing ? user.email : 'They can sign in once they have an account here'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" form={formId} loading={submitting}>
            {editing ? 'Save changes' : 'Add user'}
          </Button>
        </>
      }
    >
      <form id={formId} noValidate onSubmit={handleSubmit} className="space-y-5">
        {formError && <Alert tone="danger">{formError}</Alert>}

        <FormField label="Full name" required error={errors.fullName}>
          <Input {...field('fullName')} autoComplete="name" maxLength={150} />
        </FormField>

        {!editing && (
          <FormField label="Email" required hint="Their institutional address" error={errors.email}>
            <Input {...field('email')} type="email" autoComplete="email" inputMode="email" maxLength={254} />
          </FormField>
        )}

        <fieldset>
          <legend className="text-footnote font-medium text-ink">
            Roles
            <span aria-hidden="true" className="text-danger">
              {' '}
              *
            </span>
          </legend>
          <p className="mt-1 text-footnote text-ink-tertiary">
            {isSelf
              ? 'You cannot change your own roles'
              : 'A student holds no other role. Staff may add Administrator or Exam coordinator, except a head of department.'}
          </p>
          <div className="mt-2">
            <ChoiceList
              options={ROLES.map((role) => ({
                value: role,
                label: ROLE_LABELS[role],
                disabled: isSelf || !canAddRole(role, values.roles),
              }))}
              isChosen={(role) => values.roles.includes(role)}
              onToggle={toggleRole}
            />
          </div>
          {errors.roles && <p className="mt-1 text-footnote text-danger">{errors.roles}</p>}
        </fieldset>
      </form>
    </Dialog>
  )
}
