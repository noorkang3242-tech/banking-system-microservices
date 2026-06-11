import { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Badge, Empty, App as AntApp } from 'antd'
import { CheckOutlined } from '@ant-design/icons'
import { notifApi, errMsg } from '../api/endpoints'

const typeColor = { TRANSACTION: 'blue', TRANSFER: 'cyan', LOAN: 'gold', CARD: 'purple', SECURITY: 'red', GENERAL: 'default' }

export default function Notifications() {
  const { message } = AntApp.useApp()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    notifApi.me().then((r) => setRows(r.data)).catch((e) => message.error(errMsg(e))).finally(() => setLoading(false))
  }
  useEffect(load, [])

  const markRead = async (id) => { try { await notifApi.read(id); load() } catch (e) { message.error(errMsg(e)) } }
  const markAll = async () => { try { await notifApi.readAll(); message.success('All marked read'); load() } catch (e) { message.error(errMsg(e)) } }

  const unread = rows.filter((n) => n.status === 'UNREAD').length

  return (
    <Card
      title={<span>Notifications {unread > 0 && <Badge count={unread} />}</span>}
      extra={<Button onClick={markAll} disabled={unread === 0}>Mark all read</Button>}
    >
      {rows.length === 0 && !loading ? <Empty description="No notifications" /> : (
        <List
          loading={loading}
          dataSource={rows}
          renderItem={(n) => (
            <List.Item
              style={{ background: n.status === 'UNREAD' ? '#e6f4ff' : '#fff', padding: '12px 16px', borderRadius: 8, marginBottom: 8 }}
              actions={n.status === 'UNREAD' ? [<Button key="r" size="small" type="text" icon={<CheckOutlined />} onClick={() => markRead(n.notificationId)}>Read</Button>] : []}
            >
              <List.Item.Meta
                title={<span><Tag color={typeColor[n.type]}>{n.type}</Tag> {n.title}</span>}
                description={n.message}
              />
              <span style={{ color: '#999', fontSize: 12 }}>{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</span>
            </List.Item>
          )}
        />
      )}
    </Card>
  )
}
