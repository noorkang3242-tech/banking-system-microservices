import { useEffect, useState } from 'react'
import { Row, Col, Card, Form, Select, Input, InputNumber, Button, Table, Tag, Alert, App as AntApp } from 'antd'
import { UserOutlined, FileTextOutlined } from '@ant-design/icons'
import { accountApi, transferApi, errMsg } from '../api/endpoints'
import Receipt from '../components/Receipt'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()

export default function Transfer() {
  const { message } = AntApp.useApp()
  const [accounts, setAccounts] = useState([])
  const [transfers, setTransfers] = useState([])
  const [busy, setBusy] = useState(false)
  const [verifying, setVerifying] = useState(false)
  const [beneficiary, setBeneficiary] = useState(null)
  const [receipt, setReceipt] = useState(null)
  const [form] = Form.useForm()
  const toAccount = Form.useWatch('toAccount', form)

  const loadTransfers = () => transferApi.me().then((r) => setTransfers(r.data)).catch(() => {})
  useEffect(() => {
    accountApi.me().then((r) => setAccounts(r.data)).catch(() => {})
    loadTransfers()
  }, [])

  // changing the destination forces re-verification
  useEffect(() => { setBeneficiary(null) }, [toAccount])

  const verify = async () => {
    const acc = (toAccount || '').trim()
    if (acc.length < 6) { message.warning('Please enter the recipient account number first'); return }
    setVerifying(true)
    try {
      const r = await accountApi.beneficiary(acc)
      setBeneficiary(r.data)
    } catch (e) {
      setBeneficiary(null)
      message.error(errMsg(e))
    } finally { setVerifying(false) }
  }

  const onFinish = async (v) => {
    if (!beneficiary) { message.warning('Please verify the beneficiary first'); return }
    setBusy(true)
    try {
      const res = await transferApi.create({ fromAccount: v.fromAccount, toAccount: v.toAccount, amount: v.amount })
      message.success(`Sent ${money(v.amount)} to ${beneficiary.holderName || beneficiary.accountNumber}`)
      // show a downloadable receipt for this transfer
      setReceipt({ ...res.data, beneficiaryName: beneficiary.holderName })
      form.resetFields(); setBeneficiary(null); loadTransfers()
      accountApi.me().then((r) => setAccounts(r.data))
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <>
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={10}>
        <Card title="Send Money">
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <Form.Item name="fromAccount" label="From Account" rules={[{ required: true }]}>
              <Select
                placeholder="Select your account"
                options={accounts.map((a) => ({ value: a.accountNumber, label: `${a.accountType} - ${a.accountNumber} (${money(a.balance)})` }))}
              />
            </Form.Item>

            <Form.Item name="toAccount" label="To Account Number" rules={[{ required: true }]}>
              <Input.Search
                placeholder="Recipient account number"
                enterButton={verifying ? 'Checking…' : 'Verify'}
                loading={verifying}
                onSearch={verify}
              />
            </Form.Item>

            {beneficiary && (
              <Alert
                type={beneficiary.status === 'ACTIVE' ? 'success' : 'warning'}
                showIcon
                style={{ marginBottom: 16 }}
                message={<span><UserOutlined /> &nbsp;{beneficiary.holderName || 'Account holder'}</span>}
                description={
                  <span>
                    {beneficiary.accountType} account · {beneficiary.accountNumber} ·{' '}
                    <Tag color={beneficiary.status === 'ACTIVE' ? 'green' : 'orange'}>{beneficiary.status}</Tag>
                  </span>
                }
              />
            )}

            <Form.Item name="amount" label="Amount" rules={[{ required: true, type: 'number', min: 0.01 }]}>
              <InputNumber min={0.01} style={{ width: '100%' }} prefix="PKR" placeholder="Amount" disabled={!beneficiary} />
            </Form.Item>

            <Button type="primary" htmlType="submit" block loading={busy} disabled={!beneficiary}>
              {beneficiary ? `Transfer to ${beneficiary.holderName || beneficiary.accountNumber}` : 'Verify beneficiary first'}
            </Button>
          </Form>
        </Card>
      </Col>
      <Col xs={24} lg={14}>
        <Card title="My Transfers">
          <Table
            rowKey="transferId"
            dataSource={transfers}
            size="small"
            columns={[
              { title: 'From', dataIndex: 'fromAccount' },
              { title: 'To', dataIndex: 'toAccount' },
              { title: 'Amount', dataIndex: 'amount', render: (v) => money(v) },
              { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'COMPLETED' ? 'green' : 'red'}>{s}</Tag> },
              { title: 'Date', dataIndex: 'createdAt', render: (d) => d ? new Date(d).toLocaleString() : '-' },
              { title: 'Receipt', key: 'receipt', render: (_, row) => <Button size="small" icon={<FileTextOutlined />} onClick={() => setReceipt(row)}>Slip</Button> },
            ]}
          />
        </Card>
      </Col>
    </Row>
    <Receipt open={!!receipt} data={receipt} onClose={() => setReceipt(null)} />
    </>
  )
}
