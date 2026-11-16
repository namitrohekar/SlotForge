import { api } from '../../../services/apiClient.js'

export const listAuditLogs = (query) => api.get('/audit-logs', { params: query }).then((response) => response.data)
