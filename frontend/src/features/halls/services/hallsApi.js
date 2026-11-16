import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listHalls = (query) => api.get('/halls', { params: query }).then(data)
export const createHall = (body) => api.post('/halls', body).then(data)
export const updateHall = (id, body) => api.put(`/halls/${id}`, body).then(data)
export const deleteHall = (id) => api.delete(`/halls/${id}`)
export const listHallOutages = (hallId) => api.get(`/halls/${hallId}/unavailability`).then(data)
export const addHallOutage = (hallId, body) => api.post(`/halls/${hallId}/unavailability`, body).then(data)
export const removeHallOutage = (hallId, outageId) => api.delete(`/halls/${hallId}/unavailability/${outageId}`)
