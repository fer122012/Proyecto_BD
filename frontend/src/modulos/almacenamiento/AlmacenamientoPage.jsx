import { useState } from 'react'
import { almacenamiento } from '../../api/endpoints.js'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { ContenidoConsulta, MensajeError } from '../../components/Estados.jsx'
import Medidor from '../../components/Medidor.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { fechaHora, numero, tamano } from '../../utils/formato.js'
import GraficoCrecimiento from './GraficoCrecimiento.jsx'

/** Color de cada estado de filegroup que calcula el backend. */
const TONO_ESTADO = { NORMAL: 'ok', ATENCION: 'aviso', CRITICO: 'error' }
const TEXTO_ESTADO = { NORMAL: 'Normal', ATENCION: 'Atención', CRITICO: 'Crítico' }
const TONO_MEDIDOR = { ATENCION: 'aviso', CRITICO: 'error' }

/** El estado más grave entre los filegroups, para el resumen de la portada. */
function estadoMasGrave(filegroups) {
  if (filegroups.some((f) => f.estado === 'CRITICO')) return 'CRITICO'
  if (filegroups.some((f) => f.estado === 'ATENCION')) return 'ATENCION'
  return 'NORMAL'
}

const columnasBases = (maximo, base) => [
  {
    titulo: 'Base de datos',
    clase: 'mono',
    valor: (b) => (
      <span className={b.esSistema ? 'tenue' : undefined} style={b.nombre === base ? { color: 'var(--dorado-claro)' } : undefined}>
        {b.nombre}
      </span>
    ),
  },
  { titulo: 'Datos', clase: 'mono numero', valor: (b) => tamano(b.tamanoDatosMb) },
  { titulo: 'Log', clase: 'mono numero', valor: (b) => tamano(b.tamanoLogMb) },
  { titulo: 'Total', clase: 'mono numero', valor: (b) => tamano(b.tamanoTotalMb) },
  {
    titulo: 'Proporción',
    valor: (b) => <Medidor porcentaje={maximo > 0 ? (b.tamanoTotalMb / maximo) * 100 : 0} etiqueta={`Tamaño de ${b.nombre}`} />,
  },
  { titulo: 'Archivos', clase: 'mono numero', valor: (b) => b.cantidadArchivos },
]

const COLUMNAS_ARCHIVOS = [
  { titulo: 'Archivo', clase: 'mono', valor: (a) => a.nombreLogico },
  { titulo: 'Tipo', valor: (a) => (a.tipo === 'LOG' ? 'Log' : 'Datos') },
  { titulo: 'Filegroup', clase: 'mono tenue', valor: (a) => a.filegroup ?? '—' },
  { titulo: 'Tamaño', clase: 'mono numero', valor: (a) => tamano(a.tamanoMb) },
  { titulo: 'Máximo', clase: 'mono numero', valor: (a) => a.tamanoMaximoTexto },
  { titulo: 'Crecimiento', valor: (a) => a.crecimientoTexto },
  {
    titulo: 'Estado',
    valor: (a) => <span className={`chip ${a.estado === 'ONLINE' ? 'ok' : 'error'}`}>{a.estado}</span>,
  },
  { titulo: 'Ruta física', clase: 'mono tenue', valor: (a) => a.rutaFisica },
]

const columnasObjetos = (maximo) => [
  { titulo: 'Objeto', clase: 'mono', valor: (o) => `${o.esquema}.${o.objeto}` },
  { titulo: 'Índice', clase: 'mono tenue', valor: (o) => o.indice },
  { titulo: 'Tipo', valor: (o) => o.tipoIndice },
  { titulo: 'Filas', clase: 'mono numero', valor: (o) => numero(o.filas) },
  { titulo: 'Reservado', clase: 'mono numero', valor: (o) => tamano(o.reservadoMb) },
  { titulo: 'Usado', clase: 'mono numero', valor: (o) => tamano(o.usadoMb) },
  {
    titulo: 'Proporción',
    valor: (o) => <Medidor porcentaje={maximo > 0 ? (o.reservadoMb / maximo) * 100 : 0} etiqueta={`Espacio de ${o.objeto}`} />,
  },
]

