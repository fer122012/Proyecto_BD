import { useState } from 'react'
import { instancia, mantenimiento } from '../../api/endpoints.js'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { ContenidoConsulta, MensajeError } from '../../components/Estados.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { fechaHora, numero } from '../../utils/formato.js'

/** Una estadística que nunca se calculó no tiene fecha: el optimizador no sabe nada de esos datos. */
const nuncaCalculada = (e) => e.ultimaActualizacion == null

/**
 * Porcentaje de filas modificadas desde la última actualización de la estadística.
 * Si nunca se calculó, se trata como 100 % desactualizada.
 */
const porcentajeModificado = (e) => {
  if (nuncaCalculada(e)) return 100
  return e.filas > 0 ? (e.filasModificadas / e.filas) * 100 : 0
}

/** A partir de este porcentaje se recomienda recalcular la estadística. */
const UMBRAL_DESACTUALIZADA = 20

/** Módulo 6: recalcular estadísticas y recompilar objetos inválidos. */
export default function MantenimientoPage() {
  const bases = useConsulta(instancia.basesDeDatos)
  const basesUsuario = (bases.datos ?? []).filter((b) => !b.esSistema && b.estado === 'ONLINE')
  const [baseElegida, setBaseElegida] = useState('')
  const base = baseElegida || basesUsuario[0]?.nombre || ''

  const estadisticas = useConsulta(base ? () => mantenimiento.estadisticas(base) : null, [base])
  const objetos = useConsulta(base ? () => mantenimiento.objetosInvalidos(base) : null, [base])

  const [accionEnCurso, setAccionEnCurso] = useState(null)
  const [resultado, setResultado] = useState(null)

  async function ejecutar(clave, accion, recargar) {
    setAccionEnCurso(clave)
    setResultado(null)
    try {
      setResultado(await accion())
      recargar()
    } catch (error) {
      setResultado({ exito: false, mensaje: error.message })
    } finally {
      setAccionEnCurso(null)
    }
  }

  const recalcular = (e) =>
    ejecutar(
      `est:${e.esquema}.${e.tabla}`,
      () => mantenimiento.recalcularEstadisticas({ baseDatos: base, esquema: e.esquema, tabla: e.tabla }),
      estadisticas.recargar,
    )

  const recompilar = (o) =>
    ejecutar(
      `obj:${o.esquema}.${o.objeto}`,
      () => mantenimiento.recompilarObjeto({ baseDatos: base, esquema: o.esquema, objeto: o.objeto }),
      objetos.recargar,
    )

  const desactualizadas = (estadisticas.datos ?? []).filter((e) => porcentajeModificado(e) >= UMBRAL_DESACTUALIZADA)

  const columnasEstadisticas = [
    { titulo: 'Tabla', clase: 'mono', valor: (e) => `${e.esquema}.${e.tabla}` },
    { titulo: 'Estadística', clase: 'mono tenue', valor: (e) => e.estadistica },
    {
      titulo: 'Actualizada',
      clase: 'mono',
      valor: (e) =>
        nuncaCalculada(e) ? <span style={{ color: 'var(--error)' }}>Nunca</span> : fechaHora(e.ultimaActualizacion),
    },
    { titulo: 'Filas', clase: 'mono numero', valor: (e) => (nuncaCalculada(e) ? '—' : numero(e.filas)) },
    {
      titulo: 'Modificadas',
      clase: 'mono numero',
      valor: (e) => {
        if (nuncaCalculada(e)) return <span className="tenue">Sin calcular</span>
        const porcentaje = porcentajeModificado(e)
        const color = porcentaje >= UMBRAL_DESACTUALIZADA ? 'var(--error)' : undefined
        return (
          <span style={{ color }}>
            {numero(e.filasModificadas)} · {Math.round(porcentaje)} %
          </span>
        )
      },
    },
    {
      titulo: 'Acción',
      valor: (e) => {
        const clave = `est:${e.esquema}.${e.tabla}`
        const recomendada = porcentajeModificado(e) >= UMBRAL_DESACTUALIZADA
        return (
          <button
            type="button"
            className={`boton ${recomendada ? 'principal' : ''}`}
            disabled={accionEnCurso !== null}
            onClick={() => recalcular(e)}
          >
            {accionEnCurso === clave ? 'Recalculando…' : 'Recalcular'}
          </button>
        )
      },
    },
  ]

  const columnasObjetos = [
    { titulo: 'Objeto', clase: 'mono', valor: (o) => `${o.esquema}.${o.objeto}` },
    { titulo: 'Tipo', valor: (o) => o.tipo },
    {
      titulo: 'Referencia rota',
      clase: 'mono',
      valor: (o) => (
        <span style={{ color: 'var(--error)' }}>
          {o.esquemaReferenciado ? `${o.esquemaReferenciado}.` : ''}
          {o.objetoReferenciado}
        </span>
      ),
    },
    {
      titulo: 'Acción',
      valor: (o) => {
        const clave = `obj:${o.esquema}.${o.objeto}`
        return (
          <button type="button" className="boton principal" disabled={accionEnCurso !== null} onClick={() => recompilar(o)}>
            {accionEnCurso === clave ? 'Recompilando…' : 'Recompilar'}
          </button>
        )
      },
    },
  ]

  return (
    <>
      <Portada
        etiqueta="Mantenimiento preventivo"
        titulo={
          estadisticas.datos
            ? desactualizadas.length === 0
              ? 'Estadísticas al día'
              : `${desactualizadas.length} ${desactualizadas.length === 1 ? 'estadística' : 'estadísticas'} por recalcular`
            : 'Mantenimiento preventivo'
        }
        subtitulo={base ? `en ${base}` : undefined}
      >
        <div className="chips" style={{ alignItems: 'center' }}>
          <label className="campo" htmlFor="base-datos">
            Base de datos
            <select
              id="base-datos"
              value={base}
              onChange={(e) => {
                setBaseElegida(e.target.value)
                setResultado(null)
              }}
              disabled={basesUsuario.length === 0}
            >
              {basesUsuario.length === 0 && <option value="">Sin bases de usuario</option>}
              {basesUsuario.map((b) => (
                <option key={b.nombre} value={b.nombre}>
                  {b.nombre}
                </option>
              ))}
            </select>
          </label>
          {objetos.datos && (
            <span className={`chip ${objetos.datos.length > 0 ? 'error' : 'ok'}`}>
              {objetos.datos.length === 0 ? 'Sin objetos inválidos' : `${objetos.datos.length} ${objetos.datos.length === 1 ? 'objeto' : 'objetos'} por recompilar`}
            </span>
          )}
        </div>
      </Portada>

      <div className="contenido">
        {bases.error && <MensajeError mensaje={bases.error} onReintentar={bases.recargar} />}
        {bases.datos && basesUsuario.length === 0 && (
          <div className="mensaje error">La instancia no tiene bases de datos de usuario en línea sobre las cuales trabajar.</div>
        )}

        {resultado && (
          <div className={`mensaje ${resultado.exito ? 'exito' : 'error'}`} role="status">
            <span>
              <strong>{resultado.exito ? 'Operación completada.' : 'La operación no se completó.'}</strong> {resultado.mensaje}
              {resultado.objeto && <span className="mono"> · {resultado.objeto}</span>}
              {resultado.fecha && <span className="mono"> · {fechaHora(resultado.fecha)}</span>}
            </span>
            <button type="button" className="boton" onClick={() => setResultado(null)}>
              Cerrar
            </button>
          </div>
        )}

        {base && (
          <>
            <section className="bloque">
              <div className="bloque-encabezado">
                <h2 className="etiqueta">Estadísticas de tablas</h2>
                <BotonActualizar
                  cargando={estadisticas.cargando || objetos.cargando}
                  etiqueta="Actualizar mantenimiento"
                  onClick={() => {
                    estadisticas.recargar()
                    objetos.recargar()
                  }}
                />
              </div>
              <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
                Se recomienda recalcular cuando se modificó al menos el {UMBRAL_DESACTUALIZADA} % de las filas desde la última
                actualización.
              </p>
              <ContenidoConsulta consulta={estadisticas} vacio="La base no tiene estadísticas en tablas de usuario.">
                {(datos) => (
                  <Tabla
                    columnas={columnasEstadisticas}
                    filas={datos}
                    clave={(e) => `${e.esquema}.${e.tabla}.${e.estadistica}`}
                  />
                )}
              </ContenidoConsulta>
            </section>

            <section className="bloque">
              <h2 className="etiqueta">Objetos inválidos o con errores</h2>
              <ContenidoConsulta consulta={objetos} vacio="No hay objetos con dependencias rotas en esta base.">
                {(datos) => (
                  <Tabla
                    columnas={columnasObjetos}
                    filas={datos}
                    clave={(o) => `${o.esquema}.${o.objeto}-${o.esquemaReferenciado}.${o.objetoReferenciado}`}
                  />
                )}
              </ContenidoConsulta>
            </section>
          </>
        )}
      </div>
    </>
  )
}
