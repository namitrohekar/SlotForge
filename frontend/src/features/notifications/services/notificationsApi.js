import { api } from '../../../services/apiClient.js'

export const fetchNotifications = () => api.get('/me/notifications').then((response) => response.data)
export const markNotificationsRead = () => api.post('/me/notifications/read')
