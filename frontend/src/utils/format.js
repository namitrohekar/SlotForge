const dateFormat = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const dayFormat = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
const dateTimeFormat = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
const longDateFormat = new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
const clockFormat = new Intl.DateTimeFormat('en-IN', { timeStyle: 'medium' })
const numberFormat = new Intl.NumberFormat('en-IN')

// A plain YYYY-MM-DD string is parsed as UTC by Date, so read it as a local date.
const toDate = (value) => (/^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value))

export const formatDate = (value) => dateFormat.format(toDate(value))
export const formatDay = (value) => dayFormat.format(toDate(value))
export const formatDateTime = (value) => dateTimeFormat.format(toDate(value))
// "10:00 am", matching the 12-hour time pickers the browser shows.
export const formatTime = (value) => {
  const [hours, minutes] = String(value).split(':').map(Number)
  return `${hours % 12 || 12}:${String(minutes).padStart(2, '0')} ${hours < 12 ? 'am' : 'pm'}`
}
export const formatNumber = (value) => numberFormat.format(value)
export const formatLongDate = (value) => longDateFormat.format(value)
export const formatClock = (value) => clockFormat.format(value)

// "8 semesters · 4 years": how a programme's length reads at VJTI.
export const formatLength = (semesters) => {
  const years = semesters / 2
  return `${semesters} ${semesters === 1 ? 'semester' : 'semesters'} · ${years} ${years === 1 ? 'year' : 'years'}`
}

// "3 h", "1 h 30 min", "45 min".
export const formatDuration = (minutes) => {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return [hours && `${hours} h`, rest && `${rest} min`].filter(Boolean).join(' ')
}

// Today as YYYY-MM-DD in the viewer's zone, the format date inputs use.
export const todayIso = () => new Date().toLocaleDateString('en-CA')

export const addDaysIso = (iso, days) => {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day + days).toLocaleDateString('en-CA')
}

export const isWeekend = (iso) => {
  const [year, month, day] = iso.split('-').map(Number)
  return [0, 6].includes(new Date(year, month - 1, day).getDay())
}
