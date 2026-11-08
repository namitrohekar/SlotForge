import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listUsers = (query) => api.get('/users', { params: query }).then(data)
export const createUser = (body) => api.post('/users', body).then(data)
export const renameUser = (id, fullName) => api.patch(`/users/${id}`, { fullName }).then(data)
export const changeRoles = (id, roles) => api.put(`/users/${id}/roles`, { roles }).then(data)
export const deactivateUser = (id) => api.post(`/users/${id}/deactivate`).then(data)
export const activateUser = (id) => api.post(`/users/${id}/activate`).then(data)
