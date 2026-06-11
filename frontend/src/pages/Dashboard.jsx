import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Row, Col, Card, Statistic, Table, Tag, Spin, Empty } from 'antd'
import {
  BankOutlined, DollarOutlined, CreditCardOutlined, WalletOutlined,
  SwapOutlined, HistoryOutlined, BellOutlined,
} from '@ant-design/icons'
import { accountApi, loanApi, cardApi, txnApi } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { useLang } from '../i18n/LanguageContext'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()

const typeColor = { DEPOSIT: 'green', WITHDRAWAL: 'red', TRANSFER_IN: 'green', TRANSFER_OUT: 'orange' }

// each service shows its own icon + colour, like a real bank dashboard
const SERVICES = [
  { tkey: 'nav.accounts', icon: <BankOutlined />, color: '#1677ff', path: '/accounts' },
  { tkey: 'nav.transfer', icon: <SwapOutlined />, color: '#fb8c00', path: '/transfer' },
  { tkey: 'nav.transactions', icon: <HistoryOutlined />, color: '#722ed1', path: '/transactions' },
  { tkey: 'nav.loans', icon: <DollarOutlined />, color: '#13c2c2', path: '/loans' },
  { tkey: 'nav.cards', icon: <CreditCardOutlined />, color: '#eb2f96', path: '/cards' },
  { tkey: 'nav.notifications', icon: <BellOutlined />, color: '#52c41a', path: '/notifications' },
]

const STATS = [
  { key: 'bal', tkey: 'dash.totalBalance', icon: <WalletOutlined />, color: '#1677ff', bg: '#e6f0ff' },
  { key: 'acc', tkey: 'nav.accounts', icon: <BankOutlined />, color: '#fb8c00', bg: '#fff3e0' },
  { key: 'loan', tkey: 'dash.activeLoans', icon: <DollarOutlined />, color: '#13c2c2', bg: '#e6fffb' },
  { key: 'card', tkey: 'nav.cards', icon: <CreditCardOutlined />, color: '#eb2f96', bg: '#fff0f6' },
]

export default function Dashboard() {
  const { user } = useAuth()
  const { t } = useLang()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [accounts, setAccounts] = useState([])
  const [loans, setLoans] = useState([])
  const [cards, setCards] = useState([])
  const [txns, setTxns] = useState([])

  useEffect(() => {
    Promise.allSettled([accountApi.me(), loanApi.me(), cardApi.me(), txnApi.me()])
      .then(([a, l, c, t]) => {
        if (a.status === 'fulfilled') setAccounts(a.value.data)
        if (l.status === 'fulfilled') setLoans(l.value.data)
        if (c.status === 'fulfilled') setCards(c.value.data)
        if (t.status === 'fulfilled') setTxns(t.value.data.slice(0, 6))
      })
      .finally(() => setLoading(false))
  }, [])

  const totalBalance = accounts.reduce((s, a) => s + Number(a.balance || 0), 0)
  const activeLoans = loans.filter((l) => l.status === 'ACTIVE').length

  if (loading) return <div style={{ textAlign: 'center', padding: 60 }}><Spin size="large" /></div>

  return (
    <div>
      {/* Head-office hero banner */}
      <div style={{ position: 'relative', borderRadius: 16, overflow: 'hidden', marginBottom: 16, height: 210, backgroundImage: "url('/bank-building.jpg')", backgroundSize: 'cover', backgroundPosition: 'center', boxShadow: '0 10px 34px rgba(120,70,0,0.28)' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(95deg, rgba(35,20,6,0.88) 0%, rgba(35,20,6,0.55) 45%, rgba(251,140,0,0.30) 100%)' }} />
        <div style={{ position: 'relative', padding: '30px 34px', color: '#fff' }}>
          <div style={{ fontSize: 30, fontWeight: 800, letterSpacing: 0.5 }}>☀️ NOOR Bank</div>
          <div style={{ marginTop: 6, fontSize: 15, opacity: 0.92 }}>{t('dash.headOffice')}</div>
          <div style={{ marginTop: 16, display: 'inline-block', background: 'rgba(255,255,255,0.18)', backdropFilter: 'blur(4px)', padding: '6px 14px', borderRadius: 20, fontSize: 13 }}>
            {t('dash.welcome')}, <b>{user?.email}</b>
          </div>
        </div>
      </div>
      <Row gutter={[16, 16]}>
        {STATS.map((s) => {
          const val = s.key === 'bal' ? money(totalBalance) : s.key === 'acc' ? accounts.length : s.key === 'loan' ? activeLoans : cards.length
          return (
            <Col xs={12} md={6} key={s.key}>
              <Card>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <div style={{ width: 48, height: 48, borderRadius: 12, background: s.bg, color: s.color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>{s.icon}</div>
                  <div>
                    <div style={{ color: '#888', fontSize: 13 }}>{t(s.tkey)}</div>
                    <div style={{ fontSize: 20, fontWeight: 700 }}>{val}</div>
                  </div>
                </div>
              </Card>
            </Col>
          )
        })}
      </Row>

      {/* quick service tiles — each with its own icon */}
      <Card title={t('dash.quickServices')} style={{ marginTop: 16 }}>
        <Row gutter={[12, 12]}>
          {SERVICES.map((s) => (
            <Col xs={8} md={4} key={s.tkey}>
              <div className="svc-tile" onClick={() => navigate(s.path)}>
                <div className="svc-icon" style={{ background: s.color }}>{s.icon}</div>
                <div style={{ fontWeight: 600, fontSize: 13 }}>{t(s.tkey)}</div>
              </div>
            </Col>
          ))}
        </Row>
      </Card>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={10}>
          <Card title={t('dash.myAccounts')}>
            {accounts.length === 0 ? <Empty description="No accounts yet" /> : accounts.map((a) => (
              <div key={a.accountNumber} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid #f0f0f0' }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{a.accountType}</div>
                  <div style={{ color: '#999', fontSize: 12 }}>{a.accountNumber}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 600 }}>{money(a.balance)}</div>
                  <Tag color={a.status === 'ACTIVE' ? 'green' : 'default'}>{a.status}</Tag>
                </div>
              </div>
            ))}
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title={t('dash.recentTx')}>
            <Table
              size="small"
              rowKey="transactionId"
              dataSource={txns}
              pagination={false}
              locale={{ emptyText: 'No transactions yet' }}
              columns={[
                { title: 'Type', dataIndex: 'type', render: (t) => <Tag color={typeColor[t]}>{t}</Tag> },
                { title: 'Amount', dataIndex: 'amount', render: (v) => money(v) },
                { title: 'Balance After', dataIndex: 'balanceAfter', render: (v) => money(v) },
                { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-' },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
