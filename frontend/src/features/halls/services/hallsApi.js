import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listHalls = (query) => api.get('/halls', { params: query }).then(data)
export const createHall = (body) => api.post('/halls', body).then(data)
export const updateHall = (id, body) => api.put(`/halls/${id}`, body).then(data)
export const deleteHall = (id) => api.delete(`/halls/${id}`)
