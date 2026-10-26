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
