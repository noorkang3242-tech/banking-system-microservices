import { createContext, useContext, useEffect, useState } from 'react'
import { translations } from './translations'

const RTL_LANGS = ['ur', 'ar']
const LanguageContext = createContext(null)

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(() => localStorage.getItem('lang') || 'en')
  const dir = RTL_LANGS.includes(lang) ? 'rtl' : 'ltr'

  useEffect(() => {
    document.documentElement.lang = lang
    document.documentElement.dir = dir
  }, [lang, dir])

  const setLang = (l) => {
    localStorage.setItem('lang', l)
    setLangState(l)
  }

  // translate: current language -> English fallback -> the key itself
  const t = (key) =>
    (translations[lang] && translations[lang][key]) || translations.en[key] || key

  return (
    <LanguageContext.Provider value={{ lang, setLang, dir, t }}>
      {children}
    </LanguageContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLang() {
  return useContext(LanguageContext)
}
