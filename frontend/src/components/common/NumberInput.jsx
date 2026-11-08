import Input from './Input.jsx'

// Digits only, so a minus sign, an exponent or a pasted value cannot get in. Limits are checked by the form.
export default function NumberInput({ onChange, ...props }) {
  return (
    <Input
      {...props}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      onChange={(event) => {
        event.target.value = event.target.value.replace(/\D/g, '')
        onChange(event)
      }}
    />
  )
}
