import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ModuloPendiente from './components/ModuloPendiente.jsx'
import InstanciaPage from './modulos/instancia/InstanciaPage.jsx'
import AuditoriaPage from './modulos/auditoria/AuditoriaPage.jsx'
import MantenimientoPage from './modulos/mantenimiento/MantenimientoPage.jsx'

/** Rutas de la aplicación: una por módulo del enunciado. */
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/instancia" replace />} />
        <Route path="instancia" element={<InstanciaPage />} />
        <Route path="rendimiento" element={<ModuloPendiente numero={2} nombre="Monitoreo de rendimiento" />} />
        <Route path="almacenamiento" element={<ModuloPendiente numero={3} nombre="Gestión del almacenamiento" />} />
        <Route path="respaldos" element={<ModuloPendiente numero={4} nombre="Respaldo y recuperación" />} />
        <Route path="auditoria" element={<AuditoriaPage />} />
        <Route path="mantenimiento" element={<MantenimientoPage />} />
        <Route path="*" element={<Navigate to="/instancia" replace />} />
      </Route>
    </Routes>
  )
}
