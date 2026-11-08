import { useState } from 'react'
import { toApiError } from '../services/apiError.js'

function focusFirstInvalid(form) {
  requestAnimationFrame(() => form?.querySelector('[aria-invalid="true"]')?.focus())
}

export function useForm({ initial, validate = () => ({}), onSubmit }) {
  const [values, setValues] = useState(initial)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const setValue = (name, value) => {
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  const field = (name) => ({
    name,
    value: values[name] ?? '',
    onChange: (event) => setValue(name, event.target.value),
  })

  const handleSubmit = async (event) => {
    event.preventDefault()
    const form = event.currentTarget

    const found = validate(values)
    setErrors(found)
    setFormError(null)
    if (Object.values(found).some(Boolean)) {
      focusFirstInvalid(form)
      return
    }

    setSubmitting(true)
    try {
      await onSubmit(values)
    } catch (error) {
      const failure = toApiError(error)
      setErrors(Object.fromEntries(failure.fieldErrors.map(({ field: name, message }) => [name, message])))
      if (failure.fieldErrors.length > 0) focusFirstInvalid(form)
      else setFormError(failure.message)
    } finally {
      setSubmitting(false)
    }
  }

  return { values, errors, formError, submitting, field, setValue, handleSubmit }
}
