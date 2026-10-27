/**
 * Texto de una sentencia SQL en una celda de tabla: se muestra en una sola línea recortada y
 * el texto completo aparece al pasar el cursor, para que las consultas largas no deformen la tabla.
 */
export default function TextoSql({ texto }) {
  if (!texto) return <span className="tenue">—</span>
  const compacto = texto.replace(/\s+/g, ' ').trim()
  return (
    <span className="mono sql-corto" title={compacto}>
      {compacto}
    </span>
  )
}
