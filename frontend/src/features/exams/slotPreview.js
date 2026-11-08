const parts = (iso) => iso.split('-').map(Number)
const clock = (value) => String(value).slice(0, 5)

// How many slots a template would really add, so the button never promises what an overlap will skip.
export function countRequestedSlots({ from, to, weekdays, times, existing = [] }) {
  if (!from || !to || from > to) return 0
  const [fromYear, fromMonth, fromDay] = parts(from)
  const [toYear, toMonth, toDay] = parts(to)
  const last = new Date(toYear, toMonth - 1, toDay)
  const usable = times.filter(({ start, end }) => start && end)
  let count = 0
  for (let date = new Date(fromYear, fromMonth - 1, fromDay); date <= last; date.setDate(date.getDate() + 1)) {
    // getDay() counts from Sunday as 0. The API uses ISO numbers, with Sunday as 7.
    if (!weekdays.includes(date.getDay() || 7)) continue
    const iso = date.toLocaleDateString('en-CA')
    const taken = existing.filter((slot) => slot.date === iso)
    count += usable.filter(({ start, end }) => !taken.some((slot) => start < clock(slot.end) && clock(slot.start) < end)).length
  }
  return count
}
