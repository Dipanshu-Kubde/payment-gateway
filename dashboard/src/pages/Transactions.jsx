import { useState, useEffect } from 'react'
import { Search, Filter, RefreshCw, Eye } from 'lucide-react'
import { transactionAPI } from '../services/api'

const demoTransactions = [
  { transactionId: 'TXN-A1B2C3D4', orderId: 'ORD-001', amount: 15000, currency: 'INR', paymentMethod: 'UPI', status: 'SUCCESS', riskScore: 12, customerEmail: 'john@example.com', createdAt: '2026-06-29T10:30:00' },
  { transactionId: 'TXN-E5F6G7H8', orderId: 'ORD-002', amount: 75000, currency: 'INR', paymentMethod: 'CREDIT_CARD', status: 'FRAUD_REVIEW', riskScore: 72, customerEmail: 'suspicious@test.com', createdAt: '2026-06-29T10:25:00' },
  { transactionId: 'TXN-I9J0K1L2', orderId: 'ORD-003', amount: 2500, currency: 'INR', paymentMethod: 'DEBIT_CARD', status: 'SUCCESS', riskScore: 5, customerEmail: 'alice@example.com', createdAt: '2026-06-29T10:20:00' },
  { transactionId: 'TXN-M3N4O5P6', orderId: 'ORD-004', amount: 45000, currency: 'INR', paymentMethod: 'NET_BANKING', status: 'FAILED', riskScore: 0, customerEmail: 'bob@example.com', createdAt: '2026-06-29T10:15:00' },
  { transactionId: 'TXN-Q7R8S9T0', orderId: 'ORD-005', amount: 8900, currency: 'INR', paymentMethod: 'UPI', status: 'SUCCESS', riskScore: 8, customerEmail: 'sara@example.com', createdAt: '2026-06-29T10:10:00' },
  { transactionId: 'TXN-U1V2W3X4', orderId: 'ORD-006', amount: 120000, currency: 'INR', paymentMethod: 'CREDIT_CARD', status: 'REJECTED', riskScore: 91, customerEmail: 'fraud@darkweb.com', createdAt: '2026-06-29T10:05:00' },
  { transactionId: 'TXN-Y5Z6A7B8', orderId: 'ORD-007', amount: 3200, currency: 'INR', paymentMethod: 'WALLET', status: 'SUCCESS', riskScore: 3, customerEmail: 'mike@example.com', createdAt: '2026-06-29T10:00:00' },
  { transactionId: 'TXN-C9D0E1F2', orderId: 'ORD-008', amount: 58000, currency: 'INR', paymentMethod: 'UPI', status: 'INITIATED', riskScore: 0, customerEmail: 'dave@example.com', createdAt: '2026-06-29T09:55:00' },
]

const getStatusBadge = (status) => {
  const map = {
    SUCCESS: 'badge-success',
    FAILED: 'badge-danger',
    FRAUD_REVIEW: 'badge-warning',
    REJECTED: 'badge-danger',
    INITIATED: 'badge-info',
    PROCESSING: 'badge-info',
    RETRY_SCHEDULED: 'badge-warning',
    REFUNDED: 'badge-accent',
  }
  return map[status] || 'badge-info'
}

const getRiskColor = (score) => {
  if (score <= 30) return 'var(--success)'
  if (score <= 60) return 'var(--warning)'
  if (score <= 80) return '#f97316'
  return 'var(--danger)'
}

export default function Transactions({ merchant }) {
  const [transactions, setTransactions] = useState(demoTransactions)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)

  useEffect(() => {
    loadTransactions()
  }, [page, statusFilter])

  const loadTransactions = async () => {
    setLoading(true)
    try {
      const params = { page, size: 20 }
      if (statusFilter !== 'ALL') params.status = statusFilter
      const res = await transactionAPI.getAll(params)
      if (res.data?.data?.content) {
        setTransactions(res.data.data.content)
      }
    } catch (err) {
      console.log('Using demo transactions')
    } finally {
      setLoading(false)
    }
  }

  const filtered = transactions.filter(txn => {
    const matchesSearch = searchTerm === '' ||
      txn.transactionId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      txn.customerEmail?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      txn.orderId?.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesSearch
  })

  const formatAmount = (amount) => `₹${amount?.toLocaleString('en-IN') || 0}`
  const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) +
      ' ' + d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div>
      {/* Filters */}
      <div className="filters-bar">
        <div style={{ position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="search-input"
            placeholder="Search by ID, email, or order..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <select
          className="filter-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Status</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILED">Failed</option>
          <option value="INITIATED">Initiated</option>
          <option value="PROCESSING">Processing</option>
          <option value="FRAUD_REVIEW">Fraud Review</option>
          <option value="REJECTED">Rejected</option>
          <option value="REFUNDED">Refunded</option>
        </select>

        <button className="btn btn-ghost btn-sm" onClick={loadTransactions}>
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      {/* Table */}
      <div className="table-card">
        <div className="table-header">
          <h3 className="table-title">Recent Transactions</h3>
          <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>
            {filtered.length} transactions
          </span>
        </div>

        <table className="data-table">
          <thead>
            <tr>
              <th>Transaction ID</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Status</th>
              <th>Risk Score</th>
              <th>Customer</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((txn, idx) => (
              <tr key={txn.transactionId || idx}>
                <td>
                  <span style={{ fontFamily: 'monospace', fontSize: 13, color: 'var(--text-primary)' }}>
                    {txn.transactionId?.substring(0, 12) || '-'}
                  </span>
                </td>
                <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                  {formatAmount(txn.amount)}
                </td>
                <td>
                  <span className="badge badge-accent">
                    {txn.paymentMethod?.replace('_', ' ')}
                  </span>
                </td>
                <td>
                  <span className={`badge ${getStatusBadge(txn.status)}`}>
                    <span className="badge-dot" />
                    {txn.status}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{
                      width: 40,
                      height: 6,
                      background: 'var(--border)',
                      borderRadius: 3,
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        width: `${txn.riskScore || 0}%`,
                        height: '100%',
                        background: getRiskColor(txn.riskScore || 0),
                        borderRadius: 3,
                      }} />
                    </div>
                    <span style={{
                      fontSize: 12,
                      fontWeight: 600,
                      color: getRiskColor(txn.riskScore || 0)
                    }}>
                      {txn.riskScore || 0}
                    </span>
                  </div>
                </td>
                <td style={{ fontSize: 13 }}>
                  {txn.customerEmail || '-'}
                </td>
                <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                  {formatDate(txn.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filtered.length === 0 && (
          <div className="empty-state">
            <div className="empty-state-title">No transactions found</div>
            <p>Try adjusting your filters</p>
          </div>
        )}

        <div className="pagination">
          <span className="pagination-info">
            Showing {filtered.length} results
          </span>
          <div className="pagination-buttons">
            <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
              Previous
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => setPage(p => p + 1)}>
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
