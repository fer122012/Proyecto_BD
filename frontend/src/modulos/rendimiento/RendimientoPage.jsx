import { useEffect, useState } from 'react'
import { rendimiento } from '../../api/endpoints.js'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { ContenidoConsulta } from '../../components/Estados.jsx'
import Medidor from '../../components/Medidor.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { fechaHora, memoriaKb, milisegundos, numero } from '../../utils/formato.js'
import TextoSql from './TextoSql.jsx'

/** Cada cuánto se vuelve a consultar cuando la actualización automática está activa. */
const INTERVALO_MS = 15000

/** Color de cada estado de bloqueo que calcula el backend. */
const TONO_ESTADO = { ATENCION: 'aviso', CRITICO: 'error' }
const TEXTO_ESTADO = { ATENCION: 'Atención', CRITICO: 'Crítico' }

const COLUMNAS_BLOQUEOS = [
  { titulo: 'Sesión', clase: 'mono numero', valor: (b) => b.idSesionBloqueada },
  { titulo: 'Bloqueada por', clase: 'mono numero', valor: (b) => b.idSesionBloqueadora },
  {
    titulo: 'Estado',
    valor: (b) => <span className={`chip ${TONO_ESTADO[b.estado]}`}>{TEXTO_ESTADO[b.estado]}</span>,
  },
  { titulo: 'Espera', clase: 'mono numero', valor: (b) => milisegundos(b.tiempoEsperaMs) },
  { titulo: 'Tipo de espera', clase: 'mono tenue', valor: (b) => b.tipoEspera },
  { titulo: 'Base', clase: 'mono', valor: (b) => b.baseDatos ?? '—' },
  { titulo: 'Bloqueador', valor: (b) => `${b.usuarioBloqueador ?? '—'} · ${b.programaBloqueador ?? '—'}` },
  { titulo: 'Consulta bloqueada', valor: (b) => <TextoSql texto={b.consultaBloqueada} /> },
  { titulo: 'Consulta bloqueadora', valor: (b) => <TextoSql texto={b.consultaBloqueadora} /> },
]

const columnasProcesos = (porMemoria, maximo) => [
  { titulo: 'Proceso', clase: 'mono numero', valor: (p) => p.idProceso },
  { titulo: 'Tipo', valor: (p) => <span className={p.tipoProceso === 'Sistema' ? 'tenue' : undefined}>{p.tipoProceso}</span> },
  { titulo: 'Usuario', clase: 'mono', valor: (p) => p.usuario ?? '—' },
  { titulo: 'Programa', clase: 'tenue', valor: (p) => p.programa || '—' },
  { titulo: 'Estado', valor: (p) => p.estado },
  { titulo: 'CPU', clase: 'mono numero', valor: (p) => milisegundos(p.cpuMs) },
  { titulo: '% CPU', clase: 'mono numero', valor: (p) => (p.porcentajeCpu == null ? '—' : `${numero(p.porcentajeCpu)} %`) },
  { titulo: 'Memoria sesión', clase: 'mono numero', valor: (p) => memoriaKb(p.memoriaSesionKb) },
  { titulo: 'Memoria concedida', clase: 'mono numero', valor: (p) => memoriaKb(p.memoriaConcedidaKb) },
  {
    titulo: 'Proporción',
    valor: (p) => (
      <Medidor
        porcentaje={maximo > 0 ? (valorProceso(p, porMemoria) / maximo) * 100 : 0}
        etiqueta={`${porMemoria ? 'Memoria' : 'CPU'} del proceso ${p.idProceso}`}
      />
    ),
  },
  { titulo: 'Consulta actual', valor: (p) => <TextoSql texto={p.consultaActual} /> },
]

/** Valor con el que se ordena y se dibuja la barra: CPU o memoria total (sesión + concedida). */
const valorProceso = (p, porMemoria) => (porMemoria ? p.memoriaSesionKb + (p.memoriaConcedidaKb ?? 0) : p.cpuMs)

