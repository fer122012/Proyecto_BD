# Evidencias de pruebas

Registro de las pruebas de DBA Monitor contra una instancia real de SQL Server.

> **Plantilla parcial.** La sección 1 contiene resultados que ya se obtuvieron. En la sección 3, la
> columna *Resultado obtenido* debe llenarse solo después de ejecutar cada prueba, y cada captura
> debe guardarse en `docs/evidencias/` con el nombre indicado. Quiten esta nota al terminar.

## 1. Verificaciones ya ejecutadas

Ejecutadas el 2026-11-02 sobre SQL Server 2022 Express (`SQLEXPRESS`).

| Verificación | Cómo se ejecuta | Resultado |
|---|---|---|
| Pruebas del backend | `.\mvnw.cmd test` desde `backend/` | 28 pruebas, 0 fallos, 0 errores |
| Compilación del frontend | `npm run build` desde `frontend/` | Correcta |
| Análisis estático del frontend | `npm run lint` desde `frontend/` | Sin errores |
| Endpoints de lectura de los seis módulos | Solicitud GET a cada ruta, con `BASE_MONITOR` donde se pide una base | 24 de 24 responden HTTP 200 |

En esa revisión de los endpoints, dos rutas del Módulo 2 respondieron sin filas
(`/api/rendimiento/consultas-consumo` y `/api/rendimiento/tiempo-promedio`) porque la caché de planes
estaba vacía, y el historial de `BASE_MONITOR` tenía un solo respaldo. Por eso la sección 2 se hace
antes de las pruebas.

## 2. Preparación

Ejecutar desde la raíz del repositorio, con un usuario administrador:

```
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\05_generar_historial_respaldos.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\06_generar_carga_rendimiento.sql -o NUL
```

El script 05 hace respaldos reales y agrega ventas a `BASE_MONITOR`; ocupa unos 250 MB por ejecución.
El script 06 se ejecuta justo antes de probar el Módulo 2.

Datos de la ejecución:

- **Fecha:**
- **Responsable:**
- **Equipo e instancia:**

## 3. Pruebas funcionales

Estado: *Cumple*, *Cumple con limitación* o *No cumple*.

### Módulo 1. Estado general de la instancia

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                          |
|---|---|---|---|---|----------------------------------|
| M1-01 | Abrir **Instancia** | Muestra servidor, instancia, tipo, versión, estado, inicio y tiempo de actividad | | | `![img.png](evidencias/img_1.png)` |
| M1-02 | Revisar la memoria | Muestra memoria asignada y en uso, con unidades | | | `![img.png](evidencias/img.png)` |
| M1-03 | Revisar las bases de datos | Lista las bases de la instancia | | | `![img.png](img_2.png)`          |
| M1-04 | Cambiar la URL en `application-local.properties` y reiniciar el backend | La aplicación muestra la instancia configurada, sin cambiar código | | | `m1-04-cambio-instancia.png`     |

### Módulo 2. Monitoreo de rendimiento

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                   |
|---|---|---|---|---|---------------------------|
| M2-01 | Revisar las consultas de mayor consumo después del script 06 | Lista consultas ordenadas por consumo | | | `![img_3.png](img_3.png)` |
| M2-02 | Revisar el tiempo promedio | Muestra el tiempo de ejecución, con unidades | | | `![img_4.png](img_4.png)` |
| M2-03 | Revisar las sesiones activas | Lista las sesiones con información relevante | | | `![img_5.png](img_5.png)` |
| M2-04 | Revisar bloqueos sin bloqueos activos | Informa claramente que no existen | | | `![img_6.png](img_6.png)` |
| M2-05 | Provocar un bloqueo con `prueba_sesiones_bloqueadas.sql` | Muestra la sesión bloqueada y la bloqueadora | | | `m2-05-bloqueo.png`       |
| M2-06 | Revisar los procesos de mayor consumo | Lista procesos con CPU y memoria | | | `![img_7.png](img_7.png)` |

