import { instancia } from '../../api/endpoints.js'
import AnilloPorcentaje from '../../components/AnilloPorcentaje.jsx'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { Cargando, ContenidoConsulta, MensajeError } from '../../components/Estados.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { duracionLarga, fecha, fechaHora, numero } from '../../utils/formato.js'

const COLUMNAS_BASES = [
  { titulo: 'Nombre', clase: 'mono', valor: (b) => <span className={b.esSistema ? 'tenue' : undefined}>{b.nombre}</span> },
  {
    titulo: 'Estado',
    valor: (b) => <span className={`chip ${b.estado === 'ONLINE' ? 'ok' : 'error'}`}>{b.estado}</span>,
  },
  { titulo: 'Recuperación', valor: (b) => b.modeloRecuperacion },
  { titulo: 'Compat.', clase: 'mono numero', valor: (b) => b.nivelCompatibilidad },
  { titulo: 'Creada', clase: 'mono tenue', valor: (b) => fecha(b.fechaCreacion) },
  { titulo: 'Tipo', clase: 'tenue', valor: (b) => (b.esSistema ? 'Sistema' : 'Usuario') },
]

/** Módulo 1: estado general de la instancia de SQL Server. */
export default function InstanciaPage() {
  const resumen = useConsulta(instancia.resumen)
  const bases = useConsulta(instancia.basesDeDatos)

  const r = resumen.datos
  const enLinea = r?.estado === 'EN_LINEA'

  return (
    <>
      {r ? (
        <Portada
          etiqueta="Estado general de la instancia"
          titulo={duracionLarga(r.tiempoActividadSegundos)}
          subtitulo={enLinea ? 'sin interrupciones' : 'con alertas en sus bases'}
        >
          <div className="chips">
            <span className={`chip ${enLinea ? 'ok' : 'aviso'}`}>
              <span className={`punto ${enLinea ? 'ok' : ''}`} aria-hidden="true" />
              {enLinea ? 'En línea' : 'Requiere atención'}
            </span>
            <span className="chip mono">
              {r.versionComercial} · {r.version}
            </span>
            <span className="chip">{r.edicion}</span>
          </div>
        </Portada>
      ) : (
        <Portada
          etiqueta="Estado general de la instancia"
          titulo={resumen.error ? 'Sin datos de la instancia' : 'Consultando la instancia…'}
        >
          {resumen.error && <MensajeError mensaje={resumen.error} onReintentar={resumen.recargar} />}
        </Portada>
      )}

      <div className="contenido">
        {r?.basesConAlerta?.length > 0 && (
          <div className="mensaje error" role="alert">
            <span>
              <strong>Bases que no están en línea:</strong>{' '}
              {r.basesConAlerta.map((b) => `${b.nombre} (${b.estado}): ${b.motivo}`).join(' ')}
            </span>
          </div>
        )}

        {!resumen.error && (
          <div className="rejilla">
            <section className="bloque" style={{ alignItems: 'center', textAlign: 'center' }}>
              <h2 className="etiqueta" style={{ alignSelf: 'flex-start' }}>
                Memoria
              </h2>
              {resumen.cargando && !r ? (
                <Cargando filas={3} />
              ) : r ? (
                <>
                  <AnilloPorcentaje porcentaje={r.memoria.porcentajeUso} leyenda="del objetivo" />
                  <div className="cifras">
                    <div>
                      <span className="valor mono">{numero(r.memoria.enUsoMb)}</span>
                      <span className="nombre">En uso MB</span>
                    </div>
                    <div>
                      <span className="valor mono">{numero(r.memoria.asignadaMb)}</span>
                      <span className="nombre">Asignada MB</span>
                    </div>
                    <div>
                      <span className="valor mono">{numero(r.memoria.objetivoMb)}</span>
                      <span className="nombre">Objetivo MB</span>
                    </div>
                  </div>
                </>
              ) : null}
            </section>
  
            <section className="bloque">
              <h2 className="etiqueta">Identificación</h2>
              {resumen.cargando && !r ? (
                <Cargando />
              ) : r ? (
                <>
                  <dl className="datos">
                    <div>
                      <dt>Servidor</dt>
                      <dd className="mono">{r.servidor}</dd>
                    </div>
                    <div>
                      <dt>Instancia</dt>
                      <dd className="mono">{r.instancia}</dd>
                    </div>
                    <div>
                      <dt>Conexión</dt>
                      <dd className="mono">{r.nombreConexion}</dd>
                    </div>
                    <div>
                      <dt>Gestor</dt>
                      <dd>
                        {r.tipoGestor} · {r.nivel}
                      </dd>
                    </div>
                    <div>
                      <dt>Inicio</dt>
                      <dd className="mono">{fechaHora(r.fechaInicio)}</dd>
                    </div>
                    <div>
                      <dt>Activa</dt>
                      <dd className="mono">{r.tiempoActividadTexto}</dd>
                    </div>
                    <div>
                      <dt>Memoria máx.</dt>
                      <dd>{r.memoria.maximaConfiguradaTexto}</dd>
                    </div>
                  </dl>
                  {r.limitacionesEdicion?.length > 0 && (
                    <div className="nota">
                      Limitaciones de la edición {r.edicion}:
                      <ul>
                        {r.limitacionesEdicion.map((limitacion) => (
                          <li key={limitacion}>{limitacion}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </>
              ) : null}
            </section>
          </div>
        )}

        <section className="bloque">
          <div className="bloque-encabezado">
            <h2 className="etiqueta">Bases de datos administradas{bases.datos ? ` · ${bases.datos.length}` : ''}</h2>
            <BotonActualizar
              cargando={bases.cargando || resumen.cargando}
              etiqueta="Actualizar estado de la instancia"
              onClick={() => {
                resumen.recargar()
                bases.recargar()
              }}
            />
          </div>
          <ContenidoConsulta consulta={bases} vacio="La instancia no tiene bases de datos.">
            {(datos) => <Tabla columnas={COLUMNAS_BASES} filas={datos} clave={(b) => b.nombre} />}
          </ContenidoConsulta>
        </section>
      </div>
    </>
  )
}
