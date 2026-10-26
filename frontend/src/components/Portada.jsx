/**
 * Encabezado de cada módulo: la celosía dorada a la izquierda y el titular a la derecha.
 * `titulo` es la idea principal; `subtitulo` aparece en gris debajo.
 */
export default function Portada({ etiqueta, titulo, subtitulo, children }) {
  return (
    <section className="portada">
      <div className="celosia" aria-hidden="true">
        <span className="franja" style={{ left: -160, top: -200 }} />
        <span className="franja" style={{ left: 40, top: -260, background: '#1e2227' }} />
        <span className="oro" style={{ left: 60, top: -220 }} />
        <span className="oro inversa" style={{ left: 150, top: -160 }} />
        <span className="oro inversa" style={{ left: 250, top: -240, height: 700 }} />
      </div>
      <div className="portada-contenido">
        <span className="etiqueta" style={{ color: 'var(--dorado)' }}>
          {etiqueta}
        </span>
        <h1>
          {titulo}
          {subtitulo && <span className="tenue">{subtitulo}</span>}
        </h1>
        {children}
      </div>
    </section>
  )
}
