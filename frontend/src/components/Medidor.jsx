/**
 * Barra horizontal para un porcentaje entre 0 y 100.
 * `tono` puede ser "aviso" o "error" para resaltar valores que requieren atención.
 */
export default function Medidor({ porcentaje, tono, etiqueta }) {
  const acotado = Math.min(Math.max(porcentaje ?? 0, 0), 100)
  return (
    <div
      className={`medidor ${tono ?? ''}`}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(acotado)}
      aria-label={etiqueta}
    >
      <span style={{ width: `${acotado}%` }} />
    </div>
  )
}
