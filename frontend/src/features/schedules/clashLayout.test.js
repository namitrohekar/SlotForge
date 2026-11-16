import { describe, expect, it } from 'vitest'
import { clashLayout } from './clashLayout.js'

const entry = (examinationId, subjectCode, date, ...shares) => ({
  examinationId, subjectCode, subjectName: subjectCode, date, start: '10:00:00',
  conflicts: shares.map((other) => ({ examinationId: other, sharedStudents: 5 })),
})

describe('clashLayout', () => {
  it('puts each slot in its own column and stacks papers that share it', () => {
    const layout = clashLayout([entry(1, 'MA101', '2026-12-08', 2), entry(2, 'PH101', '2026-12-07', 1), entry(3, 'MCA301', '2026-12-07')])

    expect(layout.lanes.map((lane) => lane.date)).toEqual(['2026-12-07', '2026-12-08'])
    expect(layout.nodes.map((node) => `${node.code}@${node.lane}`)).toEqual(['MCA301@0', 'PH101@0', 'MA101@1'])
    expect(layout.nodes[1].y).toBeGreaterThan(layout.nodes[0].y)
  })

  it('draws each clash once and counts none inside a column for a valid timetable', () => {
    const layout = clashLayout([entry(1, 'MA101', '2026-12-08', 2), entry(2, 'PH101', '2026-12-07', 1)])

    expect(layout.edges).toHaveLength(1)
    expect(layout.clashesInOneSlot).toBe(0)
  })

  it('counts a clash that shares a column, which a broken timetable would have', () => {
    expect(clashLayout([entry(1, 'MA101', '2026-12-07', 2), entry(2, 'PH101', '2026-12-07', 1)]).clashesInOneSlot).toBe(1)
  })
})
