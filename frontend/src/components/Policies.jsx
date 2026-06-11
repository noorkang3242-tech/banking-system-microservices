import { useState } from 'react'
import { Modal, Collapse, Button, Typography } from 'antd'
import { SafetyCertificateOutlined } from '@ant-design/icons'

const { Text } = Typography

const li = { margin: '4px 0', lineHeight: 1.55 }

const sections = [
  {
    key: '1',
    label: '🔐 Account Opening & KYC',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Account opening requires a valid identity and complete KYC details.</li>
        <li style={li}>New accounts stay <b>PENDING</b> until KYC is <b>VERIFIED</b> by bank staff.</li>
        <li style={li}>Customers must keep their profile and contact information up to date.</li>
        <li style={li}>The bank may freeze accounts with incomplete or suspicious KYC.</li>
      </ul>
    ),
  },
  {
    key: '2',
    label: '💸 Transactions & Transfers',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Always <b>verify the beneficiary name</b> before confirming a transfer.</li>
        <li style={li}>A completed transfer is <b>final and cannot be reversed</b> — please double-check details.</li>
        <li style={li}>Transactions need sufficient balance; <b>FROZEN</b> accounts cannot deposit or withdraw.</li>
        <li style={li}>Every transaction is recorded and a downloadable receipt is provided.</li>
      </ul>
    ),
  },
  {
    key: '3',
    label: '🔒 Security & Privacy',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>NOOR Bank will <b>never ask for your password</b> via call, email or message.</li>
        <li style={li}>Never share your password or login link with anyone.</li>
        <li style={li}>Sessions are secured; always log out on shared devices.</li>
        <li style={li}>Your personal data is protected and never sold to third parties.</li>
      </ul>
    ),
  },
  {
    key: '4',
    label: '💳 Cards',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Your full card number and CVV are shown <b>only once</b> — store them safely.</li>
        <li style={li}><b>Block</b> a lost or stolen card immediately from the Cards page.</li>
        <li style={li}>You are responsible for card usage until it is reported and blocked.</li>
      </ul>
    ),
  },
  {
    key: '5',
    label: '🏦 Loans',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Loans are subject to eligibility and bank approval.</li>
        <li style={li}>Approved loans are disbursed to your account; interest applies as agreed.</li>
        <li style={li}>Repay on time — defaults may lead to penalties or account freeze.</li>
      </ul>
    ),
  },
  {
    key: '6',
    label: '💰 Fees & Charges',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Basic banking and standard transactions are <b>free of charge</b>.</li>
        <li style={li}>Premium services may carry nominal charges, disclosed before you proceed.</li>
        <li style={li}>No hidden fees — all charges are shown transparently.</li>
      </ul>
    ),
  },
  {
    key: '7',
    label: '📞 Support & Grievance',
    children: (
      <ul style={{ paddingLeft: 18, margin: 0 }}>
        <li style={li}>Reach us anytime: <a href="mailto:noorkang3242@gmail.com">noorkang3242@gmail.com</a></li>
        <li style={li}>WhatsApp: <a href="https://wa.me/923233522940" target="_blank" rel="noreferrer">+92 323 3522940</a></li>
        <li style={li}>Complaints are acknowledged within 24 hours and resolved promptly.</li>
      </ul>
    ),
  },
  {
    key: '8',
    label: '📍 Head Office & Location',
    children: (
      <div>
        <p style={{ margin: '0 0 6px' }}>
          <b>NOOR Bank — Head Office</b><br />
          Chak No. 199P, Sadiqabad, District Rahim Yar Khan, Punjab, Pakistan
        </p>
        <a href="https://www.google.com/maps/search/?api=1&query=Chak+199P+Sadiqabad+Punjab" target="_blank" rel="noreferrer">
          Open in Google Maps →
        </a>
        <div style={{ marginTop: 10, borderRadius: 8, overflow: 'hidden', border: '1px solid #eee' }}>
          <iframe
            title="NOOR Bank Head Office Location"
            src="https://maps.google.com/maps?q=Chak%20199P%20Sadiqabad%20Punjab&t=&z=13&ie=UTF8&iwloc=&output=embed"
            width="100%"
            height="220"
            style={{ border: 0 }}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
          />
        </div>
      </div>
    ),
  },
]

export default function Policies() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <span
        className="sky-text hdr-pill"
        onClick={() => setOpen(true)}
        style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 7, fontSize: 14.5, fontWeight: 700 }}
      >
        <SafetyCertificateOutlined style={{ fontSize: 19 }} />
        <span className="hide-sm">Policies</span>
      </span>

      <Modal
        open={open}
        onCancel={() => setOpen(false)}
        title="🛡️ NOOR Bank — Policies & Terms"
        width={680}
        footer={<Button type="primary" onClick={() => setOpen(false)}>I Understand</Button>}
      >
        <Collapse accordion defaultActiveKey={['1']} items={sections} />
        <div style={{ marginTop: 14, textAlign: 'center' }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            These policies help keep your banking safe. NOOR Bank · Head Office · noorkang3242@gmail.com
          </Text>
        </div>
      </Modal>
    </>
  )
}
