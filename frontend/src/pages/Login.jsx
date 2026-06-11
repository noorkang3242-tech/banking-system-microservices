import { useState } from 'react'
import { Form, Input, Button, App as AntApp } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errMsg } from '../api/endpoints'
import Logo from '../components/Logo'
import LanguageSwitcher from '../components/LanguageSwitcher'
import { useLang } from '../i18n/LanguageContext'

export default function Login() {
  const { login } = useAuth()
  const { t } = useLang()
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const [loading, setLoading] = useState(false)

  const onFinish = async (v) => {
    setLoading(true)
    try {
      await login(v.email, v.password)
      message.success(t('login.welcome'))
      navigate('/dashboard')
    } catch (e) {
      message.error(errMsg(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div style={{ height: 96, margin: '-32px -32px 18px', backgroundImage: "url('/bank-building.jpg')", backgroundSize: 'cover', backgroundPosition: 'center', borderRadius: '16px 16px 0 0', position: 'relative' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(180deg, rgba(35,20,6,0.15), rgba(35,20,6,0.6))', borderRadius: '16px 16px 0 0' }} />
        </div>
        <div style={{ textAlign: 'center', marginBottom: 6 }}><Logo height={48} /></div>
        <p style={{ textAlign: 'center', color: '#888', marginTop: 0, marginBottom: 14 }}>{t('login.subtitle')}</p>
        <div style={{ marginBottom: 18 }}><LanguageSwitcher size="small" /></div>
        <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
          <Form.Item name="email" label={t('common.email')} rules={[{ required: true, type: 'email', message: 'Enter a valid email' }]}>
            <Input size="large" placeholder="you@example.com" autoComplete="email" />
          </Form.Item>
          <Form.Item name="password" label={t('common.password')} rules={[{ required: true, message: 'Enter your password' }]}>
            <Input.Password size="large" placeholder="••••••••" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block loading={loading}>{t('common.signIn')}</Button>
        </Form>
        <p style={{ textAlign: 'center', marginTop: 16 }}>{t('login.noAccount')} <Link to="/register">{t('login.createOne')}</Link></p>
        <div style={{ marginTop: 12, textAlign: 'center', fontSize: 12, color: '#888' }}>
          {t('login.needHelp')} <a href="mailto:noorkang3242@gmail.com">Email</a> &nbsp;·&nbsp;
          <a href="https://wa.me/923233522940" target="_blank" rel="noreferrer" style={{ color: '#25D366', fontWeight: 600 }}>WhatsApp 0323-3522940</a>
        </div>
      </div>
    </div>
  )
}
