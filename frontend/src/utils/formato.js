/** Funciones para presentar fechas y números de forma consistente en toda la interfaz. */

const formatoNumero = new Intl.NumberFormat('es-CR')

const formatoFecha = new Intl.DateTimeFormat('es-CR', { day: '2-digit', month: '2-digit', year: 'numeric' })

const formatoFechaHora = new Intl.DateTimeFormat('es-CR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export const numero = (valor) => (valor == null ? '—' : formatoNumero.format(valor))

export const fecha = (valor) => (valor ? formatoFecha.format(new Date(valor)) : '—')

export const fechaHora = (valor) => (valor ? formatoFechaHora.format(new Date(valor)) : '—')

/** "3 días, 14 h, 22 min" a partir de segundos, para el titular del Módulo 1. */
export function duracionLarga(segundos) {
  const dias = Math.floor(segundos / 86400)
  const horas = Math.floor((segundos % 86400) / 3600)
  const partes = []
  if (dias > 0) partes.push(`${dias} ${dias === 1 ? 'día' : 'días'}`)
  partes.push(`${horas} ${horas === 1 ? 'hora' : 'horas'}`)
  return partes.join(', ')
}

const formatoDecimal = new Intl.NumberFormat('es-CR', { maximumFractionDigits: 1 })

/** Tamaño en MB como "512 MB" o "1,5 GB", para el Módulo 3. */
export function tamano(mb) {
  if (mb == null) return '—'
  return mb >= 1024 ? `${formatoDecimal.format(mb / 1024)} GB` : `${formatoDecimal.format(mb)} MB`
}

/** Tiempo en milisegundos como "850 ms", "4,6 s" o "2,5 min", para el Módulo 2. */
export function milisegundos(ms) {
  if (ms == null) return '—'
  if (ms < 1000) return `${formatoDecimal.format(ms)} ms`
  if (ms < 60000) return `${formatoDecimal.format(ms / 1000)} s`
  return `${formatoDecimal.format(ms / 60000)} min`
}

/** Memoria en KB como "32 KB" o "1,5 MB", para el Módulo 2. */
export function memoriaKb(kb) {
  if (kb == null) return '—'
  return kb >= 1024 ? tamano(kb / 1024) : `${formatoNumero.format(kb)} KB`
}
