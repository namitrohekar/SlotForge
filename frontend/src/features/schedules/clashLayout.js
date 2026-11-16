export const LANE = 132
export const NODE = { width: 104, height: 28, gap: 10 }
export const HEADER = 44

const slotKey = (entry) => `${entry.date}|${entry.start}`

/** Papers laid out in one column per slot, with a line for every pair that shares students. */
export function clashLayout(entries) {
  const lanes = [...new Set(entries.map(slotKey))].sort()
  const filled = lanes.map(() => 0)
  const nodes = entries
    .slice()
    .sort((a, b) => slotKey(a).localeCompare(slotKey(b)) || a.subjectCode.localeCompare(b.subjectCode))
    .map((entry) => {
      const lane = lanes.indexOf(slotKey(entry))
      const row = filled[lane]++
      return {
        id: entry.examinationId,
        code: entry.subjectCode,
        name: entry.subjectName,
        lane,
        x: lane * LANE + (LANE - NODE.width) / 2,
        y: HEADER + row * (NODE.height + NODE.gap),
        neighbours: entry.conflicts.map((conflict) => conflict.examinationId),
      }
    })
  const byId = new Map(nodes.map((node) => [node.id, node]))
  const edges = nodes.flatMap((node) =>
    node.neighbours.filter((other) => other > node.id && byId.has(other)).map((other) => ({ from: node, to: byId.get(other) })),
  )
  return {
    lanes: lanes.map((key) => ({ date: key.split('|')[0], start: key.split('|')[1] })),
    nodes,
    edges,
    width: Math.max(lanes.length, 1) * LANE,
    height: HEADER + Math.max(...filled, 1) * (NODE.height + NODE.gap),
    // A timetable is clash-free exactly when no line stays inside one column.
    clashesInOneSlot: edges.filter((edge) => edge.from.lane === edge.to.lane).length,
  }
}
