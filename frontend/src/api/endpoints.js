import { enviar, obtener } from './cliente.js'

/** Rutas de la API agrupadas por módulo. */

export const salud = {
  estado: () => obtener('/api/salud'),
}

export const instancia = {
  resumen: () => obtener('/api/instancia'),
  basesDeDatos: () => obtener('/api/instancia/bases-de-datos'),
}

export const auditoria = {
  logins: () => obtener('/api/auditoria/logins'),
  roles: () => obtener('/api/auditoria/roles'),
  privilegios: () => obtener('/api/auditoria/privilegios'),
  objetosInvalidos: () => obtener('/api/auditoria/objetos-invalidos'),
}

const conBase = (ruta, baseDatos) => `${ruta}?baseDatos=${encodeURIComponent(baseDatos)}`

export const almacenamiento = {
  basesDeDatos: () => obtener('/api/almacenamiento/bases-de-datos'),
  filegroups: (baseDatos) => obtener(conBase('/api/almacenamiento/filegroups', baseDatos)),
  archivos: (baseDatos) => obtener(conBase('/api/almacenamiento/archivos', baseDatos)),
  objetos: (baseDatos) => obtener(conBase('/api/almacenamiento/objetos', baseDatos)),
  crecimiento: (baseDatos) => obtener(conBase('/api/almacenamiento/crecimiento', baseDatos)),
}

export const mantenimiento = {
  estadisticas: (baseDatos) => obtener(conBase('/api/mantenimiento/estadisticas', baseDatos)),
  objetosInvalidos: (baseDatos) => obtener(conBase('/api/mantenimiento/objetos-invalidos', baseDatos)),
  recalcularEstadisticas: (solicitud) => enviar('/api/mantenimiento/estadisticas/recalcular', solicitud),
  recompilarObjeto: (solicitud) => enviar('/api/mantenimiento/objetos/recompilar', solicitud),
}
