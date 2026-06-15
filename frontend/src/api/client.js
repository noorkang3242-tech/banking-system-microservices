import axios from 'axios'
import { AUTH_MODE, getKeycloak } from '../auth/keycloak'

// Single axios instance pointing at the API gateway. Every backend service is
// reached through here (http://localhost:8090 locally, or the public URL).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090',
})

// Attach the access token on every request (Keycloak token in keycloak mode,
// the stored JWT otherwise).
api.interceptors.request.use(async (config) => {
  if (AUTH_MODE === 'keycloak') {
    const kc = getKeycloak()
    if (kc && kc.token) {
      try { await kc.updateToken(30) } catch { /* refresh failed; request will 401 */ }
      config.headers.Authorization = `Bearer ${kc.token}`
    }
  } else {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401 (expired/invalid token) clear session and bounce to login.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      if (AUTH_MODE === 'keycloak') {
        const kc = getKeycloak()
        if (kc) kc.login()
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

export default api
