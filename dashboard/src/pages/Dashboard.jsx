import { useState, useEffect } from 'react'
import {
  DollarSign, TrendingUp, CheckCircle2, ShieldAlert,
  Clock, CreditCard, ArrowUpRight, ArrowDownRight
} from 'lucide-react'
import {
  LineChart, Line, AreaChart, Area, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts'
import { transactionAPI, fraudAPI } from '../services/api'

// Demo data for when backend is not running
const demoStats = {
  totalTransactions: 12847,
  successCount: 11234,
  failedCount: 987,
  pendingCount: 326,
  fraudFlaggedCount: 300,
  totalRevenue: 4523890.50,
  successRate: 87.4,
  fraudRate: 2.3,
}

const demoDailyVolume = [
  { date: 'Mon', count: 1840, revenue: 645200 },
  { date: 'Tue', count: 2100, revenue: 735000 },
  { date: 'Wed', count: 1950, revenue: 682500 },
  { date: 'Thu', count: 2300, revenue: 805000 },
  { date: 'Fri', count: 2680, revenue: 938000 },
  { date: 'Sat', count: 1500, revenue: 525000 },
  { date: 'Sun', count: 1200, revenue: 420000 },
]

const demoDistribution = [
  { name: 'UPI', value: 45 },
  { name: 'Credit Card', value: 25 },
  { name: 'Debit Card', value: 18 },
  { name: 'Net Banking', value: 8 },
  { name: 'Wallet', value: 4 },
]

const CHART_COLORS = ['#6366f1', '#8b5cf6', '#a78bfa', '#c4b5fd', '#ddd6fe']

const formatCurrency = (amount) => {
  if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(2)}Cr`
  if (amount >= 100000) return `₹${(amount / 100000).toFixed(2)}L`
  if (amount >= 1000) return `₹${(amount / 1000).toFixed(1)}K`
  return `₹${amount?.toFixed(2) || 0}`
}

const formatNumber = (num) => {
  if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}K`
  return num?.toLocaleString() || '0'
}

