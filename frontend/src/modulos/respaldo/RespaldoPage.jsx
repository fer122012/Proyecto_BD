import { useState } from 'react'
import { respaldo } from '../../api/endpoints.js'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { ContenidoConsulta, MensajeError } from '../../components/Estados.jsx'
import Medidor from '../../components/Medidor.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { duracion, fechaHora, numero, tamano } from '../../utils/formato.js'

/** Color y texto de cada nivel de riesgo que calcula el backend. */
const TONO_RIESGO = { ADECUADO: 'ok', ATENCION: 'aviso', CRITICO: 'error' }
const TEXTO_RIESGO = { ADECUADO: 'Adecuado', ATENCION: 'Atención', CRITICO: 'Crítico' }

const TONO_ESTADO = { EXITOSO: 'ok', DANADO: 'error', INCOMPLETO: 'aviso' }
const TEXTO_ESTADO = { EXITOSO: 'Exitoso', DANADO: 'Dañado', INCOMPLETO: 'Incompleto' }

const columnasEstado = (base) => [
  {
    titulo: 'Base de datos',
    clase: 'mono',
    valor: (b) => (
      <span className={b.esSistema ? 'tenue' : undefined} style={b.baseDatos === base ? { color: 'var(--dorado-claro)' } : undefined}>
        {b.baseDatos}
      </span>
    ),
  },
  { titulo: 'Recuperación', valor: (b) => b.modeloRecuperacion },
  { titulo: 'Último completo', clase: 'mono', valor: (b) => fechaHora(b.ultimoCompleto) },
  { titulo: 'Último diferencial', clase: 'mono tenue', valor: (b) => fechaHora(b.ultimoDiferencial) },
  { titulo: 'Último de log', clase: 'mono tenue', valor: (b) => fechaHora(b.ultimoLog) },
  { titulo: 'Respaldos', clase: 'mono numero', valor: (b) => numero(b.totalRespaldos) },
  {
    titulo: 'Riesgo',
    valor: (b) => <span className={`chip ${TONO_RIESGO[b.nivelRiesgo]}`}>{TEXTO_RIESGO[b.nivelRiesgo]}</span>,
  },
  { titulo: 'Interpretación', clase: 'tenue', valor: (b) => <span style={{ whiteSpace: 'normal', fontSize: 13 }}>{b.interpretacion}</span> },
]

const COLUMNAS_HISTORIAL = [
  { titulo: 'Tipo', valor: (r) => (r.esCopia ? `${r.tipoDescripcion} (solo copia)` : r.tipoDescripcion) },
  { titulo: 'Inicio', clase: 'mono', valor: (r) => fechaHora(r.inicio) },
  { titulo: 'Fin', clase: 'mono', valor: (r) => fechaHora(r.fin) },
  { titulo: 'Duración', clase: 'mono numero', valor: (r) => duracion(r.duracionSegundos) },
  { titulo: 'Tamaño', clase: 'mono numero', valor: (r) => tamano(r.tamanoMb) },
  {
    titulo: 'Estado',
    valor: (r) => (
      <span className={`chip ${TONO_ESTADO[r.estado]}`} title={r.detalleEstado}>
        {TEXTO_ESTADO[r.estado]}
      </span>
    ),
  },
  {
    titulo: 'Checksum',
    valor: (r) => (r.conChecksum ? 'Sí' : <span className="tenue">No</span>),
  },
  { titulo: 'Usuario', clase: 'mono tenue', valor: (r) => r.usuario },
  { titulo: 'Destino', clase: 'mono tenue', valor: (r) => r.destino ?? '—' },
]