const COLUMNAS_CRECIMIENTO = [
  { titulo: 'Respaldo completo', clase: 'mono', valor: (p) => fechaHora(p.fecha) },
  { titulo: 'Datos asignados', clase: 'mono numero', valor: (p) => tamano(p.tamanoDatosMb) },
  { titulo: 'Log asignado', clase: 'mono numero', valor: (p) => tamano(p.tamanoLogMb) },
  { titulo: 'Usado', clase: 'mono numero', valor: (p) => tamano(p.usadoMb) },
  {
    titulo: 'Variación',
    clase: 'mono numero',
    valor: (p) => {
      if (p.variacionUsadoMb == null) return '—'
      const signo = p.variacionUsadoMb > 0 ? '+' : p.variacionUsadoMb < 0 ? '−' : ''
      return `${signo}${tamano(Math.abs(p.variacionUsadoMb))}`
    },
  },
]

/** Módulo 3: espacio por filegroup, tamaño de la base, archivos, objetos y crecimiento. */
export default function AlmacenamientoPage() {
  const bases = useConsulta(almacenamiento.basesDeDatos)
  const basesUsuario = (bases.datos ?? []).filter((b) => !b.esSistema && b.estado === 'ONLINE')
  const [baseElegida, setBaseElegida] = useState('')
  const base = baseElegida || basesUsuario[0]?.nombre || ''

  const filegroups = useConsulta(base ? () => almacenamiento.filegroups(base) : null, [base])
  const archivos = useConsulta(base ? () => almacenamiento.archivos(base) : null, [base])
  const objetos = useConsulta(base ? () => almacenamiento.objetos(base) : null, [base])
  const crecimiento = useConsulta(base ? () => almacenamiento.crecimiento(base) : null, [base])

  const datosBase = bases.datos?.find((b) => b.nombre === base)
  const maximoBases = Math.max(0, ...(bases.datos ?? []).map((b) => b.tamanoTotalMb))
  const maximoObjetos = Math.max(0, ...(objetos.datos ?? []).map((o) => o.reservadoMb))
  const estadoGeneral = filegroups.datos?.length ? estadoMasGrave(filegroups.datos) : null

  const recargarTodo = () => {
    bases.recargar()
    filegroups.recargar()
    archivos.recargar()
    objetos.recargar()
    crecimiento.recargar()
  }

  return (
    <>
      <Portada
        etiqueta="Gestión del almacenamiento"
        titulo={datosBase ? tamano(datosBase.tamanoTotalMb) : bases.error ? 'Sin datos de almacenamiento' : 'Gestión del almacenamiento'}
        subtitulo={datosBase ? `ocupa ${base} en disco` : undefined}
      >
        <div className="chips" style={{ alignItems: 'center' }}>
          <label className="campo" htmlFor="base-datos">
            Base de datos
            <select
              id="base-datos"
              value={base}
              onChange={(e) => setBaseElegida(e.target.value)}
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
          {datosBase && (
            <span className="chip mono">
              Datos {tamano(datosBase.tamanoDatosMb)} · Log {tamano(datosBase.tamanoLogMb)}
            </span>
          )}
          {estadoGeneral && (
            <span className={`chip ${TONO_ESTADO[estadoGeneral]}`}>
              {estadoGeneral === 'NORMAL' ? 'Espacio suficiente' : `Filegroups en estado ${TEXTO_ESTADO[estadoGeneral].toLowerCase()}`}
            </span>
          )}
        </div>
      </Portada>

      <div className="contenido">
        {bases.error && <MensajeError mensaje={bases.error} onReintentar={bases.recargar} />}
        {bases.datos && basesUsuario.length === 0 && (
          <div className="mensaje error">La instancia no tiene bases de datos de usuario en línea para analizar.</div>
        )}

        {base && (
          <section className="bloque">
            <div className="bloque-encabezado">
              <h2 className="etiqueta">Espacio por filegroup</h2>
              <BotonActualizar
                cargando={bases.cargando || filegroups.cargando || archivos.cargando || objetos.cargando || crecimiento.cargando}
                etiqueta="Actualizar almacenamiento"
                onClick={recargarTodo}
              />
            </div>
            <ContenidoConsulta consulta={filegroups} vacio="La base no tiene filegroups de datos.">
              {(datos) => (
                <div className="filegroups">
                  {datos.map((f) => (
                    <div key={f.filegroup} className="filegroup">
                      <div className="filegroup-encabezado">
                        <span>
                          <span className="mono">{f.filegroup}</span>
                          {f.esPredeterminado && <span className="tenue"> · predeterminado</span>}
                          {f.soloLectura && <span className="tenue"> · solo lectura</span>}
                        </span>
                        <span className={`chip ${TONO_ESTADO[f.estado]}`}>{TEXTO_ESTADO[f.estado]}</span>
                      </div>
                      <Medidor porcentaje={f.porcentajeUso} tono={TONO_MEDIDOR[f.estado]} etiqueta={`Uso de ${f.filegroup}`} />
                      <span className="mono" style={{ fontSize: 13 }}>
                        {tamano(f.usadoMb)} usados · {tamano(f.disponibleMb)} disponibles · {tamano(f.tamanoMb)} asignados ·{' '}
                        {numero(f.porcentajeUso)} %
                      </span>
                      <span className="tenue" style={{ fontSize: 13 }}>
                        {f.archivos} {f.archivos === 1 ? 'archivo' : 'archivos'}, {f.archivosConCrecimiento} con crecimiento
                        automático. {f.interpretacion}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </ContenidoConsulta>
          </section>
        )}

        {base && (
          <section className="bloque">
            <h2 className="etiqueta">Crecimiento de {base}</h2>
            <ContenidoConsulta consulta={crecimiento}>
              {(datos) => (
                <>
                  {datos.puntos.length > 0 && (
                    <>
                      <div className="leyenda">
                        <span>
                          <i /> Espacio usado
                        </span>
                        <span>
                          <i className="asignado" /> Datos asignados
                        </span>
                      </div>
                      <GraficoCrecimiento puntos={datos.puntos} />
                    </>
                  )}
                  <p className={datos.puntos.length < 2 ? 'nota' : 'tenue'} style={{ margin: 0, fontSize: 13 }}>
                    {datos.interpretacion}
                  </p>
                  {datos.puntos.length > 0 && (
                    <Tabla columnas={COLUMNAS_CRECIMIENTO} filas={[...datos.puntos].reverse()} clave={(p) => p.fecha} />
                  )}
                </>
              )}
            </ContenidoConsulta>
          </section>
        )}

        {base && (
          <section className="bloque">
            <h2 className="etiqueta">Archivos de datos y log</h2>
            <ContenidoConsulta consulta={archivos} vacio="No se encontraron archivos para esta base.">
              {(datos) => <Tabla columnas={COLUMNAS_ARCHIVOS} filas={datos} clave={(a) => a.nombreLogico} />}
            </ContenidoConsulta>
          </section>
        )}

        {base && (
          <section className="bloque">
            <h2 className="etiqueta">Objetos con mayor consumo</h2>
            <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
              Las 20 tablas e índices que más espacio reservan. Reservado es lo que ocupa en el archivo; usado, lo que tiene datos.
            </p>
            <ContenidoConsulta consulta={objetos} vacio="La base no tiene tablas ni índices de usuario con datos.">
              {(datos) => (
                <Tabla columnas={columnasObjetos(maximoObjetos)} filas={datos} clave={(o) => `${o.esquema}.${o.objeto}.${o.indice}`} />
              )}
            </ContenidoConsulta>
          </section>
        )}

        <section className="bloque">
          <h2 className="etiqueta">Tamaño de las bases de datos{bases.datos ? ` · ${bases.datos.length}` : ''}</h2>
          <ContenidoConsulta consulta={bases} vacio="La instancia no tiene bases de datos.">
            {(datos) => <Tabla columnas={columnasBases(maximoBases, base)} filas={datos} clave={(b) => b.nombre} />}
          </ContenidoConsulta>
        </section>
      </div>
    </>
  )
}
