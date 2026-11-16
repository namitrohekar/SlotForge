import { useParams } from 'react-router'
import { useApi } from '../../../hooks/useApi.js'
import { formatDate, formatDay, formatTime } from '../../../utils/format.js'
import { listPublishedTimetables } from '../services/schedulesApi.js'
import PrintSheet, { PrintTable } from './PrintSheet.jsx'
import PrintStates from './PrintStates.jsx'

// The notice for the board: every paper of one published session, a day's name printed once.
export default function NoticePrintPage() {
  const { sessionId } = useParams()
  const { data, error, reload } = useApi(listPublishedTimetables)
  const timetable = data?.find((candidate) => String(candidate.sessionId) === sessionId)

  if (!timetable) return <PrintStates data={data} error={error} reload={reload} missing />

  return (
    <PrintSheet
      title="Examination timetable"
      session={timetable.sessionName}
      note={`${formatDate(timetable.startDate)} to ${formatDate(timetable.endDate)} · published ${formatDate(timetable.publishedAt)}`}
      back="/timetables"
    >
      <PrintTable caption="Examination timetable" headers={['Day', 'Time', 'Code', 'Paper', 'Halls']}>
        {timetable.papers.map((paper, index) => (
          <tr key={`${paper.date}-${paper.start}-${paper.paperCode}`}>
            <td className="font-medium whitespace-nowrap">{timetable.papers[index - 1]?.date === paper.date ? '' : formatDay(paper.date)}</td>
            <td className="font-mono text-caption whitespace-nowrap">
              {formatTime(paper.start)} – {formatTime(paper.end)}
            </td>
            <td className="font-mono text-caption whitespace-nowrap">{paper.paperCode}</td>
            <td>{paper.paperName}</td>
            <td className="font-mono text-caption">{paper.halls.join(', ')}</td>
          </tr>
        ))}
      </PrintTable>
    </PrintSheet>
  )
}
