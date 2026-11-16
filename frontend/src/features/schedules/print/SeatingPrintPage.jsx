import { formatDay, formatTime } from '../../../utils/format.js'
import { listSeating } from '../services/schedulesApi.js'
import PrintSheet, { PrintTable } from './PrintSheet.jsx'
import PrintStates from './PrintStates.jsx'
import { useVersionDocument, versionNote } from './useVersionDocument.js'

// One page per hall of each paper. The empty last column makes it the attendance sheet too.
export default function SeatingPrintPage() {
  const { data, error, reload, back } = useVersionDocument(listSeating)

  if (!data?.version) return <PrintStates data={data} error={error} reload={reload} missing />

  return (
    <PrintSheet title="Seating lists" session={data.session.name} note={versionNote(data.version)} back={back}>
      {data.rows.length === 0 && <p className="text-callout text-ink-secondary">No student is seated in this version</p>}
      {data.rows.map((hall) => (
        <section key={`${hall.date}-${hall.start}-${hall.hallCode}`} className="mb-10 break-after-page last:break-after-auto">
          <h2 className="text-title-3">
            {hall.hallName} <span className="font-mono text-footnote font-normal">({hall.hallCode})</span>
          </h2>
          <p className="text-footnote text-ink-secondary">
            {hall.building} · {formatDay(hall.date)}, {formatTime(hall.start)} – {formatTime(hall.end)}
          </p>
          <p className="mb-2 text-footnote text-ink">
            <span className="font-mono text-caption">{hall.paperCode}</span> {hall.paperName} · {hall.students.length}{' '}
            {hall.students.length === 1 ? 'student' : 'students'}
          </p>
          <PrintTable caption={`${hall.hallCode}, ${hall.paperCode}`} headers={['No.', 'Roll number', 'Name', 'Signature']}>
            {hall.students.map((student, index) => (
              <tr key={student.rollNo}>
                <td className="w-10 font-mono text-caption">{index + 1}</td>
                <td className="w-32 font-mono text-caption">{student.rollNo}</td>
                <td>{student.fullName}</td>
                <td className="w-40" />
              </tr>
            ))}
          </PrintTable>
        </section>
      ))}
    </PrintSheet>
  )
}
