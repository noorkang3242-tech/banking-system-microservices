import { Modal, Button } from 'antd'
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons'

const money = (n) => 'PKR ' + Number(n || 0).toLocaleString()
const fmt = (d) => (d ? new Date(d).toLocaleString() : new Date().toLocaleString())

// Self-contained branded slip — used for both download (.html) and print/Save-as-PDF.
function buildHtml(d) {
  const benRow = d.beneficiaryName
    ? `<div class="row"><b>Beneficiary</b><span>${d.beneficiaryName}</span></div>` : ''
  return `<!doctype html><html><head><meta charset="utf-8">
  <title>NOOR Bank Receipt ${d.transferId || ''}</title>
  <style>
    body{font-family:Segoe UI,Arial,sans-serif;background:#f4f6f8;margin:0;padding:24px;color:#2a1606}
    .slip{max-width:440px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.12)}
    .top{background:linear-gradient(120deg,#ff7a00,#ffb300);color:#fff;padding:22px 26px}
    .top h1{margin:0;font-size:20px;letter-spacing:.5px}.top p{margin:4px 0 0;opacity:.95;font-size:13px}
    .body{padding:22px 26px}.amt{text-align:center;margin:6px 0 18px}
    .amt .n{font-size:30px;font-weight:800;color:#2e7d32}
    .row{display:flex;justify-content:space-between;padding:9px 0;border-bottom:1px dashed #eee;font-size:14px}
    .row b{color:#777;font-weight:600}
    .ok{display:inline-block;background:#e8f5e9;color:#2e7d32;padding:2px 10px;border-radius:12px;font-size:12px;font-weight:700}
    .foot{padding:16px 26px;background:#fff8ef;font-size:12px;color:#8a6d3b;text-align:center}
  </style></head><body>
  <div class="slip">
    <div class="top"><h1>☀️ NOOR Bank</h1><p>Fund Transfer Receipt</p></div>
    <div class="body">
      <div class="amt"><div class="n">${money(d.amount)}</div><span class="ok">${d.status || 'COMPLETED'}</span></div>
      <div class="row"><b>Reference</b><span>${d.transferId || '-'}</span></div>
      <div class="row"><b>Date</b><span>${fmt(d.createdAt)}</span></div>
      <div class="row"><b>From Account</b><span>${d.fromAccount || '-'}</span></div>
      <div class="row"><b>To Account</b><span>${d.toAccount || '-'}</span></div>
      ${benRow}
    </div>
    <div class="foot">Thank you for banking with NOOR Bank &middot; Head Office<br>noorkang3242@gmail.com &middot; WhatsApp +92 323 3522940</div>
  </div></body></html>`
}

export default function Receipt({ open, onClose, data }) {
  if (!data) return null

  const download = () => {
    const blob = new Blob([buildHtml(data)], { type: 'text/html' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `NOOR-Receipt-${String(data.transferId || 'transfer').slice(0, 8)}.html`
    document.body.appendChild(a); a.click(); a.remove()
    URL.revokeObjectURL(url)
  }

  const print = () => {
    const w = window.open('', '_blank')
    if (!w) return
    w.document.write(buildHtml(data)); w.document.close(); w.focus()
    setTimeout(() => w.print(), 300)
  }

  const rows = [
    ['Reference', data.transferId || '-'],
    ['Date', fmt(data.createdAt)],
    ['From', data.fromAccount || '-'],
    ['To', data.toAccount || '-'],
    ...(data.beneficiaryName ? [['Beneficiary', data.beneficiaryName]] : []),
  ]

  return (
    <Modal
      open={open}
      onCancel={onClose}
      title="Transfer Receipt"
      footer={[
        <Button key="dl" icon={<DownloadOutlined />} onClick={download}>Save</Button>,
        <Button key="pr" icon={<PrinterOutlined />} onClick={print}>Print / PDF</Button>,
        <Button key="ok" type="primary" onClick={onClose}>Done</Button>,
      ]}
    >
      <div style={{ borderRadius: 12, overflow: 'hidden', border: '1px solid #f0e6d6' }}>
        <div style={{ background: 'linear-gradient(120deg,#ff7a00,#ffb300)', color: '#fff', padding: '18px 22px' }}>
          <div style={{ fontSize: 18, fontWeight: 800 }}>☀️ NOOR Bank</div>
          <div style={{ fontSize: 12, opacity: 0.95 }}>Fund Transfer Receipt</div>
        </div>
        <div style={{ padding: '18px 22px' }}>
          <div style={{ textAlign: 'center', marginBottom: 14 }}>
            <div style={{ fontSize: 26, fontWeight: 800, color: '#2e7d32' }}>{money(data.amount)}</div>
            <span style={{ background: '#e8f5e9', color: '#2e7d32', padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 700 }}>{data.status || 'COMPLETED'}</span>
          </div>
          {rows.map(([k, v]) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px dashed #eee', fontSize: 13 }}>
              <span style={{ color: '#888', fontWeight: 600 }}>{k}</span>
              <span style={{ textAlign: 'right', wordBreak: 'break-all', marginLeft: 12 }}>{v}</span>
            </div>
          ))}
        </div>
      </div>
    </Modal>
  )
}