const columnasConsumo = (maximo) => [
  { titulo: 'Consulta', valor: (c) => <TextoSql texto={c.consulta} /> },
  { titulo: 'Base', clase: 'mono', valor: (c) => c.baseDatos ?? '—' },
  { titulo: 'Ejecuciones', clase: 'mono numero', valor: (c) => numero(c.ejecuciones) },
  { titulo: 'CPU total', clase: 'mono numero', valor: (c) => milisegundos(c.cpuTotalMs) },
  {
    titulo: 'Proporción',
    valor: (c) => <Medidor porcentaje={maximo > 0 ? (c.cpuTotalMs / maximo) * 100 : 0} etiqueta="CPU de la consulta" />,
  },
  { titulo: 'Duración total', clase: 'mono numero', valor: (c) => milisegundos(c.duracionTotalMs) },
  { titulo: 'Lecturas lógicas', clase: 'mono numero', valor: (c) => numero(c.lecturasLogicasTotal) },
  { titulo: 'Escrituras lógicas', clase: 'mono numero', valor: (c) => numero(c.escriturasLogicasTotal) },
  { titulo: 'Última ejecución', clase: 'mono', valor: (c) => fechaHora(c.ultimaEjecucion) },
]

const columnasPromedio = (maximo) => [
  { titulo: 'Consulta', valor: (c) => <TextoSql texto={c.consulta} /> },
  { titulo: 'Base', clase: 'mono', valor: (c) => c.baseDatos ?? '—' },
  { titulo: 'Ejecuciones', clase: 'mono numero', valor: (c) => numero(c.ejecuciones) },
  { titulo: 'Duración promedio', clase: 'mono numero', valor: (c) => milisegundos(c.duracionPromedioMs) },
  {
    titulo: 'Proporción',
    valor: (c) => (
      <Medidor porcentaje={maximo > 0 ? (c.duracionPromedioMs / maximo) * 100 : 0} etiqueta="Duración promedio de la consulta" />
    ),
  },
  { titulo: 'CPU promedio', clase: 'mono numero', valor: (c) => milisegundos(c.cpuPromedioMs) },
  { titulo: 'Mínima', clase: 'mono numero', valor: (c) => milisegundos(c.duracionMinMs) },
  { titulo: 'Máxima', clase: 'mono numero', valor: (c) => milisegundos(c.duracionMaxMs) },
  { titulo: 'Lecturas promedio', clase: 'mono numero', valor: (c) => numero(c.lecturasPromedio) },
]

const COLUMNAS_SESIONES = [
  { titulo: 'Sesión', clase: 'mono numero', valor: (s) => s.idSesion },
  { titulo: 'Usuario', clase: 'mono', valor: (s) => s.usuario },
  { titulo: 'Equipo', clase: 'mono tenue', valor: (s) => s.equipo ?? '—' },
  { titulo: 'Programa', clase: 'tenue', valor: (s) => s.programa || '—' },
  { titulo: 'Base', clase: 'mono', valor: (s) => s.baseDatos ?? '—' },
  {
    titulo: 'Estado',
    valor: (s) => <span className={`chip ${s.estadoSolicitud ? 'ok' : ''}`}>{s.estadoSolicitud ?? s.estadoSesion}</span>,
  },
  { titulo: 'Comando', clase: 'mono', valor: (s) => s.comando ?? '—' },
  { titulo: 'Espera', clase: 'mono tenue', valor: (s) => s.tipoEspera ?? '—' },
  { titulo: 'CPU', clase: 'mono numero', valor: (s) => milisegundos(s.cpuMs) },
  { titulo: 'Memoria', clase: 'mono numero', valor: (s) => memoriaKb(s.memoriaKb) },
  { titulo: 'Inicio de sesión', clase: 'mono', valor: (s) => fechaHora(s.inicioSesion) },
]

