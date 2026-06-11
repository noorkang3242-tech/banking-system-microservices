import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

// Guards routes: needs a token; optionally needs staff role.
export default function ProtectedRoute({ children, staffOnly }) {
  const { token, isStaff } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  if (staffOnly && !isStaff) return <Navigate to="/dashboard" replace />
  return children
}
