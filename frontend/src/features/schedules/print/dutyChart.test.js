import { describe, expect, it } from 'vitest'
import { dutiesByPerson, dutyRows } from './dutyChart.js'

const hall = (code, ...invigilators) => ({ code, name: code, building: 'Main', seats: 30, capacity: 40, invigilators })
const entries = [
  { date: '2026-12-08', start: '10:00:00', end: '13:00:00', subjectCode: 'PH101', halls: [hall('MB-101', 'Asha')] },
  { date: '2026-12-07', start: '10:00:00', end: '13:00:00', subjectCode: 'MA101', halls: [hall('MB-102', 'Ravi'), hall('AUD-1', 'Asha', 'Ravi')] },
]

describe('duty chart', () => {
  it('lists every hall of every paper in the order the days run', () => {
    expect(dutyRows(entries).map((row) => `${row.date} ${row.code} ${row.paperCode}`)).toEqual([
      '2026-12-07 AUD-1 MA101',
      '2026-12-07 MB-102 MA101',
      '2026-12-08 MB-101 PH101',
    ])
  })

  it('groups duties by person, busiest first and then by name', () => {
    const people = dutiesByPerson(dutyRows(entries))

    expect(people.map((person) => `${person.name} ${person.duties.length}`)).toEqual(['Asha 2', 'Ravi 2'])
    expect(people[0].duties.map((duty) => duty.code)).toEqual(['AUD-1', 'MB-101'])
  })
})
