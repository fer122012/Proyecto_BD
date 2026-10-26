/** Componentes para los estados de una consulta: cargando, error y sin resultados. */

export function Cargando({ filas = 4 }) {
  return (
    <div role="status" aria-label="Cargando" style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%' }}>
      {Array.from({ length: filas }, (_, i) => (
        <div key={i} className="esqueleto" style={{ width: `${90 - i * 12}%` }} />
      ))}
    </div>
  )
}

export function MensajeError({ mensaje, onReintentar }) {
  return (
    <div className="mensaje error" role="alert">
      <span>
        <strong>No se pudo cargar la información.</strong> {mensaje}
      </span>
      {onReintentar && (
        <button type="button" className="boton" onClick={onReintentar}>
          Reintentar
        </button>
      )}
    </div>
  )
}

export function SinDatos({ children }) {
  return <p className="vacio">{children}</p>
}

/** Envuelve el contenido de un bloque y muestra el estado que corresponda. */
export function ContenidoConsulta({ consulta, vacio, filas, children }) {
  const { datos, cargando, error, recargar } = consulta
  if (error) return <MensajeError mensaje={error} onReintentar={recargar} />
  if (cargando && !datos) return <Cargando filas={filas} />
  if (Array.isArray(datos) && datos.length === 0) return <SinDatos>{vacio}</SinDatos>
  if (!datos) return null
  return children(datos)
}
