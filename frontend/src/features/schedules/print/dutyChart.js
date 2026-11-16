/** One row per hall of each paper, in the order the day runs. */
export function dutyRows(entries) {
  return entries
    .flatMap((entry) => entry.halls.map((hall) => ({ ...hall, date: entry.date, start: entry.start, end: entry.end, paperCode: entry.subjectCode })))
    .sort((a, b) => `${a.date}${a.start}${a.code}`.localeCompare(`${b.date}${b.start}${b.code}`))
}

/** Each invigilator with their duties in date order, busiest first, so an uneven load is easy to spot. */
export function dutiesByPerson(rows) {
  const duties = new Map()
  rows.forEach((row) => row.invigilators.forEach((name) => duties.set(name, [...(duties.get(name) ?? []), row])))
  return [...duties].map(([name, list]) => ({ name, duties: list })).sort((a, b) => b.duties.length - a.duties.length || a.name.localeCompare(b.name))
}
