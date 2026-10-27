import { NavLink, Outlet } from 'react-router-dom'
import { salud } from '../api/endpoints.js'
import useConsulta from '../hooks/useConsulta.js'

/** Módulos del enunciado. Los que aún no tienen backend se marcan como pendientes. */
const MODULOS = [
  { ruta: '/instancia', nombre: 'Instancia' },
  { ruta: '/rendimiento', nombre: 'Rendimiento' },
  { ruta: '/almacenamiento', nombre: 'Almacenamiento' },
  { ruta: '/respaldos', nombre: 'Respaldos' },
  { ruta: '/auditoria', nombre: 'Auditoría' },
  { ruta: '/mantenimiento', nombre: 'Mantenimiento' },
]

/** Estructura común: barra superior con navegación y estado de la conexión. */
export default function Layout() {
  const { datos, error, cargando } = useConsulta(salud.estado)

  let estadoConexion = 'Verificando conexión…'
  if (error) estadoConexion = 'Sin conexión con la API'
  else if (datos) estadoConexion = datos.servidor

  return (
    <>
      <header className="barra">
        <NavLink to="/instancia" className="marca">
          <svg width="22" height="22" viewBox="0 0 22 22" aria-hidden="true">
            <path d="M2 20 L20 2 M2 11 L11 2 M11 20 L20 11" stroke="var(--dorado)" strokeWidth="3" fill="none" />
          </svg>
          DBA Monitor
        </NavLink>
        <nav className="navegacion" aria-label="Módulos">
          {MODULOS.map((modulo) => (
            <NavLink key={modulo.ruta} to={modulo.ruta} className={({ isActive }) => (isActive ? 'activo' : undefined)}>
              {modulo.nombre}
              {modulo.pendiente && <span className="pendiente">pronto</span>}
            </NavLink>
          ))}
        </nav>
        <span className="conexion mono" title={error || undefined}>
          <span className={`punto ${error ? 'error' : datos ? 'ok' : ''}`} aria-hidden="true" />
          {cargando && !datos ? 'Verificando conexión…' : estadoConexion}
        </span>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
