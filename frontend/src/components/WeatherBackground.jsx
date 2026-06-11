import { useMemo } from 'react'
import './weather.css'

// Pick a season from the current month (Pakistan-ish): Apr-Jun hot summer,
// Jul-Sep monsoon, Dec-Feb winter, else warm/sunny.
function getSeason() {
  const m = new Date().getMonth() + 1
  if (m >= 4 && m <= 6) return 'summer'
  if (m >= 7 && m <= 9) return 'monsoon'
  if (m === 12 || m <= 2) return 'winter'
  return 'summer'
}

// eslint-disable-next-line react-refresh/only-export-components
export function seasonLabel() {
  const s = getSeason()
  return s === 'summer' ? '☀️ Hot & Sunny' : s === 'monsoon' ? '🌧️ Monsoon' : '❄️ Winter'
}

const rand = (a, b) => a + Math.random() * (b - a)

export default function WeatherBackground() {
  const season = getSeason()

  const items = useMemo(() => {
    if (season === 'summer') return Array.from({ length: 14 }, () => ({ left: rand(0, 100), size: rand(5, 14), dur: rand(6, 14), delay: rand(0, 8) }))
    if (season === 'winter') return Array.from({ length: 40 }, () => ({ left: rand(0, 100), size: rand(3, 8), dur: rand(5, 12), delay: rand(0, 8) }))
    if (season === 'monsoon') return Array.from({ length: 60 }, () => ({ left: rand(0, 100), size: 0, dur: rand(0.5, 1.2), delay: rand(0, 2) }))
    return []
  }, [season])

  return (
    <>
      <div className={`weather weather-${season}`} aria-hidden="true">
        {season === 'summer' && (
          <>
            <div className="sun-rays" />
            <div className="sun" />
            <div className="heat" />
            {items.map((p, i) => (
              <span key={i} className="particle" style={{ left: `${p.left}%`, width: p.size, height: p.size, animationDuration: `${p.dur}s`, animationDelay: `${p.delay}s` }} />
            ))}
          </>
        )}
        {season === 'winter' && items.map((p, i) => (
          <span key={i} className="snow" style={{ left: `${p.left}%`, width: p.size, height: p.size, animationDuration: `${p.dur}s`, animationDelay: `${p.delay}s` }} />
        ))}
        {season === 'monsoon' && items.map((p, i) => (
          <span key={i} className="rain" style={{ left: `${p.left}%`, animationDuration: `${p.dur}s`, animationDelay: `${p.delay}s` }} />
        ))}
      </div>

    </>
  )
}
