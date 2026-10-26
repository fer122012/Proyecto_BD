/** Botón de ícono para volver a consultar la API. Gira mientras la consulta está en curso. */
export default function BotonActualizar({ onClick, cargando, etiqueta = 'Actualizar' }) {
  return (
    <button type="button" className="boton icono" onClick={onClick} disabled={cargando} aria-label={etiqueta} title={etiqueta}>
      <svg
        className={cargando ? 'girando' : undefined}
        width="18"
        height="18"
        viewBox="0 0 24 24"
        aria-hidden="true"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M20 11a8 8 0 1 0-2.3 5.7" />
        <path d="M20 4v7h-7" />
      </svg>
    </button>
  )
}
