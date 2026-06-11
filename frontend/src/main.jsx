import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider, App as AntApp } from 'antd'
import 'antd/dist/reset.css'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './auth/AuthContext.jsx'
import { LanguageProvider, useLang } from './i18n/LanguageContext.jsx'

// reads the chosen language so antd flips to RTL for Urdu / Arabic
function Root() {
  const { dir } = useLang()
  return (
    <ConfigProvider direction={dir} theme={{ token: { colorPrimary: '#fb8c00', colorInfo: '#fb8c00', borderRadius: 8 } }}>
      <AntApp>
        <BrowserRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  )
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LanguageProvider>
      <Root />
    </LanguageProvider>
  </StrictMode>,
)
