import { useState, useEffect } from 'react'
import { ShieldAlert, ShieldCheck, ShieldX, RefreshCw, CheckCircle, XCircle } from 'lucide-react'
import { fraudAPI } from '../services/api'
import toast from 'react-hot-toast'

const demoFlaggedTxns = [
  { transactionId: 'TXN-FRAUD-001', orderId: 'ORD-F01', merchantId: 1, amount: 85000, riskScore: 78, riskLevel: 'HIGH', triggeredRules: 'HIGH_AMOUNT,VELOCITY_CHECK,UNUSUAL_HOUR', recommendation: 'High risk detected. Transaction blocked.', blocked: true, reviewedBy: null, createdAt: '2026-06-29T03:15:00' },
  { transactionId: 'TXN-FRAUD-002', orderId: 'ORD-F02', merchantId: 1, amount: 150000, riskScore: 92, riskLevel: 'CRITICAL', triggeredRules: 'HIGH_AMOUNT,GEO_MISMATCH,FIRST_TIME_HIGH,SUSPICIOUS_PATTERN', recommendation: 'Critical fraud indicators. Auto-blocked.', blocked: true, reviewedBy: null, createdAt: '2026-06-29T02:45:00' },
  { transactionId: 'TXN-FRAUD-003', orderId: 'ORD-F03', merchantId: 1, amount: 42000, riskScore: 55, riskLevel: 'MEDIUM', triggeredRules: 'VELOCITY_CHECK,UNUSUAL_HOUR', recommendation: 'Moderate risk. Additional verification suggested.', blocked: false, reviewedBy: null, createdAt: '2026-06-29T04:30:00' },
  { transactionId: 'TXN-FRAUD-004', orderId: 'ORD-F04', merchantId: 1, amount: 95000, riskScore: 85, riskLevel: 'CRITICAL', triggeredRules: 'HIGH_AMOUNT,FAILED_VELOCITY,GEO_MISMATCH,HIGH_RISK_COUNTRY', recommendation: 'Auto-blocked. Immediate investigation required.', blocked: true, reviewedBy: null, createdAt: '2026-06-28T23:10:00' },
]

const demoStats = {
  totalChecks: 12847,
  totalBlocked: 187,
  lowRisk: 11234,
  mediumRisk: 1089,
  highRisk: 337,
  criticalRisk: 187,
  blockRate: 1.46,
}

const demoRules = [
  { name: 'HIGH_AMOUNT', description: 'Flags transactions exceeding ₹50,000', maxScore: 15 },
  { name: 'VELOCITY_CHECK', description: '> 5 transactions in 10 minutes', maxScore: 20 },
  { name: 'FAILED_ATTEMPT_VELOCITY', description: '> 3 failures in 5 minutes', maxScore: 15 },
  { name: 'GEO_LOCATION_MISMATCH', description: 'IP country ≠ card country', maxScore: 15 },
  { name: 'HIGH_RISK_COUNTRY', description: 'Origin from sanctioned country', maxScore: 10 },
  { name: 'UNUSUAL_HOUR', description: 'Transaction at 2-5 AM', maxScore: 5 },
  { name: 'FIRST_TIME_HIGH_AMOUNT', description: 'New customer + > ₹20,000', maxScore: 10 },
  { name: 'SUSPICIOUS_PATTERN', description: 'Round amounts, threshold testing', maxScore: 10 },
]

const getRiskBadge = (level) => {
  const map = {
    LOW: 'badge-success',
    MEDIUM: 'badge-warning',
    HIGH: 'badge-danger',
    CRITICAL: 'badge-danger',
  }
  return map[level] || 'badge-info'
}

