import { useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, Select, InputNumber, Tag, Space, App as AntApp } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { accountApi, errMsg } from '../api/endpoints'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()

export default function Accounts() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [openModal, setOpenModal] = useState(false)
  const [moneyModal, setMoneyModal] = useState(null) // { account, mode }
  const [form] = Form.useForm()
  const [mForm] = Form.useForm()
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    accountApi.me().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const doOpen = async (v) => {
    setBusy(true)
    try {
      await accountApi.open({ accountType: v.accountType, initialDeposit: v.initialDeposit || 0 })
      message.success('Account opened')
      setOpenModal(false); form.resetFields(); load()
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  const doMoney = async (v) => {
    setBusy(true)
    try {
      const { account, mode } = moneyModal
      if (mode === 'deposit') await accountApi.deposit(account.accountNumber, v.amount)
      else await accountApi.withdraw(account.accountNumber, v.amount)
      message.success(`${mode === 'deposit' ? 'Deposited' : 'Withdrew'} ${money(v.amount)}`)
      setMoneyModal(null); mForm.resetFields(); load()
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <Card
      title="My Accounts"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setOpenModal(true)}>Open Account</Button>}
    >
      <Table
        rowKey="accountNumber"
        loading={loading}
        dataSource={rows}
        columns={[
          { title: 'Account Number', dataIndex: 'accountNumber' },
          { title: 'Type', dataIndex: 'accountType', render: (t) => <Tag color="blue">{t}</Tag> },
          { title: 'Balance', dataIndex: 'balance', render: (v) => <b>{money(v)}</b> },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={s === 'ACTIVE' ? 'green' : 'default'}>{s}</Tag> },
          {
            title: 'Actions', render: (_, r) => (
              <Space>
                <Button size="small" onClick={() => setMoneyModal({ account: r, mode: 'deposit' })}>Deposit</Button>
                <Button size="small" onClick={() => setMoneyModal({ account: r, mode: 'withdraw' })}>Withdraw</Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal title="Open New Account" open={openModal} onCancel={() => setOpenModal(false)} onOk={() => form.submit()} confirmLoading={busy} okText="Open">
        <Form form={form} layout="vertical" onFinish={doOpen}>
          <Form.Item name="accountType" label="Account Type" rules={[{ required: true }]}>
            <Select options={[{ value: 'SAVINGS', label: 'Savings' }, { value: 'CURRENT', label: 'Current' }]} placeholder="Select type" />
          </Form.Item>
          <Form.Item name="initialDeposit" label="Initial Deposit (optional)">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="0" prefix="PKR" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={moneyModal ? `${moneyModal.mode === 'deposit' ? 'Deposit to' : 'Withdraw from'} ${moneyModal.account.accountNumber}` : ''}
        open={!!moneyModal} onCancel={() => setMoneyModal(null)} onOk={() => mForm.submit()} confirmLoading={busy} okText="Confirm"
      >
        <Form form={mForm} layout="vertical" onFinish={doMoney}>
          <Form.Item name="amount" label="Amount" rules={[{ required: true, type: 'number', min: 0.01 }]}>
            <InputNumber min={0.01} style={{ width: '100%' }} prefix="PKR" placeholder="Enter amount" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
