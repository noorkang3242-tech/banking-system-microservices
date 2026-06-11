import { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, App as AntApp } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { txnApi, errMsg } from '../api/endpoints'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()
const typeColor = { DEPOSIT: 'green', WITHDRAWAL: 'red', TRANSFER_IN: 'green', TRANSFER_OUT: 'orange' }

export default function Transactions() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    txnApi.me()
      .then((r) => setRows(r.data))
      .catch((e) => message.error('Failed to load transactions: ' + errMsg(e)))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  return (
    <Card
      title="Transaction History"
      extra={<Button icon={<ReloadOutlined />} onClick={load} loading={loading}>Refresh</Button>}
    >
      <Table
        rowKey="transactionId"
        loading={loading}
        dataSource={rows}
        columns={[
          { title: 'Type', dataIndex: 'type', render: (t) => <Tag color={typeColor[t]}>{t}</Tag>, filters: Object.keys(typeColor).map((k) => ({ text: k, value: k })), onFilter: (v, r) => r.type === v },
          { title: 'Account', dataIndex: 'accountNumber' },
          { title: 'Amount', dataIndex: 'amount', render: (v) => money(v), sorter: (a, b) => a.amount - b.amount },
          { title: 'Balance After', dataIndex: 'balanceAfter', render: (v) => money(v) },
          { title: 'Description', dataIndex: 'description', render: (d) => d || '-' },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'SUCCESS' ? 'green' : 'default'}>{s}</Tag> },
          { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-', sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt), defaultSortOrder: 'descend' },
        ]}
      />
    </Card>
  )
}
