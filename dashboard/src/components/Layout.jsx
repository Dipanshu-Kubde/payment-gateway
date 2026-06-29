import { useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, ArrowLeftRight, ShieldAlert,
  Landmark, LogOut, Zap, Settings
} from 'lucide-react'

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  { path: '/fraud', label: 'Fraud Alerts', icon: ShieldAlert },
  { path: '/settlements', label: 'Settlements', icon: Landmark },
]

export default function Layout({ children, merchant, onLogout }) {
  const location = useLocation()
  const navigate = useNavigate()

  const pageTitle = navItems.find(item => item.path === location.pathname)?.label || 'Dashboard'

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="sidebar-logo">
            <div className="sidebar-logo-icon">
              <Zap size={20} />
            </div>
            <div>
              <div className="sidebar-logo-text">PayFlow</div>
              <div className="sidebar-logo-badge">Gateway</div>
            </div>
          </div>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section-title">Menu</div>
          {navItems.map((item) => (
            <button
              key={item.path}
              className={`nav-link ${location.pathname === item.path ? 'active' : ''}`}
              onClick={() => navigate(item.path)}
            >
              <item.icon size={18} />
              {item.label}
            </button>
          ))}

          <div style={{ flex: 1 }} />

          <div className="nav-section-title">Account</div>
          <div style={{ padding: '8px 16px', marginBottom: '8px' }}>
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>
              {merchant?.businessName || 'Merchant'}
            </div>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {merchant?.email || ''}
            </div>
          </div>
          <button className="nav-link" onClick={onLogout}>
            <LogOut size={18} />
            Sign Out
          </button>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="top-bar">
          <h1 className="top-bar-title">{pageTitle}</h1>
          <div className="top-bar-actions">
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/settings')}>
              <Settings size={16} />
              Settings
            </button>
          </div>
        </header>
        <div className="page-content animate-in">
          {children}
        </div>
      </main>
    </div>
  )
}
