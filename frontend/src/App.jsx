import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './components/AppLayout'
import WeatherBackground from './components/WeatherBackground'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transfer from './pages/Transfer'
import Transactions from './pages/Transactions'
import Loans from './pages/Loans'
import Cards from './pages/Cards'
import Notifications from './pages/Notifications'
import Admin from './pages/Admin'

export default function App() {
  return (
    <>
    <WeatherBackground />
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/transfer" element={<Transfer />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/loans" element={<Loans />} />
        <Route path="/cards" element={<Cards />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/admin" element={<ProtectedRoute staffOnly><Admin /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
    </>
  )
}
