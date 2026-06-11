import { useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, Select, Input, InputNumber, Tag, Space, Alert, Typography, App as AntApp } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { cardApi, accountApi, errMsg } from '../api/endpoints'

const statusColor = { ACTIVE: 'green', BLOCKED: 'red', EXPIRED: 'default' }

export default function Cards() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [issueOpen, setIssueOpen] = useState(false)
  const [issued, setIssued] = useState(null) // full number + cvv shown once
  const [busy, setBusy] = useState(false)
  const [form] = Form.useForm()
  const cardType = Form.useWatch('cardType', form)

  const load = () => {
    setLoading(true)
    cardApi.me().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load(); accountApi.me().then((r) => setAccounts(r.data)).catch(() => {}) }, [])

  const doIssue = async (v) => {
    setBusy(true)
    try {
      const body = { cardType: v.cardType, cardholderName: v.cardholderName }
      if (v.cardType === 'DEBIT') body.accountNumber = v.accountNumber
      if (v.cardType === 'CREDIT') body.creditLimit = v.creditLimit
      const res = await cardApi.issue(body)
      setIssued(res.data)
      setIssueOpen(false); form.resetFields(); load()
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  const toggle = async (r) => {
    try {
      if (r.status === 'BLOCKED') { await cardApi.unblock(r.cardId); message.success('Card unblocked') }
      else { await cardApi.block(r.cardId); message.success('Card blocked') }
      load()
    } catch (e) { message.error(errMsg(e)) }
  }

  return (
    <Card title="My Cards" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setIssueOpen(true)}>Issue Card</Button>}>
      <Table
        rowKey="cardId"
        loading={loading}
        dataSource={rows}
        scroll={{ x: 700 }}
        columns={[
          { title: 'Card Number', dataIndex: 'maskedNumber' },
          { title: 'Holder', dataIndex: 'cardholderName' },
          { title: 'Type', dataIndex: 'cardType', render: (t) => <Tag color={t === 'CREDIT' ? 'purple' : 'blue'}>{t}</Tag> },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={statusColor[s]}>{s}</Tag> },
          { title: 'Expiry', dataIndex: 'expiryDate' },
          {
            title: 'Action', render: (_, r) => (
              <Space>
                <Button size="small" danger={r.status !== 'BLOCKED'} disabled={r.status === 'EXPIRED'} onClick={() => toggle(r)}>
                  {r.status === 'BLOCKED' ? 'Unblock' : 'Block'}
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal title="Issue New Card" open={issueOpen} onCancel={() => setIssueOpen(false)} onOk={() => form.submit()} confirmLoading={busy} okText="Issue">
        <Form form={form} layout="vertical" onFinish={doIssue} initialValues={{ cardType: 'DEBIT' }}>
          <Form.Item name="cardType" label="Card Type" rules={[{ required: true }]}>
            <Select options={[{ value: 'DEBIT', label: 'Debit (linked to account)' }, { value: 'CREDIT', label: 'Credit' }]} />
          </Form.Item>
          <Form.Item name="cardholderName" label="Cardholder Name" rules={[{ required: true }]}>
            <Input placeholder="Name on card" />
          </Form.Item>
          {cardType === 'DEBIT' && (
            <Form.Item name="accountNumber" label="Linked Account" rules={[{ required: true }]}>
              <Select placeholder="Select account" options={accounts.map((a) => ({ value: a.accountNumber, label: `${a.accountType} - ${a.accountNumber}` }))} />
            </Form.Item>
          )}
          {cardType === 'CREDIT' && (
            <Form.Item name="creditLimit" label="Credit Limit (optional)">
              <InputNumber min={0} style={{ width: '100%' }} prefix="PKR" placeholder="e.g. 200000" />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal title="Card Issued ✅" open={!!issued} onCancel={() => setIssued(null)} footer={<Button type="primary" onClick={() => setIssued(null)}>I have saved it</Button>}>
        <Alert type="warning" showIcon style={{ marginBottom: 16 }} message="Save these now — the full number and CVV are shown only once." />
        {issued && (
          <div style={{ background: 'linear-gradient(135deg,#1677ff,#0a3d91)', color: '#fff', borderRadius: 12, padding: 20 }}>
            <div style={{ fontSize: 12, opacity: 0.8 }}>{issued.cardType} CARD</div>
            <Typography.Title level={3} copyable style={{ color: '#fff', letterSpacing: 2, margin: '12px 0' }}>{issued.cardNumber}</Typography.Title>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>{issued.cardholderName}</span>
              <span>CVV: <b>{issued.cvv}</b></span>
              <span>Exp: {issued.expiryDate}</span>
            </div>
          </div>
        )}
      </Modal>
    </Card>
  )
}
