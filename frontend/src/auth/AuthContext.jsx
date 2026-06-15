import { createContext, useContext, useEffect, useState } from 'react'
import { authApi } from '../api/endpoints'
import { AUTH_MODE, getKeycloak, roleFromToken } from './keycloak'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => (AUTH_MODE === 'jwt' ? localStorage.getItem('token') : null))
  const [user, setUser] = useState(() => {
    if (AUTH_MODE !== 'jwt') return null
    const u = localStorage.getItem('user')
    return u ? JSON.parse(u) : null
  })
  // jwt mode is ready immediately; keycloak mode becomes ready after init()
  const [ready, setReady] = useState(AUTH_MODE === 'jwt')

  // ---- Keycloak (OIDC) bootstrap ----
  useEffect(() => {
    if (AUTH_MODE !== 'keycloak') return
    const kc = getKeycloak()
    kc.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
      .then((authenticated) => {
        if (authenticated) {
          setToken(kc.token)
          setUser({
            userId: kc.tokenParsed.sub,
            email: kc.tokenParsed.email || kc.tokenParsed.preferred_username,
            role: roleFromToken(kc.tokenParsed),
          })
        }
      })
      .catch(() => {})
      .finally(() => setReady(true))
    // keep the token fresh
    kc.onTokenExpired = () => kc.updateToken(30).then(() => setToken(kc.token)).catch(() => kc.login())
  }, [])

  // ---- jwt mode persistence ----
  const persist = (data) => {
    const { accessToken, userId, email, role } = data
    const u = { userId, email, role }
    localStorage.setItem('token', accessToken)
    localStorage.setItem('user', JSON.stringify(u))
    setToken(accessToken)
    setUser(u)
  }

  const login = async (email, password) => {
    if (AUTH_MODE === 'keycloak') { getKeycloak().login(); return }
    const res = await authApi.login({ email, password })
    persist(res.data)
    return res.data
  }

  const register = async (email, password) => {
    if (AUTH_MODE === 'keycloak') { getKeycloak().register(); return }
    const res = await authApi.register({ email, password })
    persist(res.data)
    return res.data
  }

  const logout = () => {
    if (AUTH_MODE === 'keycloak') { getKeycloak().logout({ redirectUri: window.location.origin }); return }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  const isStaff = !!user && (user.role === 'ADMIN' || user.role === 'MANAGER')

  return (
    <AuthContext.Provider value={{ token, user, isStaff, login, register, logout, ready, mode: AUTH_MODE }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
