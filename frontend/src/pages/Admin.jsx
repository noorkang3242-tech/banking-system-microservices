import { useEffect, useState } from 'react'
import { Card, Tabs, Table, Tag, Button, Space, Select, Form, Input, Row, Col, Statistic, Spin, Popconfirm, Descriptions, Empty, Modal, App as AntApp } from 'antd'
import {
  TeamOutlined, BankOutlined, WalletOutlined, HistoryOutlined,
  SwapOutlined, DollarOutlined, CreditCardOutlined, ReloadOutlined,
  StopOutlined, CheckCircleOutlined, SearchOutlined,
} from '@ant-design/icons'
import { customerApi, accountApi, loanApi, notifApi, txnApi, transferApi, cardApi, errMsg } from '../api/endpoints'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()
const loanColor = { PENDING: 'gold', APPROVED: 'blue', ACTIVE: 'green', REJECTED: 'red', CLOSED: 'default' }
const txnColor = { DEPOSIT: 'green', WITHDRAWAL: 'red', TRANSFER_IN: 'green', TRANSFER_OUT: 'orange' }

function Overview() {
  const [loading, setLoading] = useState(true)
  const [s, setS] = useState({})
  useEffect(() => {
    Promise.allSettled([customerApi.all(), accountApi.all(), txnApi.all(), transferApi.all(), loanApi.all(), cardApi.all()])
      .then(([c, a, t, tr, l, cd]) => {
        const accounts = a.status === 'fulfilled' ? a.value.data : []
        const loans = l.status === 'fulfilled' ? l.value.data : []
        setS({
          customers: c.status === 'fulfilled' ? c.value.data.length : 0,
          accounts: accounts.length,
          balance: accounts.reduce((sum, x) => sum + Number(x.balance || 0), 0),
          txns: t.status === 'fulfilled' ? t.value.data.length : 0,
          transfers: tr.status === 'fulfilled' ? tr.value.data.length : 0,
          loans: loans.length,
          activeLoans: loans.filter((x) => x.status === 'ACTIVE').length,
          cards: cd.status === 'fulfilled' ? cd.value.data.length : 0,
        })
      })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div style={{ textAlign: 'center', padding: 40 }}><Spin size="large" /></div>

  const tile = (icon, color, bg, title, value) => (
    <Col xs={12} md={6}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: bg, color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22 }}>{icon}</div>
          <div><div style={{ color: '#888', fontSize: 13 }}>{title}</div><div style={{ fontSize: 20, fontWeight: 700 }}>{value}</div></div>
        </div>
      </Card>
    </Col>
  )

  return (
    <Row gutter={[16, 16]}>
      {tile(<TeamOutlined />, '#1677ff', '#e6f0ff', 'Customers', s.customers)}
      {tile(<BankOutlined />, '#fb8c00', '#fff3e0', 'Accounts', s.accounts)}
      {tile(<WalletOutlined />, '#2e7d32', '#e8f5e9', 'Total Balance', money(s.balance))}
      {tile(<HistoryOutlined />, '#722ed1', '#f9f0ff', 'Transactions', s.txns)}
      {tile(<SwapOutlined />, '#fa541c', '#fff2e8', 'Transfers', s.transfers)}
      {tile(<DollarOutlined />, '#13c2c2', '#e6fffb', 'Loans (active)', `${s.loans} (${s.activeLoans})`)}
      {tile(<CreditCardOutlined />, '#eb2f96', '#fff0f6', 'Cards', s.cards)}
    </Row>
  )
}

