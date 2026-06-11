import { useState } from 'react'
import { Form, Input, Button, Steps, Radio, InputNumber, DatePicker, Checkbox, Descriptions, Space, Divider, App as AntApp } from 'antd'
import { UserOutlined, BankOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { customerApi, accountApi, errMsg } from '../api/endpoints'
import Logo from '../components/Logo'
import LanguageSwitcher from '../components/LanguageSwitcher'
import { useLang } from '../i18n/LanguageContext'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()

const STEPS = [
  { title: 'Personal', icon: <UserOutlined />, fields: ['firstName', 'lastName', 'dateOfBirth', 'phone', 'address'] },
  { title: 'Account', icon: <BankOutlined />, fields: ['accountType', 'initialDeposit'] },
  { title: 'Security', icon: <LockOutlined />, fields: ['email', 'password', 'confirm'] },
  { title: 'Review', icon: <SafetyCertificateOutlined />, fields: ['agree'] },
]

export default function Register() {
  const { register } = useAuth()
  const { t } = useLang()
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const [form] = Form.useForm()
  const [current, setCurrent] = useState(0)
  const [loading, setLoading] = useState(false)

  const next = async () => {
    try {
      await form.validateFields(STEPS[current].fields)
      setCurrent((c) => c + 1)
    } catch { /* validation errors are shown inline */ }
  }
  const back = () => setCurrent((c) => c - 1)

  const submit = async () => {
    try {
      await form.validateFields(['agree'])
    } catch { return }
    const v = form.getFieldsValue(true)
    setLoading(true)
    try {
      // 1) create the login (this also signs the user in)
      await register(v.email, v.password)
      // 2) save the customer profile
      try {
        await customerApi.createProfile({
          firstName: v.firstName,
          lastName: v.lastName,
          phone: v.phone,
          address: v.address,
          dateOfBirth: v.dateOfBirth ? v.dateOfBirth.format('YYYY-MM-DD') : undefined,
        })
      } catch (e) { message.warning('Profile could not be saved: ' + errMsg(e)) }
      // 3) open the first account
      try {
        await accountApi.open({ accountType: v.accountType, initialDeposit: v.initialDeposit || 0 })
      } catch (e) { message.warning('Account could not be opened: ' + errMsg(e)) }

      message.success('Welcome to NOOR Bank! Your account is ready.')
      navigate('/dashboard')
    } catch (e) {
      message.error(errMsg(e))
    } finally {
      setLoading(false)
    }
  }

  const v = form.getFieldsValue(true)

  return (
    <div className="auth-wrap">
      <div className="auth-card" style={{ maxWidth: 540 }}>
        <div style={{ height: 88, margin: '-32px -32px 16px', backgroundImage: "url('/bank-building.jpg')", backgroundSize: 'cover', backgroundPosition: 'center', borderRadius: '16px 16px 0 0', position: 'relative' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(35,20,6,0.15), rgba(35,20,6,0.6))', borderRadius: '16px 16px 0 0' }} />
        </div>
        <div style={{ textAlign: 'center', marginBottom: 6 }}><Logo height={44} /></div>
        <p style={{ textAlign: 'center', color: '#888', marginTop: 0, marginBottom: 12 }}>{t('register.title')}</p>
        <div style={{ marginBottom: 16 }}><LanguageSwitcher size="small" /></div>

        <Steps current={current} size="small" items={STEPS.map((s) => ({ title: s.title, icon: s.icon }))} style={{ marginBottom: 22 }} />

        <Form form={form} layout="vertical" requiredMark={false} initialValues={{ accountType: 'SAVINGS', initialDeposit: 0 }}>
          {/* Step 1 — Personal information */}
          <div style={{ display: current === 0 ? 'block' : 'none' }}>
            <Space style={{ width: '100%' }} styles={{ item: { flex: 1 } }}>
              <Form.Item name="firstName" label="First Name" rules={[{ required: true, message: 'Enter your first name' }]}>
                <Input size="large" placeholder="e.g. Ali" />
              </Form.Item>
              <Form.Item name="lastName" label="Last Name" rules={[{ required: true, message: 'Enter your last name' }]}>
                <Input size="large" placeholder="e.g. Khan" />
              </Form.Item>
            </Space>
            <Form.Item name="dateOfBirth" label="Date of Birth" rules={[{ required: true, message: 'Select your date of birth' }]}>
              <DatePicker size="large" style={{ width: '100%' }} format="YYYY-MM-DD" disabledDate={(d) => d && d.valueOf() > Date.now()} />
            </Form.Item>
            <Form.Item name="phone" label="Phone Number" rules={[{ required: true, message: 'Enter your phone number' }]}>
              <Input size="large" placeholder="03XXXXXXXXX" />
            </Form.Item>
            <Form.Item name="address" label="Residential Address">
              <Input.TextArea rows={2} placeholder="House, street, city" />
            </Form.Item>
          </div>

          {/* Step 2 — Account type */}
          <div style={{ display: current === 1 ? 'block' : 'none' }}>
            <Form.Item name="accountType" label="Choose your account type" rules={[{ required: true, message: 'Please choose an account type' }]}>
              <Radio.Group style={{ width: '100%' }}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Radio value="SAVINGS" style={{ padding: '6px 0' }}><b>Savings Account</b> — ideal for saving, earns profit on balance</Radio>
                  <Radio value="CURRENT" style={{ padding: '6px 0' }}><b>Current Account</b> — best for frequent, day-to-day transactions</Radio>
                </Space>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="initialDeposit" label="Opening Deposit (optional)">
              <InputNumber min={0} step={1000} style={{ width: '100%' }} size="large" prefix="PKR" placeholder="0" />
            </Form.Item>
          </div>

          {/* Step 3 — Security / credentials */}
          <div style={{ display: current === 2 ? 'block' : 'none' }}>
            <Form.Item name="email" label="Email Address" rules={[{ required: true, type: 'email', message: 'Enter a valid email' }]}>
              <Input size="large" placeholder="you@example.com" autoComplete="email" />
            </Form.Item>
            <Form.Item name="password" label="Password" rules={[{ required: true, min: 6, message: 'At least 6 characters' }]}>
              <Input.Password size="large" placeholder="••••••••" autoComplete="new-password" />
            </Form.Item>
            <Form.Item name="confirm" label="Confirm Password" dependencies={['password']} rules={[
              { required: true, message: 'Re-enter your password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) return Promise.resolve()
                  return Promise.reject(new Error('Passwords do not match'))
                },
              }),
            ]}>
              <Input.Password size="large" placeholder="••••••••" autoComplete="new-password" />
            </Form.Item>
          </div>

          {/* Step 4 — Review & confirm */}
          <div style={{ display: current === 3 ? 'block' : 'none' }}>
            <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Name">{v.firstName} {v.lastName}</Descriptions.Item>
              <Descriptions.Item label="Date of Birth">{v.dateOfBirth ? v.dateOfBirth.format('YYYY-MM-DD') : '-'}</Descriptions.Item>
              <Descriptions.Item label="Phone">{v.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label="Address">{v.address || '-'}</Descriptions.Item>
              <Descriptions.Item label="Account Type">{v.accountType}</Descriptions.Item>
              <Descriptions.Item label="Opening Deposit">{money(v.initialDeposit)}</Descriptions.Item>
              <Descriptions.Item label="Email">{v.email}</Descriptions.Item>
            </Descriptions>
            <Form.Item name="agree" valuePropName="checked" rules={[{ validator: (_, val) => val ? Promise.resolve() : Promise.reject(new Error('Please accept the terms to continue')) }]}>
              <Checkbox>I agree to NOOR Bank's terms, policies and that the information above is correct.</Checkbox>
            </Form.Item>
          </div>
        </Form>

        <Divider style={{ margin: '8px 0 16px' }} />
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
          <Button onClick={back} disabled={current === 0} size="large">Back</Button>
          {current < STEPS.length - 1
            ? <Button type="primary" onClick={next} size="large">Next</Button>
            : <Button type="primary" onClick={submit} loading={loading} size="large">Create Account</Button>}
        </div>

        <p style={{ textAlign: 'center', marginTop: 16 }}>{t('register.haveAccount')} <Link to="/login">{t('common.signIn')}</Link></p>
      </div>
    </div>
  )
}
