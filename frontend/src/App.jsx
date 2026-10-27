import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import ModuloPendiente from './components/ModuloPendiente.jsx'
import InstanciaPage from './modulos/instancia/InstanciaPage.jsx'
import RendimientoPage from './modulos/rendimiento/RendimientoPage.jsx'
import AlmacenamientoPage from './modulos/almacenamiento/AlmacenamientoPage.jsx'
import AuditoriaPage from './modulos/auditoria/AuditoriaPage.jsx'
import MantenimientoPage from './modulos/mantenimiento/MantenimientoPage.jsx'

/** Rutas de la aplicación: una por módulo del enunciado. */
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/instancia" replace />} />
        <Route path="instancia" element={<InstanciaPage />} />
        <Route path="rendimiento" element={<RendimientoPage />} />
        <Route path="almacenamiento" element={<AlmacenamientoPage />} />
        <Route path="respaldos" element={<ModuloPendiente numero={4} nombre="Respaldo y recuperación" />} />
        <Route path="auditoria" element={<AuditoriaPage />} />
        <Route path="mantenimiento" element={<MantenimientoPage />} />
        <Route path="*" element={<Navigate to="/instancia" replace />} />
      </Route>
    </Routes>
  )
}
