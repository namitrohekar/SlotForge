import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import FormField from '../components/common/FormField.jsx'
import Input from '../components/common/Input.jsx'
import { required } from '../utils/validators.js'
import { useForm } from './useForm.js'

function HallForm({ onSubmit }) {
  const form = useForm({
    initial: { name: '' },
    validate: (values) => ({ name: required(values.name, 'Hall name') }),
    onSubmit,
  })

  return (
    <form noValidate onSubmit={form.handleSubmit}>
      <FormField label="Hall name" error={form.errors.name}>
        <Input {...form.field('name')} />
      </FormField>
      {form.formError && <p role="alert">{form.formError}</p>}
      <button type="submit">Save</button>
    </form>
  )
}

describe('useForm', () => {
  it('blocks submit and shows the error beside the field', async () => {
    const onSubmit = vi.fn()
    render(<HallForm onSubmit={onSubmit} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('Hall name is required')).toBeInTheDocument()
    expect(screen.getByLabelText('Hall name')).toHaveAttribute('aria-invalid', 'true')
    await waitFor(() => expect(screen.getByLabelText('Hall name')).toHaveFocus())
  })

  it('submits valid values', async () => {
    const onSubmit = vi.fn().mockResolvedValue()
    render(<HallForm onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText('Hall name'), 'Central Hall')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Central Hall' })
  })

  it('shows server field errors under the matching input', async () => {
    const onSubmit = vi.fn().mockRejectedValue({
      response: { data: { code: 'VALIDATION_FAILED', fieldErrors: [{ field: 'name', message: 'already taken' }] } },
    })
    render(<HallForm onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText('Hall name'), 'Central Hall')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('already taken')).toBeInTheDocument()
  })

  it('shows other server failures for the whole form', async () => {
    const onSubmit = vi.fn().mockRejectedValue({
      response: { data: { code: 'DUPLICATE', detail: 'A record with these details already exists.' } },
    })
    render(<HallForm onSubmit={onSubmit} />)

    await userEvent.type(screen.getByLabelText('Hall name'), 'Central Hall')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('already exists')
  })
})
