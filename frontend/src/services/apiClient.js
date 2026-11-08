import axios from 'axios'

// The only Axios instance. Axios sends the XSRF-TOKEN cookie back as a header on same-origin requests.
export const api = axios.create({ baseURL: '/api' })
