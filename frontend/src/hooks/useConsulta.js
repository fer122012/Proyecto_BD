import { useCallback, useEffect, useState } from 'react'

/**
 * Ejecuta una consulta a la API y expone su estado: datos, cargando, error y recargar().
 * `dependencias` vuelve a ejecutar la consulta cuando cambian (por ejemplo, la base elegida).
 * Si `consulta` es null, no se ejecuta nada.
 */
export default function useConsulta(consulta, dependencias = []) {
  const [estado, setEstado] = useState({ datos: null, cargando: Boolean(consulta), error: null })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!consulta) {
      setEstado({ datos: null, cargando: false, error: null })
      return
    }
    let vigente = true
    setEstado((previo) => ({ ...previo, cargando: true, error: null }))
    consulta()
      .then((datos) => vigente && setEstado({ datos, cargando: false, error: null }))
      .catch((error) => vigente && setEstado({ datos: null, cargando: false, error: error.message }))
    return () => {
      vigente = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencias, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  return { ...estado, recargar }
}