/** Módulo 4: último respaldo de cada base con su riesgo, historial y respaldos en curso. */
export default function RespaldoPage() {
  const estado = useConsulta(respaldo.estadoPorBase)
  const enCurso = useConsulta(respaldo.enCurso)

  const lista = estado.datos ?? []
  const basesUsuario = lista.filter((b) => !b.esSistema && b.estado === 'ONLINE')
  const [baseElegida, setBaseElegida] = useState('')
  const base = baseElegida || basesUsuario[0]?.baseDatos || lista[0]?.baseDatos || ''
  const historial = useConsulta(base ? () => respaldo.historial(base) : null, [base])

  const enRiesgo = lista.filter((b) => b.nivelRiesgo !== 'ADECUADO').length
  const criticas = lista.filter((b) => b.nivelRiesgo === 'CRITICO').length
  const sinRespaldo = lista.filter((b) => b.totalRespaldos === 0).length

  const recargarTodo = () => {
    estado.recargar()
    enCurso.recargar()
    historial.recargar()
  }

  let titulo = 'Consultando los respaldos…'
  let subtitulo
  if (estado.error) titulo = 'Sin datos de respaldos'
  else if (estado.datos) {
    titulo = enRiesgo === 0 ? 'Todas las bases respaldadas' : `${enRiesgo} de ${lista.length} bases en riesgo`
    subtitulo = enRiesgo === 0 ? 'con respaldos al día' : 'de perder datos ante una falla'
  }

  return (
    <>
      <Portada etiqueta="Respaldo y recuperación" titulo={titulo} subtitulo={subtitulo}>
        {estado.datos && (
          <div className="chips">
            <span className="chip ok">{lista.length - enRiesgo} adecuadas</span>
            <span className="chip aviso">{enRiesgo - criticas} en atención</span>
            <span className="chip error">{criticas} críticas</span>
            {sinRespaldo > 0 && <span className="chip">{sinRespaldo} nunca respaldadas</span>}
            {enCurso.datos?.length > 0 && (
              <span className="chip aviso">
                {enCurso.datos.length} {enCurso.datos.length === 1 ? 'operación en curso' : 'operaciones en curso'}
              </span>
            )}
          </div>
        )}
      </Portada>

      <div className="contenido">
        {enCurso.error && <MensajeError mensaje={enCurso.error} onReintentar={enCurso.recargar} />}
        {enCurso.datos?.length > 0 && (
          <section className="bloque">
            <h2 className="etiqueta">Respaldos y restauraciones en curso</h2>
            <div className="filegroups">
              {enCurso.datos.map((r) => (
                <div key={r.idSesion} className="filegroup">
                  <div className="filegroup-encabezado">
                    <span>
                      <span className="mono">{r.comando}</span>
                      <span className="tenue">
                        {' '}
                        · {r.baseDatos} · sesión {r.idSesion}
                      </span>
                    </span>
                    <span className="chip aviso">{numero(r.porcentajeAvance)} %</span>
                  </div>
                  <Medidor porcentaje={r.porcentajeAvance} etiqueta={`Avance de ${r.comando} en ${r.baseDatos}`} />
                  <span className="tenue mono" style={{ fontSize: 13 }}>
                    Inició {fechaHora(r.inicio)} · faltan unos {duracion(r.segundosRestantes)}
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}

        <section className="bloque">
          <div className="bloque-encabezado">
            <h2 className="etiqueta">Estado de los respaldos por base{estado.datos ? ` · ${lista.length}` : ''}</h2>
            <BotonActualizar
              cargando={estado.cargando || enCurso.cargando || historial.cargando}
              etiqueta="Actualizar respaldos"
              onClick={recargarTodo}
            />
          </div>
          <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
            Último respaldo de cada tipo que terminó correctamente (sin contar los de solo copia). Crítico: nunca tuvo un
            respaldo completo. Atención: el completo tiene más de 7 días, o la base está en modelo FULL y no tiene
            respaldos de log de las últimas 24 horas.
          </p>
          <ContenidoConsulta consulta={estado} vacio="La instancia no tiene bases de datos.">
            {(datos) => <Tabla columnas={columnasEstado(base)} filas={datos} clave={(b) => b.baseDatos} />}
          </ContenidoConsulta>
        </section>

        {estado.datos && lista.length > 0 && (
          <section className="bloque">
            <div className="bloque-encabezado">
              <h2 className="etiqueta">Historial de respaldos</h2>
              <label className="campo" htmlFor="base-historial">
                Base de datos
                <select id="base-historial" value={base} onChange={(e) => setBaseElegida(e.target.value)}>
                  {lista.map((b) => (
                    <option key={b.baseDatos} value={b.baseDatos}>
                      {b.baseDatos}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
              Los últimos 50 respaldos de {base}. Solo aparecen los que terminaron: un respaldo que falla queda únicamente
              en el log de errores de SQL Server. Pasar el cursor sobre el estado muestra su explicación.
            </p>
            <ContenidoConsulta
              consulta={historial}
              vacio={`${base} no tiene respaldos registrados en msdb. Si se pierde o se daña, no hay desde dónde restaurarla.`}
            >
              {(datos) => <Tabla columnas={COLUMNAS_HISTORIAL} filas={datos} clave={(r) => r.idRespaldo} />}
            </ContenidoConsulta>
          </section>
        )}
      </div>
    </>
  )
}
