import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listStudents = (query) => api.get('/students', { params: query }).then(data)
export const createStudent = (body) => api.post('/students', body).then(data)
export const updateStudent = (id, body) => api.put(`/students/${id}`, body).then(data)
export const deleteStudent = (id) => api.delete(`/students/${id}`)
