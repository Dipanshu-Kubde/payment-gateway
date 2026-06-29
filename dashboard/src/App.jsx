import { Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import FraudAlerts from './pages/FraudAlerts'
import Settlements from './pages/Settlements'
import Layout from './components/Layout'

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [merchant, setMerchant] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('token')
    const storedMerchant = localStorage.getItem('merchant')
    if (token && storedMerchant) {
      setIsAuthenticated(true)
      setMerchant(JSON.parse(storedMerchant))
    }
  }, [])

  const handleLogin = (token, merchantData) => {
    localStorage.setItem('token', token)
    localStorage.setItem('merchant', JSON.stringify(merchantData))
    setIsAuthenticated(true)
    setMerchant(merchantData)
  }

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('merchant')
    setIsAuthenticated(false)
    setMerchant(null)
  }

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={handleLogin} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Layout merchant={merchant} onLogout={handleLogout}>
      <Routes>
        <Route path="/" element={<Dashboard merchant={merchant} />} />
        <Route path="/transactions" element={<Transactions merchant={merchant} />} />
        <Route path="/fraud" element={<FraudAlerts merchant={merchant} />} />
        <Route path="/settlements" element={<Settlements merchant={merchant} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}

export default App