export default function FraudAlerts({ merchant }) {
  const [flagged, setFlagged] = useState(demoFlaggedTxns)
  const [stats, setStats] = useState(demoStats)
  const [rules, setRules] = useState(demoRules)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [flaggedRes, statsRes, rulesRes] = await Promise.allSettled([
        fraudAPI.getFlagged({ page: 0, size: 20 }),
        fraudAPI.getStats(),
        fraudAPI.getRules(),
      ])
      if (flaggedRes.status === 'fulfilled' && flaggedRes.value.data?.data?.content) {
        setFlagged(flaggedRes.value.data.data.content)
      }
      if (statsRes.status === 'fulfilled' && statsRes.value.data?.data) {
        setStats(statsRes.value.data.data)
      }
      if (rulesRes.status === 'fulfilled' && rulesRes.value.data?.data) {
        setRules(rulesRes.value.data.data)
      }
    } catch (err) {
      console.log('Using demo fraud data')
    } finally {
      setLoading(false)
    }
  }

  const handleApprove = async (txnId) => {
    try {
      await fraudAPI.approve(txnId)
      toast.success(`Transaction ${txnId} approved`)
      setFlagged(prev => prev.map(t =>
        t.transactionId === txnId
          ? { ...t, blocked: false, reviewedBy: 'ADMIN' }
          : t
      ))
    } catch (err) {
      toast.error('Failed to approve')
    }
  }

  const handleReject = async (txnId) => {
    try {
      await fraudAPI.reject(txnId)
      toast.success(`Transaction ${txnId} rejected`)
      setFlagged(prev => prev.map(t =>
        t.transactionId === txnId
          ? { ...t, blocked: true, reviewedBy: 'ADMIN' }
          : t
      ))
    } catch (err) {
      toast.error('Failed to reject')
    }
  }

  return (
    <div>
      {/* Fraud Stats KPI */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Total Checks</span>
            <div className="kpi-icon accent"><ShieldAlert size={20} /></div>
          </div>
          <div className="kpi-value">{stats.totalChecks?.toLocaleString()}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Blocked</span>
            <div className="kpi-icon danger"><ShieldX size={20} /></div>
          </div>
          <div className="kpi-value" style={{ color: 'var(--danger)' }}>{stats.totalBlocked}</div>
          <div className="kpi-change negative">{stats.blockRate?.toFixed(2)}% block rate</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">High Risk</span>
            <div className="kpi-icon warning"><ShieldAlert size={20} /></div>
          </div>
          <div className="kpi-value" style={{ color: 'var(--warning)' }}>{stats.highRisk}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Safe Transactions</span>
            <div className="kpi-icon success"><ShieldCheck size={20} /></div>
          </div>
          <div className="kpi-value" style={{ color: 'var(--success)' }}>{stats.lowRisk?.toLocaleString()}</div>
        </div>
      </div>

      {/* Active Rules */}
      <div className="chart-card" style={{ marginBottom: 20 }}>
        <div className="chart-card-header">
          <h3 className="chart-title">Active Fraud Detection Rules</h3>
          <span className="badge badge-success">
            <span className="badge-dot" />
            {rules.length} Rules Active
          </span>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 12, padding: '0 0 8px' }}>
          {rules.map((rule, idx) => (
            <div key={idx} style={{
              padding: '12px 16px',
              background: 'var(--bg-secondary)',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border)',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>{rule.name}</span>
                <span style={{
                  fontSize: 12,
                  fontWeight: 700,
                  color: 'var(--accent-primary-light)',
                  background: 'rgba(99, 102, 241, 0.1)',
                  padding: '2px 8px',
                  borderRadius: 10,
                }}>
                  +{rule.maxScore}
                </span>
              </div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{rule.description}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Flagged Transactions */}
      <div className="table-card">
        <div className="table-header">
          <h3 className="table-title">⚠️ Flagged Transactions</h3>
          <button className="btn btn-ghost btn-sm" onClick={loadData}>
            <RefreshCw size={14} /> Refresh
          </button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Transaction</th>
              <th>Amount</th>
              <th>Risk Score</th>
              <th>Risk Level</th>
              <th>Triggered Rules</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {flagged.map((txn, idx) => (
              <tr key={txn.transactionId || idx}>
                <td>
                  <span style={{ fontFamily: 'monospace', fontSize: 12, color: 'var(--text-primary)' }}>
                    {txn.transactionId}
                  </span>
                </td>
                <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                  ₹{txn.amount?.toLocaleString('en-IN')}
                </td>
                <td>
                  <span style={{
                    fontSize: 18,
                    fontWeight: 800,
                    color: txn.riskScore >= 80 ? 'var(--danger)' : txn.riskScore >= 60 ? '#f97316' : 'var(--warning)',
                  }}>
                    {txn.riskScore}
                  </span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>/100</span>
                </td>
                <td>
                  <span className={`badge ${getRiskBadge(txn.riskLevel)}`}>
                    <span className="badge-dot" />
                    {txn.riskLevel}
                  </span>
                </td>
                <td style={{ maxWidth: 200 }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                    {txn.triggeredRules?.split(',').map((rule, i) => (
                      <span key={i} style={{
                        fontSize: 10,
                        padding: '2px 6px',
                        background: 'var(--bg-secondary)',
                        border: '1px solid var(--border)',
                        borderRadius: 4,
                        color: 'var(--text-secondary)',
                      }}>
                        {rule.trim()}
                      </span>
                    ))}
                  </div>
                </td>
                <td>
                  {txn.reviewedBy ? (
                    <span className={`badge ${txn.blocked ? 'badge-danger' : 'badge-success'}`}>
                      {txn.blocked ? 'Rejected' : 'Approved'}
                    </span>
                  ) : (
                    <span className="badge badge-warning">Pending Review</span>
                  )}
                </td>
                <td>
                  {!txn.reviewedBy && (
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        className="btn btn-success btn-sm"
                        onClick={() => handleApprove(txn.transactionId)}
                        title="Approve"
                      >
                        <CheckCircle size={14} />
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleReject(txn.transactionId)}
                        title="Reject"
                      >
                        <XCircle size={14} />
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {flagged.length === 0 && (
          <div className="empty-state">
            <ShieldCheck size={48} style={{ color: 'var(--success)', opacity: 0.5, marginBottom: 16 }} />
            <div className="empty-state-title">All Clear!</div>
            <p style={{ color: 'var(--text-muted)' }}>No flagged transactions at this time</p>
          </div>
        )}
      </div>
    </div>
  )
}
