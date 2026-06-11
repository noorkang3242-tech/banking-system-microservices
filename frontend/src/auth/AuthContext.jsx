import { createContext, useContext, useState } from 'react'
import { authApi } from '../api/endpoints'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    const u = localStorage.getItem('user')
    return u ? JSON.parse(u) : null
  })

  const persist = (data) => {
    const { accessToken, userId, email, role } = data
    const u = { userId, email, role }
    localStorage.setItem('token', accessToken)
    localStorage.setItem('user', JSON.stringify(u))
    setToken(accessToken)
    setUser(u)
  }

  const login = async (email, password) => {
    const res = await authApi.login({ email, password })
    persist(res.data)
    return res.data
  }

  const register = async (email, password) => {
    const res = await authApi.register({ email, password })
    persist(res.data)
    return res.data
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  const isStaff = !!user && (user.role === 'ADMIN' || user.role === 'MANAGER')

  return (
    <AuthContext.Provider value={{ token, user, isStaff, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext)
