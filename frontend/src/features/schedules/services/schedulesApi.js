import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const generateTimetable = (sessionId) => api.post(`/exam-sessions/${sessionId}/planning/runs`).then(data)
export const listVersions = (sessionId) => api.get(`/exam-sessions/${sessionId}/schedule-versions`).then(data)
export const listEntries = (versionId) => api.get(`/schedule-versions/${versionId}/entries`).then(data)
export const listViolations = (versionId) => api.get(`/schedule-versions/${versionId}/violations`).then(data)
export const deleteVersion = (versionId) => api.delete(`/schedule-versions/${versionId}`)
export const listAwaitingApproval = () => api.get('/schedule-versions/awaiting-approval').then(data)
export const submitVersion = (versionId) => api.post(`/schedule-versions/${versionId}/submit`)
export const approveVersion = (versionId) => api.post(`/schedule-versions/${versionId}/review`)
export const returnVersion = (versionId, comment) => api.post(`/schedule-versions/${versionId}/return`, { comment })
export const listDutiesAtRisk = () => api.get('/schedule-versions/duties-at-risk').then(data)
export const listMyDuties = () => api.get('/me/duties').then(data)
export const listMyUnavailability = () => api.get('/me/unavailability').then(data)
export const addUnavailability = (body) => api.post('/me/unavailability', body).then(data)
export const removeUnavailability = (id) => api.delete(`/me/unavailability/${id}`)
export const publishVersion = (versionId) => api.post(`/schedule-versions/${versionId}/publish`)
