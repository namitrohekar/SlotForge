import { api } from '../../../services/apiClient.js'

export const fetchHealth = () => api.get('/health').then((response) => response.data)
