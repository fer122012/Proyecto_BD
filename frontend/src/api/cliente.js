/**
 * Cliente HTTP común para la API de Spring Boot.
 * La URL base se configura con VITE_API_URL (ver .env.example).
 */
const URL_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')

/** Error con el mensaje en español que devuelve el backend (ApiError) o uno propio. */
export class ErrorApi extends Error {
  constructor(mensaje, status) {
    super(mensaje)
    this.status = status
  }
}

async function solicitar(ruta, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(`${URL_BASE}${ruta}`, {
      ...opciones,
      headers: { 'Content-Type': 'application/json', ...opciones.headers },
    })
  } catch {
    throw new ErrorApi(
      `No se pudo contactar al backend en ${URL_BASE}. Verifique que la API esté en ejecución.`,
      0,
    )
  }

  const cuerpo = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new ErrorApi(cuerpo?.mensaje || `La API respondió con el código ${respuesta.status}.`, respuesta.status)
  }
  return cuerpo
}

export const obtener = (ruta) => solicitar(ruta)

export const enviar = (ruta, datos) => solicitar(ruta, { method: 'POST', body: JSON.stringify(datos) })
