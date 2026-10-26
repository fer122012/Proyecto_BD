const RADIO = 54
const CIRCUNFERENCIA = 2 * Math.PI * RADIO

/** Indicador circular dorado para un porcentaje entre 0 y 100. */
export default function AnilloPorcentaje({ porcentaje, leyenda }) {
  const acotado = Math.min(Math.max(porcentaje ?? 0, 0), 100)
  const trazo = (acotado / 100) * CIRCUNFERENCIA

  return (
    <svg width="148" height="148" viewBox="0 0 148 148" role="img" aria-label={`${Math.round(acotado)} % ${leyenda}`}>
      <circle cx="74" cy="74" r={RADIO} fill="none" stroke="var(--superficie-control)" strokeWidth="10" />
      <circle
        cx="74"
        cy="74"
        r={RADIO}
        fill="none"
        stroke="var(--dorado)"
        strokeWidth="10"
        strokeLinecap="round"
        strokeDasharray={`${trazo} ${CIRCUNFERENCIA}`}
        transform="rotate(-90 74 74)"
      />
      <text x="74" y="72" textAnchor="middle" fill="var(--texto)" fontFamily="Outfit, sans-serif" fontSize="30" fontWeight="600">
        {Math.round(acotado)}%
      </text>
      <text x="74" y="94" textAnchor="middle" fill="var(--texto-tenue)" fontFamily="JetBrains Mono, monospace" fontSize="11">
        {leyenda}
      </text>
    </svg>
  )
}
