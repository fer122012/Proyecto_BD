import Portada from './Portada.jsx'

/** Pantalla provisional para los módulos que todavía no tienen endpoints en el backend. */
export default function ModuloPendiente({ numero, nombre }) {
  return (
    <Portada etiqueta={`Módulo ${numero}`} titulo={nombre} subtitulo="en desarrollo">
      <p className="tenue" style={{ margin: 0, maxWidth: '52ch' }}>
        Este módulo todavía no tiene endpoints en el backend. La pantalla se habilitará cuando la API esté
        disponible.
      </p>
    </Portada>
  )
}