export default function Dashboard({ merchant }) {
  const [stats, setStats] = useState(demoStats)
  const [dailyVolume, setDailyVolume] = useState(demoDailyVolume)
  const [distribution, setDistribution] = useState(demoDistribution)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [statsRes, volumeRes, distRes] = await Promise.allSettled([
        transactionAPI.getStats(),
        transactionAPI.getDailyVolume(7),
        transactionAPI.getPaymentMethodDistribution(),
      ])

      if (statsRes.status === 'fulfilled') {
        setStats(statsRes.value.data?.data || demoStats)
      }
      if (volumeRes.status === 'fulfilled' && volumeRes.value.data?.data) {
        setDailyVolume(volumeRes.value.data.data)
      }
      if (distRes.status === 'fulfilled' && distRes.value.data?.data) {
        const data = distRes.value.data.data
        setDistribution(
          Object.entries(data).map(([name, value]) => ({ name, value }))
        )
      }
    } catch (err) {
      console.log('Using demo data (backend may not be running)')
    } finally {
      setLoading(false)
    }
  }

  const kpiCards = [
    {
      label: 'Total Revenue',
      value: formatCurrency(stats.totalRevenue),
      icon: DollarSign,
      iconClass: 'success',
      change: '+12.5%',
      positive: true,
    },
    {
      label: 'Total Transactions',
      value: formatNumber(stats.totalTransactions),
      icon: TrendingUp,
      iconClass: 'accent',
      change: '+8.2%',
      positive: true,
    },
    {
      label: 'Success Rate',
      value: `${stats.successRate?.toFixed(1) || 0}%`,
      icon: CheckCircle2,
      iconClass: 'success',
      change: '+1.3%',
      positive: true,
    },
    {
      label: 'Failed',
      value: formatNumber(stats.failedCount),
      icon: Clock,
      iconClass: 'danger',
      change: '-3.1%',
      positive: true,
    },
    {
      label: 'Fraud Detected',
      value: formatNumber(stats.fraudFlaggedCount),
      icon: ShieldAlert,
      iconClass: 'warning',
      change: stats.fraudRate?.toFixed(1) + '%',
      positive: false,
    },
    {
      label: 'Payment Methods',
      value: '5 Active',
      icon: CreditCard,
      iconClass: 'info',
      change: 'All active',
      positive: true,
    },
  ]

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-sm)',
          padding: '12px 16px',
          fontSize: '13px',
        }}>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{label}</div>
          {payload.map((item, idx) => (
            <div key={idx} style={{ color: item.color, marginTop: 2 }}>
              {item.name}: {item.name === 'revenue' ? formatCurrency(item.value) : item.value}
            </div>
          ))}
        </div>
      )
    }
    return null
  }

  return (
    <div>
      {/* KPI Cards */}
      <div className="kpi-grid">
        {kpiCards.map((card, idx) => (
          <div key={idx} className="kpi-card">
            <div className="kpi-header">
              <span className="kpi-label">{card.label}</span>
              <div className={`kpi-icon ${card.iconClass}`}>
                <card.icon size={20} />
              </div>
            </div>
            <div className="kpi-value">{card.value}</div>
            <div className={`kpi-change ${card.positive ? 'positive' : 'negative'}`}>
              {card.positive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
              {card.change}
            </div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="charts-grid">
        {/* Transaction Volume Chart */}
        <div className="chart-card">
          <div className="chart-card-header">
            <h3 className="chart-title">Transaction Volume — Last 7 Days</h3>
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={dailyVolume}>
              <defs>
                <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis
                dataKey="date"
                stroke="var(--text-muted)"
                fontSize={12}
                tickLine={false}
              />
              <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              <Area
                type="monotone"
                dataKey="count"
                name="Transactions"
                stroke="#6366f1"
                fill="url(#colorCount)"
                strokeWidth={2}
              />
              <Area
                type="monotone"
                dataKey="revenue"
                name="revenue"
                stroke="#8b5cf6"
                fill="url(#colorRevenue)"
                strokeWidth={2}
                yAxisId={0}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Payment Method Distribution */}
        <div className="chart-card">
          <div className="chart-card-header">
            <h3 className="chart-title">Payment Methods</h3>
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={distribution}
                cx="50%"
                cy="50%"
                innerRadius={70}
                outerRadius={110}
                paddingAngle={4}
                dataKey="value"
              >
                {distribution.map((_, idx) => (
                  <Cell key={idx} fill={CHART_COLORS[idx % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '13px',
                }}
              />
              <Legend
                wrapperStyle={{ fontSize: '12px', color: 'var(--text-secondary)' }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Status Distribution */}
      <div className="chart-card" style={{ marginBottom: 28 }}>
        <div className="chart-card-header">
          <h3 className="chart-title">Transaction Status Overview</h3>
        </div>
        <div style={{ display: 'flex', gap: 24, justifyContent: 'center', padding: '20px 0' }}>
          {[
            { label: 'Success', count: stats.successCount, color: 'var(--success)', pct: stats.successRate },
            { label: 'Failed', count: stats.failedCount, color: 'var(--danger)', pct: (stats.failedCount / stats.totalTransactions * 100) || 0 },
            { label: 'Pending', count: stats.pendingCount, color: 'var(--warning)', pct: (stats.pendingCount / stats.totalTransactions * 100) || 0 },
            { label: 'Fraud Flagged', count: stats.fraudFlaggedCount, color: 'var(--accent-primary)', pct: stats.fraudRate },
          ].map((item, idx) => (
            <div key={idx} style={{ textAlign: 'center', minWidth: 140 }}>
              <div style={{
                fontSize: 28,
                fontWeight: 800,
                color: item.color,
                letterSpacing: '-1px'
              }}>
                {formatNumber(item.count)}
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
                {item.label}
              </div>
              <div style={{
                fontSize: 12,
                color: 'var(--text-muted)',
                marginTop: 2
              }}>
                {item.pct?.toFixed(1)}%
              </div>
              <div style={{
                width: '100%',
                height: 4,
                background: 'var(--border)',
                borderRadius: 2,
                marginTop: 8,
                overflow: 'hidden'
              }}>
                <div style={{
                  width: `${Math.min(item.pct || 0, 100)}%`,
                  height: '100%',
                  background: item.color,
                  borderRadius: 2,
                  transition: 'width 1s ease',
                }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