// Filter / search section: admin types an account number and gets its full info.
function AccountLookup() {
  const { message } = AntApp.useApp()
  const [loading, setLoading] = useState(false)
  const [acct, setAcct] = useState(null)
  const [holder, setHolder] = useState(null)
  const [txns, setTxns] = useState([])
  const [searched, setSearched] = useState(false)

  const search = async (value) => {
    const q = (value || '').trim()
    if (q.length < 6) { message.warning('Please enter a full account number'); return }
    setLoading(true); setAcct(null); setTxns([]); setHolder(null); setSearched(true)
    try {
      const a = await accountApi.one(q)
      setAcct(a.data)
      accountApi.beneficiary(q).then((r) => setHolder(r.data.holderName)).catch(() => {})
      txnApi.byAccount(q).then((r) => setTxns(r.data)).catch(() => {})
    } catch (e) {
      message.error(errMsg(e))
    } finally { setLoading(false) }
  }

  const change = async (status) => {
    try { await accountApi.setStatus(acct.accountNumber, status); message.success(status === 'FROZEN' ? 'Account blocked' : 'Account unblocked'); search(acct.accountNumber) }
    catch (e) { message.error(errMsg(e)) }
  }

  return (
    <div>
      <Input.Search
        placeholder="Enter account number (e.g. 3333342037278058)"
        enterButton={<span><SearchOutlined /> Search</span>}
        size="large"
        allowClear
        loading={loading}
        onSearch={search}
        style={{ maxWidth: 540, marginBottom: 18 }}
      />

      {acct && (
        <>
          <Descriptions
            bordered
            column={{ xs: 1, sm: 2 }}
            size="middle"
            style={{ marginBottom: 18 }}
            title={<span><BankOutlined /> &nbsp;Account Details</span>}
            extra={acct.status === 'ACTIVE'
              ? <Popconfirm title="Block this account?" description="Frozen accounts cannot deposit or withdraw." okText="Block" okButtonProps={{ danger: true }} onConfirm={() => change('FROZEN')}>
                  <Button danger icon={<StopOutlined />}>Block</Button>
                </Popconfirm>
              : <Button type="primary" icon={<CheckCircleOutlined />} onClick={() => change('ACTIVE')}>Unblock</Button>}
          >
            <Descriptions.Item label="Holder">{holder || '—'}</Descriptions.Item>
            <Descriptions.Item label="Account #">{acct.accountNumber}</Descriptions.Item>
            <Descriptions.Item label="Type">{acct.accountType}</Descriptions.Item>
            <Descriptions.Item label="Balance"><b style={{ color: '#2e7d32' }}>{money(acct.balance)}</b></Descriptions.Item>
            <Descriptions.Item label="Status"><Tag color={acct.status === 'ACTIVE' ? 'green' : acct.status === 'FROZEN' ? 'red' : 'default'}>{acct.status}</Tag></Descriptions.Item>
            <Descriptions.Item label="Currency">{acct.currency}</Descriptions.Item>
            <Descriptions.Item label="User ID" span={2}>{acct.userId}</Descriptions.Item>
          </Descriptions>

          <Card type="inner" title={`Transactions (${txns.length})`} size="small">
            <Table rowKey="transactionId" dataSource={txns} size="small" pagination={{ pageSize: 8 }} scroll={{ x: 700 }}
              locale={{ emptyText: 'No transactions for this account' }}
              columns={[
                { title: 'Type', dataIndex: 'type', render: (t) => <Tag color={txnColor[t]}>{t}</Tag> },
                { title: 'Amount', dataIndex: 'amount', render: (v) => money(v) },
                { title: 'Balance After', dataIndex: 'balanceAfter', render: (v) => money(v) },
                { title: 'Description', dataIndex: 'description', render: (d) => d || '-' },
                { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-' },
              ]} />
          </Card>
        </>
      )}

      {searched && !acct && !loading && <Empty description="No account found" />}
    </div>
  )
}

function Customers() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([]); const [loading, setLoading] = useState(true)
  const load = () => { setLoading(true); customerApi.all().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false)) }
  useEffect(load, [])
  const setKyc = async (userId, kycStatus) => { try { await customerApi.updateKyc(userId, kycStatus); message.success('KYC updated'); load() } catch (e) { message.error(errMsg(e)) } }
  return (
    <Table rowKey="userId" loading={loading} dataSource={rows} scroll={{ x: 700 }}
      columns={[
        { title: 'Name', render: (_, r) => `${r.firstName} ${r.lastName}` },
        { title: 'Email', dataIndex: 'email' },
        { title: 'Phone', dataIndex: 'phone' },
        { title: 'KYC', dataIndex: 'kycStatus', render: (s) => <Tag color={s === 'VERIFIED' ? 'green' : s === 'REJECTED' ? 'red' : 'gold'}>{s}</Tag> },
        { title: 'Set KYC', render: (_, r) => (
          <Select size="small" value={r.kycStatus} style={{ width: 130 }} onChange={(v) => setKyc(r.userId, v)}
            options={['PENDING', 'VERIFIED', 'REJECTED'].map((v) => ({ value: v, label: v }))} />
        ) },
      ]} />
  )
}

