import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listSessions = (query) => api.get('/exam-sessions', { params: query }).then(data)
export const getSession = (id) => api.get(`/exam-sessions/${id}`).then(data)
export const createSession = (body) => api.post('/exam-sessions', body).then(data)
export const updateSession = (id, body) => api.put(`/exam-sessions/${id}`, body).then(data)
export const changeSessionStatus = (id, status) => api.patch(`/exam-sessions/${id}/status`, { status }).then(data)
export const deleteSession = (id) => api.delete(`/exam-sessions/${id}`)

export const listSlots = (sessionId) => api.get(`/exam-sessions/${sessionId}/time-slots`).then(data)
export const generateSlots = (sessionId, body) => api.post(`/exam-sessions/${sessionId}/time-slots/generate`, body).then(data)
export const changeSlotStatus = (id, body) => api.patch(`/time-slots/${id}/status`, body).then(data)
export const deleteSlot = (id) => api.delete(`/time-slots/${id}`)

export const listExaminations = (sessionId, query) =>
  api.get(`/exam-sessions/${sessionId}/examinations`, { params: query }).then(data)
export const createFromCurriculum = (sessionId, body) =>
  api.post(`/exam-sessions/${sessionId}/examinations/from-curriculum`, body).then(data)
export const updateExamination = (id, body) => api.put(`/examinations/${id}`, body).then(data)
export const deleteExamination = (id) => api.delete(`/examinations/${id}`)

export const listEnrolments = (id, query) => api.get(`/examinations/${id}/enrolments`, { params: query }).then(data)
export const addEnrolments = (id, studentIds) => api.post(`/examinations/${id}/enrolments`, { studentIds }).then(data)
export const removeEnrolment = (id, studentId) => api.delete(`/examinations/${id}/enrolments/${studentId}`)

export const importEnrolments = (sessionId, file) => {
  const body = new FormData()
  body.append('file', file)
  return api.post(`/exam-sessions/${sessionId}/enrolments/import`, body).then(data)
}
