import { useEffect, useState } from 'react'
import { Layout, Menu, Button, Avatar, Badge, Dropdown, Grid, Space } from 'antd'
import {
  DashboardOutlined, BankOutlined, SwapOutlined, HistoryOutlined,
  DollarOutlined, CreditCardOutlined, BellOutlined, SafetyOutlined,
  LogoutOutlined, UserOutlined, MenuOutlined, MailOutlined, WhatsAppOutlined, EnvironmentOutlined,
} from '@ant-design/icons'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { notifApi } from '../api/endpoints'
import { seasonLabel } from './WeatherBackground'
import Logo from './Logo'
import Policies from './Policies'
import LanguageSwitcher from './LanguageSwitcher'
import { useLang } from '../i18n/LanguageContext'

const { Header, Sider, Content } = Layout

export default function AppLayout() {
  const { user, isStaff, logout } = useAuth()
  const { t } = useLang()
  const navigate = useNavigate()
  const location = useLocation()
  const screens = Grid.useBreakpoint()
  const [collapsed, setCollapsed] = useState(false)
  const [unread, setUnread] = useState(0)
  const [now, setNow] = useState(new Date())

  useEffect(() => {
    notifApi.unreadCount().then((r) => setUnread(r.data.unread || 0)).catch(() => {})
  }, [location.pathname])

  // live clock for the header
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const dateStr = now.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })

  const items = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: t('nav.dashboard') },
    { key: '/accounts', icon: <BankOutlined />, label: t('nav.accounts') },
    { key: '/transfer', icon: <SwapOutlined />, label: t('nav.transfer') },
    { key: '/transactions', icon: <HistoryOutlined />, label: t('nav.transactions') },
    { key: '/loans', icon: <DollarOutlined />, label: t('nav.loans') },
    { key: '/cards', icon: <CreditCardOutlined />, label: t('nav.cards') },
    { key: '/notifications', icon: <BellOutlined />, label: t('nav.notifications') },
    ...(isStaff ? [{ key: '/admin', icon: <SafetyOutlined />, label: t('nav.admin') }] : []),
  ]

  return (
    <Layout style={{ minHeight: '100vh', background: 'transparent' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        collapsedWidth={screens.lg ? 80 : 0}
        style={{ background: 'rgba(40, 24, 8, 0.92)' }}
      >
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 8px' }}>
          <Logo light markOnly={collapsed} height={collapsed ? 36 : 40} />
        </div>
        <Menu
          theme="dark"
          mode="inline"
          style={{ background: 'transparent' }}
          selectedKeys={[location.pathname]}
          items={items}
          onClick={(e) => navigate(e.key)}
        />
      </Sider>
      <Layout>
        <Header className="sky-header">
          {/* live animated sky */}
          <div className="sky-sun" />
          <div className="cloud" style={{ width: 46, top: 14, animationDuration: '26s' }} />
          <div className="cloud" style={{ width: 64, top: 40, animationDuration: '38s', animationDelay: '4s' }} />
          <div className="cloud" style={{ width: 36, top: 6, animationDuration: '30s', animationDelay: '9s' }} />
          <div className="cloud" style={{ width: 54, top: 54, animationDuration: '46s', animationDelay: '15s' }} />

          {/* sun + heat beating down on a family walking by under one umbrella */}
          <div className="umbrella-guy" aria-hidden="true">
            <span className="sun-mini">☀️</span>
            <span className="heat-beam" />
            <span className="umb">☂️</span>
            <span className="family">
              <span className="guy">🚶</span>
              <span className="guy">🚶‍♀️</span>
              <span className="guy kid">🧒</span>
            </span>
          </div>

          <div className="sky-content">
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Button type="text" icon={<MenuOutlined style={{ color: '#fff', fontSize: 22 }} />} onClick={() => setCollapsed(!collapsed)} />
              <div className="sky-text" style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.28 }}>
                <span style={{ fontSize: 20, fontWeight: 800, letterSpacing: 0.4 }}>🕒 {timeStr} &nbsp;·&nbsp; {dateStr}</span>
                <span style={{ fontSize: 13.5, fontWeight: 600, opacity: 0.97 }}>{t('header.tagline')} · {seasonLabel()}</span>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <LanguageSwitcher compact />
              <Policies />
              <span className="hdr-pill" style={{ display: 'flex', padding: '8px 11px', cursor: 'pointer' }} onClick={() => navigate('/notifications')}>
                <Badge count={unread} size="small">
                  <BellOutlined className="sky-text" style={{ fontSize: 22 }} />
                </Badge>
              </span>
              <Dropdown menu={{ items: [{ key: 'out', icon: <LogoutOutlined />, label: t('common.logout'), onClick: () => { logout(); navigate('/login') } }] }}>
                <span className="sky-text hdr-pill" style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 9 }}>
                  <Avatar size={32} icon={<UserOutlined />} style={{ background: '#fff', color: '#ff6f3d' }} />
                  <span style={{ fontWeight: 700, fontSize: 14 }} className="hide-sm">{user?.email}</span>
                  {isStaff && <span style={{ background: 'rgba(255,255,255,0.35)', color: '#fff', fontSize: 12, fontWeight: 700, padding: '2px 8px', borderRadius: 6 }}>{user.role}</span>}
                </span>
              </Dropdown>
            </div>
          </div>
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
        <Layout.Footer style={{ textAlign: 'center', background: 'rgba(255,255,255,0.85)', backdropFilter: 'blur(6px)' }}>
          <div style={{ fontWeight: 700, marginBottom: 6, color: '#fb8c00' }}>☀️ {t('footer.connect')}</div>
          <Space size="large" wrap style={{ justifyContent: 'center' }}>
            <a href="mailto:noorkang3242@gmail.com" style={{ color: '#555' }}><MailOutlined /> noorkang3242@gmail.com</a>
            <a href="https://wa.me/923233522940" target="_blank" rel="noreferrer" style={{ color: '#25D366', fontWeight: 600 }}><WhatsAppOutlined /> +92 323 3522940</a>
            <a href="https://www.google.com/maps/search/?api=1&query=Chak+199P+Sadiqabad+Punjab" target="_blank" rel="noreferrer" style={{ color: '#1677ff', fontWeight: 600 }}><EnvironmentOutlined /> Chak 199P, Sadiqabad</a>
          </Space>
          <div style={{ marginTop: 8, fontSize: 12, color: '#999' }}>© {new Date().getFullYear()} NOOR Bank · Head Office: Chak No. 199P, Sadiqabad, Punjab, Pakistan · {t('footer.rights')}</div>
        </Layout.Footer>
      </Layout>
    </Layout>
  )
}
