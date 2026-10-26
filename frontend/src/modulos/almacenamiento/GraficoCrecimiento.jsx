import { useEffect, useRef, useState } from 'react'
import { fecha, tamano } from '../../utils/formato.js'

const ALTO = 240
const MARGEN = { arriba: 12, derecha: 16, abajo: 28, izquierda: 64 }
const LINEAS_GUIA = 4

/**
 * Gráfico de líneas del tamaño de la base en cada respaldo completo: espacio usado (dorado) y
 * tamaño asignado a los archivos de datos (gris). Los puntos se reparten de forma pareja en el
 * eje horizontal porque los respaldos no siempre son periódicos.
 * Se dibuja con el ancho real del contenedor para que el texto conserve su tamaño en cualquier pantalla.
 */
export default function GraficoCrecimiento({ puntos }) {
  const contenedor = useRef(null)
  const [ancho, setAncho] = useState(640)

  useEffect(() => {
    const observador = new ResizeObserver(([entrada]) => setAncho(Math.max(entrada.contentRect.width, 280)))
    observador.observe(contenedor.current)
    return () => observador.disconnect()
  }, [])

  const anchoUtil = ancho - MARGEN.izquierda - MARGEN.derecha
  const altoUtil = ALTO - MARGEN.arriba - MARGEN.abajo
  const maximo = Math.max(...puntos.flatMap((p) => [p.usadoMb, p.tamanoDatosMb]), 1) * 1.1

  const x = (indice) => MARGEN.izquierda + (puntos.length === 1 ? anchoUtil / 2 : (indice * anchoUtil) / (puntos.length - 1))
  const y = (valor) => MARGEN.arriba + altoUtil - (valor / maximo) * altoUtil
  const trazo = (campo) => puntos.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i)},${y(p[campo])}`).join(' ')

  return (
    <div ref={contenedor}>
      <svg
        className="grafico"
        width={ancho}
        height={ALTO}
        viewBox={`0 0 ${ancho} ${ALTO}`}
        role="img"
        aria-label={`Crecimiento entre ${fecha(puntos[0].fecha)} y ${fecha(puntos[puntos.length - 1].fecha)}`}
      >
        {Array.from({ length: LINEAS_GUIA + 1 }, (_, i) => {
          const valor = (maximo / LINEAS_GUIA) * i
          return (
            <g key={i}>
              <line x1={MARGEN.izquierda} x2={ancho - MARGEN.derecha} y1={y(valor)} y2={y(valor)} stroke="var(--linea)" />
              <text x={MARGEN.izquierda - 8} y={y(valor) + 4} textAnchor="end" fill="var(--texto-tenue)" fontSize="11" fontFamily="var(--fuente-mono)">
                {tamano(valor)}
              </text>
            </g>
          )
        })}
        <path d={trazo('tamanoDatosMb')} fill="none" stroke="var(--texto-tenue)" strokeWidth="2" strokeDasharray="5 4" />
        <path d={trazo('usadoMb')} fill="none" stroke="var(--dorado)" strokeWidth="2.5" />
        {puntos.map((p, i) => (
          <circle key={p.fecha} cx={x(i)} cy={y(p.usadoMb)} r="4" fill="var(--dorado)">
            <title>{`${fecha(p.fecha)}: ${tamano(p.usadoMb)} usados de ${tamano(p.tamanoDatosMb)} asignados`}</title>
          </circle>
        ))}
        <text x={x(0)} y={ALTO - 8} textAnchor={puntos.length === 1 ? 'middle' : 'start'} fill="var(--texto-tenue)" fontSize="11" fontFamily="var(--fuente-mono)">
          {fecha(puntos[0].fecha)}
        </text>
        {puntos.length > 1 && (
          <text x={x(puntos.length - 1)} y={ALTO - 8} textAnchor="end" fill="var(--texto-tenue)" fontSize="11" fontFamily="var(--fuente-mono)">
            {fecha(puntos[puntos.length - 1].fecha)}
          </text>
        )}
      </svg>
    </div>
  )
}
