import { api } from '../../../services/apiClient.js'

export const fetchAuthOptions = () => api.get('/auth/options').then((response) => response.data)
export const fetchCurrentUser = () => api.get('/auth/me').then((response) => response.data)
export const fetchDemoUsers = () => api.get('/auth/demo-users').then((response) => response.data)
export const demoLogin = (userId) => api.post('/auth/demo-login', { userId }).then((response) => response.data)
export const logout = () => api.post('/auth/logout')
