import { useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, Select, InputNumber, Input, Tag, Alert, App as AntApp } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { loanApi, accountApi, errMsg } from '../api/endpoints'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString(undefined, { maximumFractionDigits: 0 })
const statusColor = { PENDING: 'gold', APPROVED: 'blue', ACTIVE: 'green', REJECTED: 'red', CLOSED: 'default' }

// Loan products aligned with State Bank of Pakistan schemes. `base` = indicative
// annual rate (% p.a.) near the SBP policy rate; the final rate also rises a little
// with a longer term (bank policy). Rates are indicative — adjust as SBP updates.
const LOAN_TYPES = [
  { value: 'PERSONAL', label: 'Personal Loan', base: 11 },
  { value: 'HOME', label: 'Home / Housing Finance', base: 12 },
  { value: 'CAR', label: 'Car / Auto Loan', base: 13 },
  { value: 'SME', label: 'SME / Business Finance', base: 13 },
  { value: 'AGRI', label: 'Agriculture Loan', base: 10 },
  { value: 'EDU', label: 'Education / Student Loan', base: 8 },
  { value: 'GOLD', label: 'Gold-backed Loan', base: 11.5 },
]

// term adjustment: longer term -> slightly higher rate
const termSurcharge = (m) => (m <= 12 ? 0 : m <= 24 ? 1 : m <= 36 ? 2 : m <= 60 ? 3 : 4)

const computeRate = (typeValue, months) => {
  const t = LOAN_TYPES.find((x) => x.value === typeValue)
  if (!t || !months) return null
  return +(t.base + termSurcharge(months)).toFixed(2)
}

export default function Loans() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [applyOpen, setApplyOpen] = useState(false)
  const [repay, setRepay] = useState(null)
  const [busy, setBusy] = useState(false)
  const [form] = Form.useForm()
  const [rForm] = Form.useForm()

  const loanType = Form.useWatch('loanType', form)
  const principal = Form.useWatch('principal', form)
  const termMonths = Form.useWatch('termMonths', form)
  const rate = computeRate(loanType, termMonths)

  // auto-fill the interest rate whenever product / term changes
  useEffect(() => {
    form.setFieldValue('interestRate', rate ?? undefined)
  }, [rate, form])

  // live estimate using the SAME formula the backend uses (simple interest)
  let est = null
  if (principal > 0 && rate != null && termMonths > 0) {
    const interest = principal * (rate / 100) * (termMonths / 12)
    const total = principal + interest
    est = { interest, total, monthly: total / termMonths }
  }

  const load = () => {
    setLoading(true)
    loanApi.me().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(() => { load(); accountApi.me().then((r) => setAccounts(r.data)).catch(() => {}) }, [])

  const doApply = async (v) => {
    setBusy(true)
    try {
      const typeLabel = LOAN_TYPES.find((t) => t.value === v.loanType)?.label || 'Loan'
      await loanApi.apply({
        accountNumber: v.accountNumber,
        principal: v.principal,
        termMonths: v.termMonths,
        interestRate: v.interestRate, // auto-computed per policy
        purpose: `${typeLabel}${v.purpose ? ' — ' + v.purpose : ''}`,
      })
      message.success('Loan application submitted')
      setApplyOpen(false); form.resetFields(); load()
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  const doRepay = async (v) => {
    setBusy(true)
    try {
      await loanApi.repay(repay.loanId, v.amount)
      message.success('Payment recorded')
      setRepay(null); rForm.resetFields(); load()
    } catch (e) { message.error(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <Card title="My Loans" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setApplyOpen(true)}>Apply for Loan</Button>}>
      <Table
        rowKey="loanId"
        loading={loading}
        dataSource={rows}
        scroll={{ x: 800 }}
        columns={[
          { title: 'Purpose', dataIndex: 'purpose', render: (p) => p || '-' },
          { title: 'Principal', dataIndex: 'principal', render: (v) => money(v) },
          { title: 'Rate', dataIndex: 'interestRate', render: (v) => `${v}%` },
          { title: 'Total Payable', dataIndex: 'totalPayable', render: (v) => money(v) },
          { title: 'Outstanding', dataIndex: 'outstanding', render: (v) => <b>{money(v)}</b> },
          { title: 'Term', dataIndex: 'termMonths', render: (v) => `${v} mo` },
          { title: 'Status', dataIndex: 'status', render: (s) => <Tag color={statusColor[s]}>{s}</Tag> },
          {
            title: 'Action', render: (_, r) => r.status === 'ACTIVE'
              ? <Button size="small" type="primary" ghost onClick={() => setRepay(r)}>Repay</Button>
              : '-',
          },
        ]}
      />

      <Modal title="Apply for a Loan" open={applyOpen} onCancel={() => setApplyOpen(false)} onOk={() => form.submit()} confirmLoading={busy} okText="Submit" width={560}>
        <Form form={form} layout="vertical" onFinish={doApply}>
          <Form.Item name="loanType" label="Loan Type (State Bank of Pakistan product)" rules={[{ required: true, message: 'Please select a loan type' }]}>
            <Select
              placeholder="Select a loan product"
              options={LOAN_TYPES.map((t) => ({ value: t.value, label: `${t.label} — from ${t.base}% p.a.` }))}
            />
          </Form.Item>
          <Form.Item name="accountNumber" label="Disbursement Account" rules={[{ required: true }]}>
            <Select placeholder="Select account" options={accounts.map((a) => ({ value: a.accountNumber, label: `${a.accountType} - ${a.accountNumber}` }))} />
          </Form.Item>
          <Form.Item name="principal" label="Loan Amount" rules={[{ required: true, type: 'number', min: 1000 }]}>
            <InputNumber min={1000} step={1000} style={{ width: '100%' }} prefix="PKR" placeholder="Min 1,000" />
          </Form.Item>
          <Form.Item name="termMonths" label="Term (months)" rules={[{ required: true, type: 'number', min: 1, max: 360 }]}>
            <InputNumber min={1} max={360} style={{ width: '100%' }} placeholder="e.g. 14" />
          </Form.Item>
          <Form.Item name="interestRate" label="Interest Rate (auto — per bank policy)">
            <InputNumber style={{ width: '100%' }} disabled addonAfter="% p.a." />
          </Form.Item>

          {est && (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 12 }}
              message={`Interest auto-applied: ${rate}% p.a.`}
              description={
                <div style={{ lineHeight: 1.9 }}>
                  <div>Loan amount: <b>{money(principal)}</b></div>
                  <div>Total interest ({termMonths} mo): <b>{money(est.interest)}</b></div>
                  <div>Total payable: <b style={{ color: '#fa541c' }}>{money(est.total)}</b></div>
                  <div>Approx. monthly installment: <b>{money(est.monthly)}</b></div>
                </div>
              }
            />
          )}

          <Form.Item name="purpose" label="Note (optional)">
            <Input.TextArea rows={2} placeholder="Any extra detail about the loan" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Repay Loan" open={!!repay} onCancel={() => setRepay(null)} onOk={() => rForm.submit()} confirmLoading={busy} okText="Pay">
        {repay && <p>Outstanding: <b>{money(repay.outstanding)}</b></p>}
        <Form form={rForm} layout="vertical" onFinish={doRepay}>
          <Form.Item name="amount" label="Payment Amount" rules={[{ required: true, type: 'number', min: 0.01 }]}>
            <InputNumber min={0.01} style={{ width: '100%' }} prefix="PKR" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