### Módulo 3. Gestión del almacenamiento

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                     |
|---|---|---|---|---|-----------------------------|
| M3-01 | Seleccionar `BASE_MONITOR` y revisar los filegroups | Muestra el espacio utilizado y disponible | | | `![img_8.png](img_8.png)`   |
| M3-02 | Revisar el tamaño de la base | Muestra el tamaño actual | | | `![img_9.png](img_9.png)`   |
| M3-03 | Revisar los archivos de datos | Muestra nombre, tamaño y otros datos | | | `![img_10.png](img_10.png)` |
| M3-04 | Revisar los objetos de mayor tamaño | Lista tablas o índices ordenados por tamaño | | | `![img_11.png](img_11.png)` |
| M3-05 | Revisar el crecimiento | El gráfico muestra varios puntos del historial | | | `![img_11.png](img_11.png)` |

### Módulo 4. Respaldo y recuperación

| ID    | Prueba                                  | Resultado esperado                                | Resultado obtenido | Estado | Captura                                     |
|-------|-----------------------------------------|---------------------------------------------------|--------------------|--------|---------------------------------------------|
| M4-01 | Revisar el último respaldo de cada base | Muestra fecha, hora y tipo                        |                    |        | `![img_13.png](img_13.png)`                 |
| M4-02 | Revisar el historial de `BASE_MONITOR`  | Muestra varios respaldos con fecha, tipo y estado |                    |        | `![img_14.png](img_14.png)`                 |
| M4-03 | Revisar el nivel de riesgo              | Explica si la condición es adecuada o un riesgo   |                    |        | `![img_15.png](img_15.png)` |

### Módulo 5. Auditoría

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                     |
|---|---|---|---|---|-----------------------------|
| M5-01 | Revisar los logins y los usuarios de `BASE_MONITOR` | Lista usuarios con información administrativa | | | `![img_16.png](img_16.png)` |
| M5-02 | Revisar los roles de servidor y de base | Muestra los roles y sus miembros | | | `![img_17.png](img_17.png)`           |
| M5-03 | Revisar los privilegios | Muestra privilegios y a quién pertenecen | | | `![img_18.png](img_18.png)`     |
| M5-04 | Revisar los objetos inválidos de `BASE_MONITOR` | Identifica cada objeto afectado y su causa | | | `![img_19.png](img_19.png)`       |

### Módulo 6. Mantenimiento preventivo

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                      |
|---|---|---|---|---|------------------------------|
| M6-01 | Recalcular las estadísticas de una tabla | Confirma el resultado y la fecha de la estadística cambia | | | `![img_20.png](img_20.png)`  |
| M6-02 | Revisar los objetos inválidos | Lista los objetos que requieren intervención | | | `![img_21.png](img_21.png)`        |
| M6-03 | Recompilar un objeto válido | Confirma que la operación fue correcta | | | `![img_22.png](img_22.png)`    |
| M6-04 | Recompilar un objeto con referencias rotas | Informa el error o las referencias rotas | | | `![img_23.png](img_23.png)` |

## 4. Pruebas de robustez

| ID | Prueba | Resultado esperado | Resultado obtenido | Estado | Captura                     |
|---|---|---|---|---|-----------------------------|
| R-01 | Detener la instancia con la aplicación abierta | Muestra un mensaje claro de error de conexión y la aplicación sigue abierta | | | `![img_24.png](img_24.png)` |
| R-02 | Detener el backend con la aplicación abierta | Indica *Sin conexión con la API* | | | `![img_25.png](img_25.png)` |
| R-03 | Consultar una sección sin resultados | Muestra un mensaje en lugar de una tabla vacía | | | `![img_26.png](img_26.png)`   |

## 5. Hallazgos y correcciones

| Fecha | Hallazgo | Corrección o decisión |
|---|---|---|
|  |  |  |
