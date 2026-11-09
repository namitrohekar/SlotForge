import { addDaysIso, isWeekend } from '../../utils/format.js'

const MAX_FULL_DAYS = 10

/** Days to draw, with null for a weekend gap. A long session shows only the days that have papers. */
export function rulerDays(startDate, endDate, papersOn) {
  const all = []
  for (let day = startDate; day <= endDate; day = addDaysIso(day, 1)) all.push(day)
  const working = all.filter((day) => papersOn[day] || !isWeekend(day))
  if (working.length > MAX_FULL_DAYS) return working.filter((day) => papersOn[day])
  return all.reduce((days, day) => {
    if (working.includes(day)) days.push(day)
    else if (days.at(-1) !== null) days.push(null)
    return days
  }, [])
}
