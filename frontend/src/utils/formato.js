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

/** "45 s", "3 min 20 s" o "1 h 5 min" a partir de segundos, para el Módulo 4. */
export function duracion(segundos) {
  if (segundos == null) return '—'
  if (segundos < 60) return `${segundos} s`
  const horas = Math.floor(segundos / 3600)
  const minutos = Math.floor((segundos % 3600) / 60)
  if (horas > 0) return `${horas} h ${minutos} min`
  return `${minutos} min ${segundos % 60} s`
}
