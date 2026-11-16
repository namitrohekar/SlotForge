import { formatDay, formatTime } from '../../../utils/format.js'
import { listEntries } from '../services/schedulesApi.js'
import { dutiesByPerson, dutyRows } from './dutyChart.js'
import PrintSheet, { PrintTable } from './PrintSheet.jsx'
import PrintStates from './PrintStates.jsx'
import { useVersionDocument, versionNote } from './useVersionDocument.js'

const when = (row) => `${formatDay(row.date)}, ${formatTime(row.start)}`

// Two readings of the same duties: by hall for the exam office, by person for the staff room.
export default function DutyChartPrintPage() {
  const { data, error, reload, back } = useVersionDocument(listEntries)

  if (!data?.version) return <PrintStates data={data} error={error} reload={reload} missing />

  const rows = dutyRows(data.rows)

  return (
    <PrintSheet title="Invigilation duty chart" session={data.session.name} note={versionNote(data.version)} back={back}>
      <h2 className="mb-2 text-title-3">By hall</h2>
      <PrintTable caption="Duties by hall" headers={['Day', 'Time', 'Hall', 'Paper', 'Students', 'Invigilators']}>
        {rows.map((row, index) => (
          <tr key={`${row.date}-${row.start}-${row.code}`}>
            <td className="font-medium whitespace-nowrap">{rows[index - 1]?.date === row.date ? '' : formatDay(row.date)}</td>
            <td className="font-mono text-caption whitespace-nowrap">
              {formatTime(row.start)} – {formatTime(row.end)}
            </td>
            <td className="font-mono text-caption whitespace-nowrap">{row.code}</td>
            <td className="font-mono text-caption whitespace-nowrap">{row.paperCode}</td>
            <td data-numeric>{row.seats}</td>
            <td>{row.invigilators.length ? row.invigilators.join(', ') : 'Nobody assigned'}</td>
          </tr>
        ))}
      </PrintTable>

      <h2 className="mt-10 mb-2 break-before-page text-title-3">By invigilator</h2>
      <PrintTable caption="Duties by invigilator" headers={['Invigilator', 'Duties', 'When and where']}>
        {dutiesByPerson(rows).map((person) => (
          <tr key={person.name}>
            <td className="font-medium whitespace-nowrap">{person.name}</td>
            <td data-numeric>{person.duties.length}</td>
            <td>{person.duties.map((duty) => `${when(duty)} in ${duty.code}`).join(' · ')}</td>
          </tr>
        ))}
      </PrintTable>
    </PrintSheet>
  )
}
