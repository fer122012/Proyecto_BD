/**
 * Tabla genérica. Cada columna define `titulo`, `valor(fila)` y opcionalmente
 * `clase` (por ejemplo "mono" o "numero") para alinear y dar formato.
 */
export default function Tabla({ columnas, filas, clave }) {
  return (
    <div className="tabla-contenedor">
      <table className="tabla">
        <thead>
          <tr>
            {columnas.map((columna) => (
              <th key={columna.titulo} className={columna.clase?.includes('numero') ? 'numero' : undefined} scope="col">
                {columna.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => (
            <tr key={clave ? clave(fila) : indice}>
              {columnas.map((columna) => (
                <td key={columna.titulo} className={columna.clase}>
                  {columna.valor(fila)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
