export const required = (value, label = 'This field') =>
  String(value ?? '').trim() ? undefined : `${label} is required`

export const validEmail = (value) =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value ?? '').trim()) ? undefined : 'Enter a valid email address'

export const positiveNumber = (value, label = 'This value') =>
  Number(value) > 0 ? undefined : `${label} must be greater than 0`

export const validYear = (value) => {
  const year = Number(value)
  return Number.isInteger(year) && year >= 2000 && year <= 2100 ? undefined : 'Enter a year from 2000 to 2100'
}

export const validDuration = (value) => {
  const minutes = Number(value)
  return Number.isInteger(minutes) && minutes >= 15 && minutes <= 480 ? undefined : 'Enter 15 to 480 minutes'
}

export const wholeNumberBetween = (value, low, high) => {
  const number = Number(value)
  return value !== '' && Number.isInteger(number) && number >= low && number <= high ? undefined : `Enter a whole number from ${low} to ${high}`
}
