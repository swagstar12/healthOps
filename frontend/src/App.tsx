import React from 'react'
import { Routes, Route, Navigate, Link } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import Login from './pages/Login'
import AdminDashboard from './pages/AdminDashboard'
import DoctorDashboard from './pages/DoctorDashboard'
import ReceptionDashboard from './pages/ReceptionDashboard'

const Protected: React.FC<{children:React.ReactNode, roles?: string[]}> = ({children, roles}) => {
  const {user} = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <div className="p-8">Not authorized</div>
  return <>{children}</>
}

const Nav: React.FC = () => {
  const {user, logout} = useAuth()
  return (
    <div className="left 0 flex items-center justify-between itmes-center px-10 py-6 z-20 bg-white shadow mt-16">
      <Link to="/" className="font-bold">HealthOps System</Link>
      <div className="flex gap-4 items-center">
        {user && <span className="text-sm">{user.fullName} — {user.role}</span>}
        {user ? <button className="btn" onClick={logout}>Logout</button> : <Link className="btn" to="/login">Login</Link>}
      </div>
    </div>
  )
}

const Home: React.FC = () => {
  const {user} = useAuth()
  if (!user) return <Navigate to="/login" />
  if (user.role === 'ADMIN') return <Navigate to="/admin" />
  if (user.role === 'DOCTOR') return <Navigate to="/doctor" />
  return <Navigate to="/reception" />
}

export default function App(){
  return (
    <AuthProvider>
      <Nav />
      <div className="max-w-6xl mx-auto p-6">
        <Routes>
          <Route path="/" element={<Home/>} />
          <Route path="/login" element={<Login/>} />
          <Route path="/admin" element={<Protected roles={['ADMIN']}><AdminDashboard/></Protected>} />
          <Route path="/doctor" element={<Protected roles={['DOCTOR','ADMIN']}><DoctorDashboard/></Protected>} />
          <Route path="/reception" element={<Protected roles={['RECEPTIONIST','ADMIN']}><ReceptionDashboard/></Protected>} />
        </Routes>
      </div>
    </AuthProvider>
  )
}