/** Titular de la portada: lo más urgente primero (bloqueos), si no, cuántas sesiones hay. */
function titular(bloqueos, sesiones) {
  const bloqueadas = bloqueos.datos?.sesiones.length ?? 0
  if (bloqueadas > 0) return { titulo: `${bloqueadas} ${bloqueadas === 1 ? 'sesión bloqueada' : 'sesiones bloqueadas'}` }
  if (sesiones.datos) {
    const total = sesiones.datos.length
    return {
      titulo: `${total} ${total === 1 ? 'sesión activa' : 'sesiones activas'}`,
      subtitulo: bloqueos.datos ? 'sin bloqueos en este momento' : undefined,
    }
  }
  if (sesiones.error) return { titulo: 'Sin datos de rendimiento' }
  return { titulo: 'Monitoreo de rendimiento' }
}

/** Módulo 2: consultas de mayor consumo, tiempos promedio, sesiones, bloqueos y procesos. */
export default function RendimientoPage() {
  const [orden, setOrden] = useState('cpu')
  const [automatico, setAutomatico] = useState(false)

  const bloqueos = useConsulta(rendimiento.bloqueos)
  const procesos = useConsulta(() => rendimiento.procesos(orden), [orden])
  const consumo = useConsulta(rendimiento.consultasConsumo)
  const promedio = useConsulta(rendimiento.tiempoPromedio)
  const sesiones = useConsulta(rendimiento.sesiones)

  const consultas = [bloqueos, procesos, consumo, promedio, sesiones]
  const cargando = consultas.some((c) => c.cargando)
  const recargarTodo = () => consultas.forEach((c) => c.recargar())

  // Bloqueos y procesos cambian de un segundo a otro: se refrescan solos si el usuario lo pide.
  const { recargar: recargarBloqueos } = bloqueos
  const { recargar: recargarProcesos } = procesos
  const { recargar: recargarSesiones } = sesiones
  useEffect(() => {
    if (!automatico) return undefined
    const temporizador = setInterval(() => {
      recargarBloqueos()
      recargarProcesos()
      recargarSesiones()
    }, INTERVALO_MS)
    return () => clearInterval(temporizador)
  }, [automatico, recargarBloqueos, recargarProcesos, recargarSesiones])

  const porMemoria = orden === 'memoria'
  const maximoProcesos = Math.max(0, ...(procesos.datos ?? []).map((p) => valorProceso(p, porMemoria)))
  const maximoConsumo = Math.max(0, ...(consumo.datos ?? []).map((c) => c.cpuTotalMs))
  const maximoPromedio = Math.max(0, ...(promedio.datos ?? []).map((c) => c.duracionPromedioMs))
  const procesoMayor = procesos.datos?.[0]
  const { titulo, subtitulo } = titular(bloqueos, sesiones)
  const hayBloqueos = (bloqueos.datos?.sesiones.length ?? 0) > 0

  return (
    <>
      <Portada etiqueta="Monitoreo de rendimiento" titulo={titulo} subtitulo={subtitulo}>
        <div className="chips" style={{ alignItems: 'center' }}>
          {bloqueos.datos && (
            <span className={`chip ${hayBloqueos ? 'error' : 'ok'}`}>{hayBloqueos ? 'Hay bloqueos' : 'Sin bloqueos'}</span>
          )}
          {procesoMayor && (
            <span className="chip mono">
              Mayor {porMemoria ? 'memoria' : 'CPU'}: proceso {procesoMayor.idProceso} ·{' '}
              {porMemoria ? memoriaKb(valorProceso(procesoMayor, true)) : milisegundos(procesoMayor.cpuMs)}
            </span>
          )}
          <label className="chip" style={{ cursor: 'pointer' }}>
            <input type="checkbox" checked={automatico} onChange={(e) => setAutomatico(e.target.checked)} />
            Actualizar cada {INTERVALO_MS / 1000} s
          </label>
        </div>
      </Portada>

      <div className="contenido">
        <section className="bloque">
          <div className="bloque-encabezado">
            <h2 className="etiqueta">Sesiones bloqueadas</h2>
            <BotonActualizar cargando={cargando} etiqueta="Actualizar rendimiento" onClick={recargarTodo} />
          </div>
          <ContenidoConsulta consulta={bloqueos} filas={2}>
            {(datos) => (
              <>
                <p className={datos.sesiones.length > 0 ? 'nota' : 'tenue'} style={{ margin: 0, fontSize: 13 }}>
                  {datos.mensaje}
                </p>
                {datos.sesiones.length > 0 && (
                  <>
                    <Tabla
                      columnas={COLUMNAS_BLOQUEOS}
                      filas={datos.sesiones}
                      clave={(b) => `${b.idSesionBloqueada}-${b.idSesionBloqueadora}`}
                    />
                    <ul className="tenue" style={{ margin: 0, paddingLeft: 18, fontSize: 13 }}>
                      {datos.sesiones.map((b) => (
                        <li key={`${b.idSesionBloqueada}-${b.idSesionBloqueadora}`}>
                          <span className="mono">Sesión {b.idSesionBloqueada}:</span> {b.interpretacion}
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </>
            )}
          </ContenidoConsulta>
        </section>

        <section className="bloque">
          <div className="bloque-encabezado">
            <h2 className="etiqueta">Procesos con mayor consumo</h2>
            <label className="campo" htmlFor="orden-procesos">
              Ordenar por
              <select id="orden-procesos" value={orden} onChange={(e) => setOrden(e.target.value)}>
                <option value="cpu">CPU</option>
                <option value="memoria">Memoria</option>
              </select>
            </label>
          </div>
          <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
            Los 20 procesos (sesiones) que más consumen, incluidos los internos de SQL Server. El CPU suma lo acumulado por la
            sesión y lo que lleva la consulta en curso; la memoria concedida es la que usa la consulta que se ejecuta ahora.
          </p>
          <ContenidoConsulta consulta={procesos} vacio="Ningún proceso está consumiendo CPU ni memoria en este momento.">
            {(datos) => <Tabla columnas={columnasProcesos(porMemoria, maximoProcesos)} filas={datos} clave={(p) => p.idProceso} />}
          </ContenidoConsulta>
        </section>

        <section className="bloque">
          <h2 className="etiqueta">Consultas con mayor consumo de CPU</h2>
          <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
            Las 20 sentencias con más CPU acumulado desde que su plan entró en la caché. Pase el cursor sobre una consulta para ver
            el texto completo.
          </p>
          <ContenidoConsulta
            consulta={consumo}
            vacio="La caché de planes está vacía: todavía no se han ejecutado consultas desde que inició la instancia o se limpió la caché."
          >
            {(datos) => <Tabla columnas={columnasConsumo(maximoConsumo)} filas={datos} />}
          </ContenidoConsulta>
        </section>

        <section className="bloque">
          <h2 className="etiqueta">Tiempo promedio de ejecución</h2>
          <p className="tenue" style={{ margin: 0, fontSize: 13 }}>
            Las 20 sentencias más lentas en promedio por ejecución. Una diferencia grande entre la mínima y la máxima indica un
            rendimiento irregular (bloqueos, parámetros distintos o falta de índices).
          </p>
          <ContenidoConsulta
            consulta={promedio}
            vacio="La caché de planes está vacía: todavía no se han ejecutado consultas desde que inició la instancia o se limpió la caché."
          >
            {(datos) => <Tabla columnas={columnasPromedio(maximoPromedio)} filas={datos} />}
          </ContenidoConsulta>
        </section>

        <section className="bloque">
          <h2 className="etiqueta">Sesiones activas{sesiones.datos ? ` · ${sesiones.datos.length}` : ''}</h2>
          <ContenidoConsulta consulta={sesiones} vacio="No hay otras sesiones de usuario conectadas a la instancia.">
            {(datos) => <Tabla columnas={COLUMNAS_SESIONES} filas={datos} clave={(s) => s.idSesion} />}
          </ContenidoConsulta>
        </section>
      </div>
    </>
  )
}
