import { api } from '../../../services/apiClient.js'

const data = (response) => response.data

export const listDepartments = (query) => api.get('/departments', { params: query }).then(data)
// Departments are few (VJTI has twelve), so pickers load them all at once.
export const listAllDepartments = () =>
  listDepartments({ size: 100, sort: 'name,asc' }).then((page) => page.items)
export const createDepartment = (body) => api.post('/departments', body).then(data)
export const updateDepartment = (id, body) => api.put(`/departments/${id}`, body).then(data)
export const deleteDepartment = (id) => api.delete(`/departments/${id}`)

export const listCourses = (query) => api.get('/courses', { params: query }).then(data)
export const createCourse = (body) => api.post('/courses', body).then(data)
export const updateCourse = (id, body) => api.put(`/courses/${id}`, body).then(data)
export const deleteCourse = (id) => api.delete(`/courses/${id}`)
export const listAllCourses = () => listCourses({ size: 100, sort: 'name,asc' }).then((page) => page.items)

export const listSubjects = (query) => api.get('/subjects', { params: query }).then(data)
export const createSubject = (body) => api.post('/subjects', body).then(data)
export const updateSubject = (id, body) => api.put(`/subjects/${id}`, body).then(data)
export const deleteSubject = (id) => api.delete(`/subjects/${id}`)

export const listCurricula = (query) => api.get('/curricula', { params: query }).then(data)
export const getCurriculum = (id) => api.get(`/curricula/${id}`).then(data)
export const createCurriculum = (body) => api.post('/curricula', body).then(data)
export const updateCurriculum = (id, body) => api.put(`/curricula/${id}`, body).then(data)
export const deleteCurriculum = (id) => api.delete(`/curricula/${id}`)
export const listCurriculumSubjects = (id) => api.get(`/curricula/${id}/subjects`).then(data)
export const replaceCurriculumSubjects = (id, subjects) =>
  api.put(`/curricula/${id}/subjects`, { subjects }).then(data)
export const listAllCurricula = () => listCurricula({ size: 100, sort: 'name,asc' }).then((page) => page.items)

export const listBatches = (query) => api.get('/batches', { params: query }).then(data)
export const listAllBatches = () => listBatches({ size: 100, sort: 'name,asc' }).then((page) => page.items)
export const createBatch = (body) => api.post('/batches', body).then(data)
export const updateBatch = (id, body) => api.put(`/batches/${id}`, body).then(data)
export const deleteBatch = (id) => api.delete(`/batches/${id}`)
