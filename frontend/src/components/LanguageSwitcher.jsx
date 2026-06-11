import { Segmented, Dropdown } from 'antd'
import { GlobalOutlined, DownOutlined } from '@ant-design/icons'
import { useLang } from '../i18n/LanguageContext'

export const LANGS = [
  { value: 'en', label: 'English', flag: '🇬🇧' },
  { value: 'ur', label: 'اردو', flag: '🇵🇰' },
  { value: 'ar', label: 'العربية', flag: '🇸🇦' },
  { value: 'hi', label: 'हिन्दी', flag: '🇮🇳' },
]

// compact = a small globe dropdown for the header; otherwise a Segmented row (login page).
export default function LanguageSwitcher({ compact, size }) {
  const { lang, setLang } = useLang()
  const current = LANGS.find((l) => l.value === lang) || LANGS[0]

  if (compact) {
    return (
      <Dropdown
        menu={{
          selectedKeys: [lang],
          items: LANGS.map((l) => ({ key: l.value, label: `${l.flag}  ${l.label}`, onClick: () => setLang(l.value) })),
        }}
      >
        <span className="sky-text hdr-pill" style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontWeight: 700 }}>
          <GlobalOutlined style={{ fontSize: 18 }} />
          <span className="hide-sm">{current.flag} {current.label}</span>
          <DownOutlined style={{ fontSize: 10 }} />
        </span>
      </Dropdown>
    )
  }

  return (
    <Segmented
      size={size}
      value={lang}
      onChange={setLang}
      block
      options={LANGS.map((l) => ({ value: l.value, label: `${l.flag} ${l.label}` }))}
    />
  )
}
