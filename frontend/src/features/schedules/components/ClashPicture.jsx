import { useState } from 'react'
import { formatDay, formatTime } from '../../../utils/format.js'
import { clashLayout, HEADER, LANE, NODE } from '../clashLayout.js'

const centre = (node) => ({ x: node.x + NODE.width / 2, y: node.y + NODE.height / 2 })

// One column per slot, one line per pair of papers that share students. Valid means no line stays inside a column.
export default function ClashPicture({ entries }) {
  const [focused, setFocused] = useState(null)
  const layout = clashLayout(entries)
  const related = (node) => focused === null || node.id === focused.id || focused.neighbours.includes(node.id)

  return (
    <figure>
      <div className="overflow-x-auto" onMouseLeave={() => setFocused(null)}>
        <svg
          role="group"
          aria-label={`Clash picture: ${layout.nodes.length} papers in ${layout.lanes.length} slots, ${layout.edges.length} pairs share students`}
          viewBox={`0 0 ${layout.width} ${layout.height}`}
          style={{ minWidth: layout.width * 0.72 }}
          className="w-full"
        >
          {layout.lanes.map((lane, index) => (
            <g key={`${lane.date}${lane.start}`}>
              {index > 0 && <line x1={index * LANE} y1={0} x2={index * LANE} y2={layout.height} className="stroke-line" />}
              <text x={index * LANE + LANE / 2} y={16} textAnchor="middle" className="fill-ink text-caption font-medium">
                {formatDay(lane.date)}
              </text>
              <text x={index * LANE + LANE / 2} y={31} textAnchor="middle" className="fill-ink-tertiary font-mono text-caption">
                {formatTime(lane.start)}
              </text>
            </g>
          ))}
          <line x1={0} y1={HEADER - 6} x2={layout.width} y2={HEADER - 6} className="stroke-ink" />

          {layout.edges.map((edge) => {
            const from = centre(edge.from)
            const to = centre(edge.to)
            const lit = focused && (edge.from.id === focused.id || edge.to.id === focused.id)
            // Bowed, so lines between the same two columns do not hide each other.
            const bow = (to.y - from.y) / 4 - 14
            return (
              <path
                key={`${edge.from.id}-${edge.to.id}`}
                d={`M ${from.x} ${from.y} Q ${(from.x + to.x) / 2} ${(from.y + to.y) / 2 + bow} ${to.x} ${to.y}`}
                fill="none"
                strokeWidth={lit ? 1.5 : 1}
                className={lit ? 'stroke-tick' : focused ? 'stroke-line opacity-40' : 'stroke-line-strong opacity-40'}
              />
            )
          })}

          {layout.nodes.map((node) => (
            <g
              key={node.id}
              tabIndex={0}
              role="img"
              aria-label={`${node.code} ${node.name}, shares students with ${node.neighbours.length} ${node.neighbours.length === 1 ? 'paper' : 'papers'}`}
              onMouseEnter={() => setFocused(node)}
              onFocus={() => setFocused(node)}
              onBlur={() => setFocused(null)}
              className={`cursor-default outline-none transition-opacity duration-(--duration-fast) ${related(node) ? '' : 'opacity-30'}`}
            >
              <rect
                x={node.x}
                y={node.y}
                width={NODE.width}
                height={NODE.height}
                rx={4}
                className={`fill-surface ${focused?.id === node.id ? 'stroke-tick' : 'stroke-line-strong'}`}
              />
              <text x={node.x + NODE.width / 2} y={node.y + NODE.height / 2 + 4} textAnchor="middle" className="fill-ink font-mono text-caption">
                {node.code}
              </text>
            </g>
          ))}
        </svg>
      </div>
      <figcaption className="mt-3 text-footnote text-ink-tertiary">
        {layout.clashesInOneSlot === 0
          ? `Each line joins two papers that share students. No line stays inside one column, so no student has two papers at once. Point at a paper to see its ${layout.edges.length === 1 ? 'clash' : 'clashes'}.`
          : `${layout.clashesInOneSlot} ${layout.clashesInOneSlot === 1 ? 'line stays' : 'lines stay'} inside one column: those papers share students and are in the same slot.`}
      </figcaption>
    </figure>
  )
}
