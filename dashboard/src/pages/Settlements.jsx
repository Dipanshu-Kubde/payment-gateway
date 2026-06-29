import { useState, useEffect } from 'react'
import { Landmark, Wallet, ArrowUpRight, RefreshCw, Zap } from 'lucide-react'
import { settlementAPI } from '../services/api'
import toast from 'react-hot-toast'

const demoBalance = {
  merchantId: 1,
  totalEarnings: 4523890.50,
  settledAmount: 3890000.00,
  pendingAmount: 633890.50,
  lastSettlementDate: '2026-06-28',
}

const demoSettlements = [
  { settlementId: 'STL-001', merchantId: 1, settlementDate: '2026-06-28', grossAmount: 780000, platformFee: 15600, netAmount: 764400, transactionCount: 342, status: 'COMPLETED', createdAt: '2026-06-28T00:05:00' },
  { settlementId: 'STL-002', merchantId: 1, settlementDate: '2026-06-27', grossAmount: 645000, platformFee: 12900, netAmount: 632100, transactionCount: 287, status: 'COMPLETED', createdAt: '2026-06-27T00:05:00' },
  { settlementId: 'STL-003', merchantId: 1, settlementDate: '2026-06-26', grossAmount: 920000, platformFee: 18400, netAmount: 901600, transactionCount: 410, status: 'COMPLETED', createdAt: '2026-06-26T00:05:00' },
  { settlementId: 'STL-004', merchantId: 1, settlementDate: '2026-06-25', grossAmount: 550000, platformFee: 11000, netAmount: 539000, transactionCount: 245, status: 'COMPLETED', createdAt: '2026-06-25T00:05:00' },
  { settlementId: 'STL-005', merchantId: 1, settlementDate: '2026-06-24', grossAmount: 1095000, platformFee: 21900, netAmount: 1073100, transactionCount: 488, status: 'COMPLETED', createdAt: '2026-06-24T00:05:00' },
]

const formatCurrency = (amount) => `₹${amount?.toLocaleString('en-IN', { maximumFractionDigits: 2 }) || 0}`

export default function Settlements({ merchant }) {
  const [balance, setBalance] = useState(demoBalance)
  const [settlements, setSettlements] = useState(demoSettlements)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    if (!merchant?.id) return
    setLoading(true)
    try {
      const [balRes, stlRes] = await Promise.allSettled([
        settlementAPI.getBalance(merchant.id),
        settlementAPI.getByMerchant(merchant.id),
      ])
      if (balRes.status === 'fulfilled' && balRes.value.data?.data) {
        setBalance(balRes.value.data.data)
      }
      if (stlRes.status === 'fulfilled' && stlRes.value.data?.data) {
        const data = stlRes.value.data.data
        setSettlements(Array.isArray(data) ? data : data.content || demoSettlements)
      }
    } catch (err) {
      console.log('Using demo settlement data')
    } finally {
      setLoading(false)
    }
  }

  const handleTriggerSettlement = async () => {
    if (!merchant?.id) return
    try {
      await settlementAPI.trigger(merchant.id)
      toast.success('Settlement triggered successfully!')
      loadData()
    } catch (err) {
      toast.error('Failed to trigger settlement')
    }
  }

  return (
    <div>
      {/* Balance Cards */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Total Earnings</span>
            <div className="kpi-icon success"><Wallet size={20} /></div>
          </div>
          <div className="kpi-value">{formatCurrency(balance.totalEarnings)}</div>
          <div className="kpi-change positive">
            <ArrowUpRight size={14} /> All time
          </div>
        </div>
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Settled Amount</span>
            <div className="kpi-icon accent"><Landmark size={20} /></div>
          </div>
          <div className="kpi-value">{formatCurrency(balance.settledAmount)}</div>
          <div className="kpi-change positive">
            <ArrowUpRight size={14} /> Transferred
          </div>
        </div>
        <div className="kpi-card" style={{ borderColor: 'var(--warning)' }}>
          <div className="kpi-header">
            <span className="kpi-label">Pending Settlement</span>
            <div className="kpi-icon warning"><Zap size={20} /></div>
          </div>
          <div className="kpi-value" style={{ color: 'var(--warning)' }}>
            {formatCurrency(balance.pendingAmount)}
          </div>
          <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
            Next settlement: T+1
          </div>
        </div>
        <div className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-label">Platform Fee</span>
            <div className="kpi-icon info"><Landmark size={20} /></div>
          </div>
          <div className="kpi-value">2.0%</div>
          <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
            Per transaction
          </div>
        </div>
      </div>

      {/* Settlement History */}
      <div className="table-card">
        <div className="table-header">
          <h3 className="table-title">Settlement History</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-ghost btn-sm" onClick={loadData}>
              <RefreshCw size={14} /> Refresh
            </button>
            <button className="btn btn-primary btn-sm" onClick={handleTriggerSettlement}>
              <Zap size={14} /> Trigger Settlement
            </button>
          </div>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Settlement ID</th>
              <th>Date</th>
              <th>Transactions</th>
              <th>Gross Amount</th>
              <th>Platform Fee</th>
              <th>Net Amount</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {settlements.map((stl, idx) => (
              <tr key={stl.settlementId || idx}>
                <td>
                  <span style={{ fontFamily: 'monospace', fontSize: 12, color: 'var(--text-primary)' }}>
                    {stl.settlementId}
                  </span>
                </td>
                <td>{stl.settlementDate}</td>
                <td style={{ fontWeight: 600 }}>{stl.transactionCount}</td>
                <td>{formatCurrency(stl.grossAmount)}</td>
                <td style={{ color: 'var(--danger)' }}>
                  -{formatCurrency(stl.platformFee)}
                </td>
                <td style={{ fontWeight: 700, color: 'var(--success)' }}>
                  {formatCurrency(stl.netAmount)}
                </td>
                <td>
                  <span className={`badge ${stl.status === 'COMPLETED' ? 'badge-success' : stl.status === 'PENDING' ? 'badge-warning' : 'badge-danger'}`}>
                    <span className="badge-dot" />
                    {stl.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {settlements.length === 0 && (
          <div className="empty-state">
            <Landmark size={48} style={{ color: 'var(--text-muted)', opacity: 0.5, marginBottom: 16 }} />
            <div className="empty-state-title">No settlements yet</div>
            <p style={{ color: 'var(--text-muted)' }}>Settlements are processed daily at midnight (T+1)</p>
          </div>
        )}
      </div>
    </div>
  )
}
