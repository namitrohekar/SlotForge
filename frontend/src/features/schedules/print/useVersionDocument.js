import { useCallback } from 'react'
import { useParams } from 'react-router'
import { useApi } from '../../../hooks/useApi.js'
import { getSession } from '../../exams/services/examsApi.js'
import { listVersions } from '../services/schedulesApi.js'

const STATES = {
  DRAFT: 'draft, not yet approved or published',
  REVIEWED: 'approved, not yet published',
  PUBLISHED: 'published',
  SUPERSEDED: 'replaced by a newer timetable',
}

/** "Version 2, published": a printed draft must say on the paper that it is one. */
export const versionNote = (version) => `Version ${version.versionNo}, ${STATES[version.status]}`

/** The session, the version and that version's rows, for a sheet printed from one timetable version. `load` must be stable. */
export function useVersionDocument(load) {
  const { sessionId, versionId } = useParams()
  const loaded = useApi(
    useCallback(async () => {
      const [session, versions, rows] = await Promise.all([getSession(sessionId), listVersions(sessionId), load(versionId)])
      return { session, rows, version: versions.find((version) => String(version.id) === versionId) }
    }, [sessionId, versionId, load]),
  )
  return { ...loaded, back: `/sessions/${sessionId}/timetable?version=${versionId}` }
}