function AllAccounts() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([]); const [loading, setLoading] = useState(true)
  const load = () => { setLoading(true); accountApi.all().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false)) }
  useEffect(load, [])
  const change = async (num, status) => {
    try { await accountApi.setStatus(num, status); message.success(status === 'FROZEN' ? 'Account blocked' : 'Account unblocked'); load() }
    catch (e) { message.error(errMsg(e)) }
  }
  return (
    <>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 12 }}>Refresh</Button>
      <Table rowKey="accountNumber" loading={loading} dataSource={rows} scroll={{ x: 700 }}
        columns={[
          { title: 'Account', dataIndex: 'accountNumber' },
          { title: 'Type', dataIndex: 'accountType' },
          { title: 'Balance', dataIndex: 'balance', render: (v) => money(v), sorter: (a, b) => a.balance - b.balance },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'ACTIVE' ? 'green' : s === 'FROZEN' ? 'red' : 'default'}>{s}</Tag> },
          { title: 'Action', render: (_, r) => r.status === 'ACTIVE' ? (
            <Popconfirm title="Block this account?" description="Frozen accounts cannot deposit or withdraw." okText="Block" okButtonProps={{ danger: true }} onConfirm={() => change(r.accountNumber, 'FROZEN')}>
              <Button size="small" danger icon={<StopOutlined />}>Block</Button>
            </Popconfirm>
          ) : (
            <Button size="small" type="primary" icon={<CheckCircleOutlined />} onClick={() => change(r.accountNumber, 'ACTIVE')}>Unblock</Button>
          ) },
        ]} />
    </>
  )
}

function AllLoans() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [q, setQ] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [rejectFor, setRejectFor] = useState(null)
  const [reason, setReason] = useState('')

  const load = () => {
    setLoading(true)
    loanApi.all()
      // newest first, like an inbox
      .then((r) => setRows([...r.data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))))
      .catch((e) => message.error(errMsg(e)))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const approve = async (id) => { try { await loanApi.approve(id); message.success('Loan approved & disbursed'); load() } catch (e) { message.error(errMsg(e)) } }
  const doReject = async () => {
    try { await loanApi.reject(rejectFor.loanId, reason || 'Rejected by admin'); message.success('Loan rejected'); setRejectFor(null); setReason(''); load() }
    catch (e) { message.error(errMsg(e)) }
  }

  const term = q.trim().toLowerCase()
  const filtered = rows.filter((r) => {
    const matchesQ = !term || [r.loanId, r.accountNumber, r.userId, r.purpose].some((f) => String(f || '').toLowerCase().includes(term))
    const matchesStatus = statusFilter === 'ALL' || r.status === statusFilter
    return matchesQ && matchesStatus
  })
  const pending = rows.filter((r) => r.status === 'PENDING').length

  return (
    <>
      <Space wrap style={{ marginBottom: 12 }}>
        <Input.Search allowClear placeholder="Search by Loan ID, Account # or User ID" onChange={(e) => setQ(e.target.value)} onSearch={setQ} style={{ width: 320 }} />
        <Select value={statusFilter} onChange={setStatusFilter} style={{ width: 160 }}
          options={['ALL', 'PENDING', 'APPROVED', 'ACTIVE', 'REJECTED', 'CLOSED'].map((s) => ({ value: s, label: s === 'ALL' ? 'All statuses' : s }))} />
        <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
        <span style={{ color: '#888' }}>{filtered.length} of {rows.length} · {pending} pending</span>
      </Space>

      <Table rowKey="loanId" loading={loading} dataSource={filtered} scroll={{ x: 1050 }}
        columns={[
          { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-', sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt), defaultSortOrder: 'descend', width: 165 },
          { title: 'Loan ID', dataIndex: 'loanId', render: (v) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span> },
          { title: 'Account', dataIndex: 'accountNumber' },
          { title: 'Purpose', dataIndex: 'purpose', render: (p) => p || '-' },
          { title: 'Principal', dataIndex: 'principal', render: (v) => money(v) },
          { title: 'Rate', dataIndex: 'interestRate', render: (v) => `${v}%` },
          { title: 'Outstanding', dataIndex: 'outstanding', render: (v) => money(v) },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={loanColor[s]}>{s}</Tag> },
          { title: 'Action', fixed: 'right', width: 170, render: (_, r) => r.status === 'PENDING' ? (
            <Space>
              <Button size="small" type="primary" onClick={() => approve(r.loanId)}>Approve</Button>
              <Button size="small" danger onClick={() => setRejectFor(r)}>Reject</Button>
            </Space>
          ) : '-' },
        ]} />

      <Modal title="Reject Loan" open={!!rejectFor} onCancel={() => { setRejectFor(null); setReason('') }} onOk={doReject} okText="Reject" okButtonProps={{ danger: true }}>
        {rejectFor && <p style={{ color: '#888' }}>Loan <b>{rejectFor.loanId}</b> · {money(rejectFor.principal)}</p>}
        <Input.TextArea rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Reason for rejection (optional)" />
      </Modal>
    </>
  )
}

