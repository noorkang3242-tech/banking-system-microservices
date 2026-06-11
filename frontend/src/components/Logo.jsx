// NOOR Bank logo. "Noor" means light, so the mark is a radiant sun.
// variant: full (sun + wordmark) or mark-only; light=true for dark backgrounds.
export default function Logo({ height = 38, light = false, markOnly = false }) {
  const word1 = light ? '#ffffff' : '#2a1606'
  const word2 = light ? '#ffd9a8' : '#fb8c00'
  const vb = markOnly ? '0 0 56 56' : '0 0 220 56'
  const rays = Array.from({ length: 12 }).map((_, i) => {
    const a = (i * 30) * Math.PI / 180
    const r1 = 17, r2 = 24
    return (
      <line key={i}
        x1={28 + Math.cos(a) * r1} y1={28 + Math.sin(a) * r1}
        x2={28 + Math.cos(a) * r2} y2={28 + Math.sin(a) * r2}
        stroke="url(#ray)" strokeWidth="2.6" strokeLinecap="round" />
    )
  })
  return (
    <svg height={height} viewBox={vb} xmlns="http://www.w3.org/2000/svg" role="img" aria-label="NOOR Bank">
      <defs>
        <radialGradient id="sun" cx="50%" cy="45%" r="60%">
          <stop offset="0%" stopColor="#fff3c4" />
          <stop offset="55%" stopColor="#ffb300" />
          <stop offset="100%" stopColor="#fb6f00" />
        </radialGradient>
        <linearGradient id="ray" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#ffca28" />
          <stop offset="100%" stopColor="#ff8f00" />
        </linearGradient>
      </defs>
      {/* sun rays */}
      <g opacity="0.95">{rays}</g>
      {/* sun disc */}
      <circle cx="28" cy="28" r="15" fill="url(#sun)" stroke="#ff8f00" strokeWidth="1" />
      {/* N monogram */}
      <text x="28" y="35" textAnchor="middle" fontFamily="Segoe UI, Arial, sans-serif" fontWeight="800" fontSize="19" fill="#fff">N</text>
      {!markOnly && (
        <g fontFamily="Segoe UI, Arial, sans-serif">
          <text x="56" y="27" fontWeight="800" fontSize="22" letterSpacing="1.5" fill={word1}>NOOR</text>
          <text x="57" y="46" fontWeight="600" fontSize="15" letterSpacing="5" fill={word2}>BANK</text>
        </g>
      )}
    </svg>
  )
}
