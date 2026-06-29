import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Zap, Mail, Lock, Building2 } from 'lucide-react'
import { authAPI } from '../services/api'
import toast from 'react-hot-toast'

export default function Login({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false)
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    businessName: '',
    email: '',
    password: '',
    phone: '',
  })
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      if (isRegister) {
        const res = await authAPI.register(form)
        toast.success('Registration successful! Please login.')
        setIsRegister(false)
      } else {
        const res = await authAPI.login({
          email: form.email,
          password: form.password,
        })
        const data = res.data.data || res.data
        onLogin(data.accessToken, {
          id: data.merchantId,
          businessName: data.businessName,
          email: data.email,
        })
        toast.success('Welcome back!')
        navigate('/')
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Something went wrong'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  // Demo login for testing without backend
  const handleDemoLogin = () => {
    onLogin('demo-token', {
      id: 1,
      businessName: 'Demo Merchant',
      email: 'demo@payflow.com',
    })
    toast.success('Logged in with demo account')
    navigate('/')
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-logo">
          <div className="sidebar-logo-icon" style={{ width: 48, height: 48, fontSize: 22 }}>
            <Zap size={24} />
          </div>
          <div>
            <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: '-0.5px' }}>PayFlow</div>
            <div style={{ fontSize: 11, color: 'var(--accent-primary-light)', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '1.5px' }}>
              Gateway
            </div>
          </div>
        </div>

        <h2 className="login-title">
          {isRegister ? 'Create Account' : 'Welcome Back'}
        </h2>
        <p className="login-subtitle">
          {isRegister
            ? 'Start accepting payments today'
            : 'Sign in to your merchant dashboard'}
        </p>

        <form onSubmit={handleSubmit}>
          {isRegister && (
            <div className="form-group">
              <label className="form-label">
                <Building2 size={14} style={{ display: 'inline', marginRight: 6 }} />
                Business Name
              </label>
              <input
                className="form-input"
                type="text"
                placeholder="Your Business Name"
                value={form.businessName}
                onChange={(e) => setForm({ ...form, businessName: e.target.value })}
                required
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label">
              <Mail size={14} style={{ display: 'inline', marginRight: 6 }} />
              Email Address
            </label>
            <input
              className="form-input"
              type="email"
              placeholder="merchant@example.com"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <Lock size={14} style={{ display: 'inline', marginRight: 6 }} />
              Password
            </label>
            <input
              className="form-input"
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-login"
            disabled={loading}
            style={{ marginTop: 8 }}
          >
            {loading ? (
              <div className="spinner" style={{ width: 20, height: 20, borderWidth: 2 }} />
            ) : isRegister ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: 20 }}>
          <button
            className="btn btn-ghost"
            style={{ width: '100%', justifyContent: 'center', fontSize: 13 }}
            onClick={() => setIsRegister(!isRegister)}
          >
            {isRegister
              ? 'Already have an account? Sign In'
              : "Don't have an account? Register"}
          </button>
        </div>

        <div style={{ textAlign: 'center', marginTop: 12 }}>
          <button
            className="btn btn-ghost btn-sm"
            style={{ color: 'var(--accent-primary-light)', justifyContent: 'center', width: '100%' }}
            onClick={handleDemoLogin}
          >
            <Zap size={14} />
            Continue with Demo Account
          </button>
        </div>
      </div>
    </div>
  )
}