function AllTransactions() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([]); const [loading, setLoading] = useState(true)
  const load = () => { setLoading(true); txnApi.all().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false)) }
  useEffect(load, [])
  return (
    <>
      <Space style={{ marginBottom: 12 }}>
        <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
        <span style={{ color: '#888' }}>{rows.length.toLocaleString()} records</span>
      </Space>
      <Table rowKey="transactionId" loading={loading} dataSource={rows} scroll={{ x: 800 }} pagination={{ pageSize: 12, showSizeChanger: true }}
        columns={[
          { title: 'Type', dataIndex: 'type', render: (t) => <Tag color={txnColor[t]}>{t}</Tag>, filters: Object.keys(txnColor).map((k) => ({ text: k, value: k })), onFilter: (v, r) => r.type === v },
          { title: 'Account', dataIndex: 'accountNumber' },
          { title: 'Amount', dataIndex: 'amount', render: (v) => money(v), sorter: (a, b) => a.amount - b.amount },
          { title: 'Balance After', dataIndex: 'balanceAfter', render: (v) => money(v) },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'SUCCESS' ? 'green' : 'default'}>{s}</Tag> },
          { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-', sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt), defaultSortOrder: 'descend' },
        ]} />
    </>
  )
}

function AllTransfers() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([]); const [loading, setLoading] = useState(true)
  const load = () => { setLoading(true); transferApi.all().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false)) }
  useEffect(load, [])
  return (
    <>
      <Space style={{ marginBottom: 12 }}>
        <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
        <span style={{ color: '#888' }}>{rows.length.toLocaleString()} records</span>
      </Space>
      <Table rowKey="transferId" loading={loading} dataSource={rows} scroll={{ x: 800 }} pagination={{ pageSize: 12, showSizeChanger: true }}
        columns={[
          { title: 'From', dataIndex: 'fromAccount' },
          { title: 'To', dataIndex: 'toAccount' },
          { title: 'Amount', dataIndex: 'amount', render: (v) => money(v), sorter: (a, b) => a.amount - b.amount },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'COMPLETED' ? 'green' : 'red'}>{s}</Tag> },
          { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-', sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt), defaultSortOrder: 'descend' },
        ]} />
    </>
  )
}

function SendNotification() {
  const { message } = AntApp.useApp()
  const [busy, setBusy] = useState(false); const [form] = Form.useForm()
  const onFinish = async (v) => {
    setBusy(true)
    try { await notifApi.send(v); message.success('Notification sent'); form.resetFields() }
    catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }
  return (
    <Form form={form} layout="vertical" onFinish={onFinish} style={{ maxWidth: 480 }} initialValues={{ type: 'GENERAL' }}>
      <Form.Item name="recipientUserId" label="Recipient User ID" rules={[{ required: true }]}>
        <Input placeholder="userId of the recipient" />
      </Form.Item>
      <Form.Item name="type" label="Type" rules={[{ required: true }]}>
        <Select options={['TRANSACTION', 'TRANSFER', 'LOAN', 'CARD', 'SECURITY', 'GENERAL'].map((v) => ({ value: v, label: v }))} />
      </Form.Item>
      <Form.Item name="title" label="Title" rules={[{ required: true }]}><Input placeholder="Title" /></Form.Item>
      <Form.Item name="message" label="Message" rules={[{ required: true }]}><Input.TextArea rows={3} placeholder="Message" /></Form.Item>
      <Button type="primary" htmlType="submit" loading={busy}>Send</Button>
    </Form>
  )
}

export default function Admin() {
  return (
    <Card title="🛡️ Admin Panel">
      <Tabs items={[
        { key: 'lookup', label: '🔍 Account Lookup', children: <AccountLookup /> },
        { key: 'overview', label: 'Overview', children: <Overview /> },
        { key: 'cust', label: 'Customers', children: <Customers /> },
        { key: 'acc', label: 'Accounts (block/unblock)', children: <AllAccounts /> },
        { key: 'txn', label: 'All Transactions', children: <AllTransactions /> },
        { key: 'transfers', label: 'All Transfers', children: <AllTransfers /> },
        { key: 'loans', label: 'Loans (approve/reject)', children: <AllLoans /> },
        { key: 'notif', label: 'Send Notification', children: <SendNotification /> },
      ]} />
    </Card>
  )
}
