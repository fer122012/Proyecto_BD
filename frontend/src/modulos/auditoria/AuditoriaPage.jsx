import { useState } from 'react'
import { auditoria } from '../../api/endpoints.js'
import BotonActualizar from '../../components/BotonActualizar.jsx'
import { ContenidoConsulta, SinDatos } from '../../components/Estados.jsx'
import Portada from '../../components/Portada.jsx'
import Tabla from '../../components/Tabla.jsx'
import useConsulta from '../../hooks/useConsulta.js'
import { fecha } from '../../utils/formato.js'

const TIPOS_LOGIN = {
  SQL_LOGIN: 'SQL',
  WINDOWS_LOGIN: 'Windows',
  WINDOWS_GROUP: 'Grupo Windows',
}

const COLUMNAS_LOGINS = [
  { titulo: 'Login', clase: 'mono', valor: (l) => l.nombre },
  { titulo: 'Tipo', valor: (l) => TIPOS_LOGIN[l.tipo] ?? l.tipo },
  {
    titulo: 'Estado',
    valor: (l) => <span className={`chip ${l.habilitado ? 'ok' : ''}`}>{l.habilitado ? 'Habilitado' : 'Deshabilitado'}</span>,
  },
  { titulo: 'Base predeterminada', valor: (l) => l.baseDatosPredeterminada ?? '—' },
  { titulo: 'Creado', clase: 'mono tenue', valor: (l) => fecha(l.fechaCreacion) },
  {
    titulo: 'Sysadmin',
    valor: (l) => (l.esSysadmin ? <span className="chip aviso">Sí</span> : <span className="tenue">No</span>),
  },
]

const COLUMNAS_PRIVILEGIOS = [
  { titulo: 'Login', clase: 'mono', valor: (p) => p.login },
  { titulo: 'Permiso', valor: (p) => p.permiso },
  {
    titulo: 'Estado',
    valor: (p) => <span className={`chip ${p.estado === 'DENY' ? 'error' : 'ok'}`}>{p.estado.replaceAll('_', ' ')}</span>,
  },
]

const COLUMNAS_OBJETOS = [
  { titulo: 'Esquema', clase: 'mono', valor: (o) => o.esquema },
  { titulo: 'Objeto', clase: 'mono', valor: (o) => o.objeto },
  { titulo: 'Tipo', valor: (o) => o.tipo },
  {
    titulo: 'Hace referencia a',
    clase: 'mono',
    valor: (o) => (
      <span style={{ color: 'var(--error)' }}>
        {o.esquemaReferenciado ? `${o.esquemaReferenciado}.` : ''}
        {o.objetoReferenciado}
      </span>
    ),
  },
]

/** Módulo 5: logins, roles, privilegios y objetos con dependencias rotas. */
export default function AuditoriaPage() {
  const logins = useConsulta(auditoria.logins)
  const roles = useConsulta(auditoria.roles)
  const privilegios = useConsulta(auditoria.privilegios)
  const objetos = useConsulta(auditoria.objetosInvalidos)
  const [busqueda, setBusqueda] = useState('')

  const actualizarTodo = () => {
    logins.recargar()
    roles.recargar()
    privilegios.recargar()
    objetos.recargar()
  }
  const cargando = logins.cargando || roles.cargando || privilegios.cargando || objetos.cargando

  const listaLogins = logins.datos ?? []
  const sysadmins = listaLogins.filter((l) => l.esSysadmin).length
  const deshabilitados = listaLogins.filter((l) => !l.habilitado).length
  const texto = busqueda.trim().toLowerCase()
  const loginsFiltrados = texto ? listaLogins.filter((l) => l.nombre.toLowerCase().includes(texto)) : listaLogins
  const privilegiosFiltrados = texto
    ? (privilegios.datos ?? []).filter((p) => p.login.toLowerCase().includes(texto))
    : privilegios.datos

  return (
    <>
      <Portada
        etiqueta="Auditoría"
        titulo={logins.datos ? `${listaLogins.length} logins registrados` : 'Consultando la seguridad…'}
        subtitulo={logins.datos ? `${sysadmins} con control total` : undefined}
      >
        {logins.datos && (
          <div className="chips">
            <span className="chip aviso">{sysadmins} sysadmin</span>
            <span className="chip">{deshabilitados} deshabilitados</span>
            {objetos.datos && (
              <span className={`chip ${objetos.datos.length > 0 ? 'error' : 'ok'}`}>
                {objetos.datos.length === 0 ? 'Sin objetos inválidos' : `${objetos.datos.length} ${objetos.datos.length === 1 ? 'objeto inválido' : 'objetos inválidos'}`}
              </span>
            )}
          </div>
        )}
      </Portada>

      <div className="contenido">
        <section className="bloque">
          <div className="bloque-encabezado">
            <h2 className="etiqueta">Logins registrados</h2>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
              <label className="campo" htmlFor="buscar-login">
                Buscar
                <input
                  id="buscar-login"
                  type="search"
                  placeholder="Nombre del login"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
              </label>
              <BotonActualizar cargando={cargando} etiqueta="Actualizar auditoría" onClick={actualizarTodo} />
            </div>
          </div>
          <ContenidoConsulta consulta={logins} vacio="No hay logins registrados.">
            {() =>
              loginsFiltrados.length === 0 ? (
                <SinDatos>Ningún login coincide con “{busqueda}”.</SinDatos>
              ) : (
                <Tabla columnas={COLUMNAS_LOGINS} filas={loginsFiltrados} clave={(l) => l.nombre} />
              )
            }
          </ContenidoConsulta>
        </section>

        <div className="rejilla">
          <section className="bloque">
            <h2 className="etiqueta">Roles de servidor y miembros</h2>
            <ContenidoConsulta consulta={roles} vacio="No se encontraron roles de servidor.">
              {(datos) => (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14, fontSize: 14 }}>
                  {datos.map((rol) => (
                    <div key={rol.rol} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <span className="mono" style={{ fontWeight: 500 }}>
                        {rol.rol}
                      </span>
                      {rol.miembros.length === 0 ? (
                        <span className="tenue">Sin miembros</span>
                      ) : (
                        <div className="chips" style={{ gap: 6 }}>
                          {rol.miembros.map((miembro) => (
                            <span key={miembro} className="chip mono" style={{ fontSize: 12 }}>
                              {miembro}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </ContenidoConsulta>
          </section>

          <section className="bloque">
            <h2 className="etiqueta">Privilegios por login</h2>
            <ContenidoConsulta consulta={privilegios} vacio="No hay permisos asignados directamente a logins.">
              {() =>
                privilegiosFiltrados.length === 0 ? (
                  <SinDatos>Ningún privilegio coincide con “{busqueda}”.</SinDatos>
                ) : (
                  <Tabla
                    columnas={COLUMNAS_PRIVILEGIOS}
                    filas={privilegiosFiltrados}
                    clave={(p) => `${p.login}-${p.permiso}-${p.estado}`}
                  />
                )
              }
            </ContenidoConsulta>
          </section>
        </div>

        <section className="bloque">
          <h2 className="etiqueta">Objetos con dependencias rotas</h2>
          <ContenidoConsulta consulta={objetos} vacio="No hay objetos inválidos: todas las dependencias se resuelven.">
            {(datos) => (
              <Tabla
                columnas={COLUMNAS_OBJETOS}
                filas={datos}
                clave={(o) => `${o.esquema}.${o.objeto}-${o.esquemaReferenciado}.${o.objetoReferenciado}`}
              />
            )}
          </ContenidoConsulta>
        </section>
      </div>
    </>
  )
}
