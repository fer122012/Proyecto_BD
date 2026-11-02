# Manual de usuario – DBA Monitor

DBA Monitor es una aplicación web para monitorear, administrar y auditar una instancia de SQL
Server. Este manual explica cómo usar cada pantalla y cómo interpretar lo que muestra. Toda la
información sale de consultas reales a la instancia conectada.

- Para instalar la aplicación: [README](../README.md) e [instalación detallada](instalacion.md).
- Para el detalle técnico de cada indicador: `docs/modulo1-instancia.md` a `docs/modulo6-mantenimiento.md`.

## 1. Antes de empezar

### Iniciar la aplicación

1. Verificar que el servicio de SQL Server esté iniciado.
2. Iniciar el backend, que queda en `http://localhost:8080`:
   ```
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
3. En otra terminal, iniciar el frontend:
   ```
   cd frontend
   npm install
   npm run dev
   ```
4. Abrir **http://localhost:5173**. La aplicación abre en el Módulo 1.

### Tener datos que mostrar

Los scripts de `database/scripts/` preparan la base de práctica `BASE_MONITOR` y los permisos. El
[README](../README.md) indica el orden y la [guía de instalación](instalacion.md) explica cada uno.

| Para ver | Ejecutar |
|---|---|
| Módulos 3, 5 y 6 sobre una base | `00` (crea `BASE_MONITOR`), `02` y `03` (permisos) |
| Módulo 4 con historial y Módulo 3 con crecimiento | `04` (permisos) y `05` (hace respaldos reales; ejecutarlo en días distintos da un promedio diario) |
| Módulo 2 con consultas | `06`, justo antes de abrir la pantalla |
| Módulo 2 con una sesión bloqueada | Los pasos de `prueba_sesiones_bloqueadas.sql` |

### Elementos comunes de todas las pantallas

- **Barra superior:** da acceso a los seis módulos. A la derecha, un punto verde y el nombre de la
  instancia indican que hay conexión; si aparece *Sin conexión con la API*, el backend no responde.
- **Portada:** resume lo más importante del módulo en una frase, con etiquetas de color.
- **Botón ⟳:** vuelve a consultar los datos del módulo. Las pantallas no se actualizan solas, salvo
  la opción de actualización automática del Módulo 2.
- **Colores:** verde significa normal, amarillo requiere atención y rojo es crítico o error.
- **Lista Base de datos:** los módulos 3, 5 y 6 trabajan sobre una base. Solo ofrecen bases de usuario
  en línea a las que la aplicación tiene acceso.
- **Si algo falla:** aparece un mensaje en rojo con la causa y el botón **Reintentar**. Los mensajes
  más comunes están en la [sección 9](#9-mensajes-comunes-y-qué-hacer).

## 2. Módulo 1 – Instancia

Responde a la pregunta: *¿qué instancia estoy monitoreando y está funcionando bien?*

Abrir **Instancia** en la barra superior. Se muestra:

1. **Portada:** el tiempo que lleva activa la instancia, su estado (*En línea* o *Requiere atención*),
   la versión y la edición.
2. **Memoria:** el anillo indica qué parte de la memoria objetivo está en uso. Debajo se ven la memoria
   en uso, la asignada y la objetivo, en MB. Es normal que estos valores sean altos, porque SQL Server
   mantiene datos en caché para no leerlos de nuevo del disco.
3. **Identificación:** servidor, instancia, nombre de conexión, gestor, fecha de inicio, tiempo de
   actividad y memoria máxima configurada. Si la edición es Express, también lista sus limitaciones.
4. **Bases de datos administradas:** nombre, estado, modelo de recuperación, nivel de compatibilidad,
   fecha de creación y si es de sistema o de usuario.

**Cómo interpretarlo:** si una base no está en línea, el estado pasa a *Requiere atención* y un mensaje
en rojo indica cuál es y qué significa su estado.

### Cambiar la instancia monitoreada

La instancia se configura fuera del código, en la URL de conexión. Solo se monitorea una a la vez.

1. Verificar que la otra instancia tenga TCP/IP habilitado en su propio puerto: dos instancias no
   pueden usar el mismo.
2. Ejecutar en esa instancia los scripts de login y permisos (`01` a `04`).
3. Cambiar el servidor o el puerto de `spring.datasource.url` en
   `backend/src/main/resources/application-local.properties`, junto con el usuario y la contraseña si
   son distintos:
   ```
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true
   ```
   También se puede definir la variable de entorno `SPRING_DATASOURCE_URL`, sin editar el archivo.
4. Reiniciar el backend. El Módulo 1 mostrará la nueva instancia.

## 3. Módulo 2 – Rendimiento

Responde a la pregunta: *¿qué está haciendo lenta a la instancia en este momento?*

Abrir **Rendimiento**. La portada indica si hay sesiones bloqueadas o cuántas sesiones activas hay, y
cuál es el proceso que más consume. La casilla **Actualizar cada 15 s** refresca solos los bloqueos,
los procesos y las sesiones. Debajo aparecen:

1. **Sesiones bloqueadas:** un mensaje indica si hay bloqueos y cuál es la sesión raíz. Si los hay, una
   tabla muestra quién bloquea a quién, cuánto lleva esperando y las dos consultas. El estado es
   *Atención* si espera menos de 30 s y *Crítico* con 30 s o más. Debajo se explica qué hacer con cada una.
2. **Procesos con mayor consumo:** los 20 procesos que más consumen. La lista **Ordenar por** cambia
   entre CPU y memoria, y la barra compara cada proceso con el mayor.
3. **Consultas con mayor consumo de CPU:** las 20 sentencias con más CPU acumulado desde que su plan
   entró en la caché, con ejecuciones, duración, lecturas y escrituras.
4. **Tiempo promedio de ejecución:** las 20 sentencias más lentas por ejecución. Una diferencia grande
   entre la mínima y la máxima indica un rendimiento irregular.
5. **Sesiones activas:** las sesiones de usuario conectadas y lo que están ejecutando.

Las consultas largas aparecen recortadas; al pasar el cursor se ve el texto completo.

**Si las consultas aparecen vacías:** la caché de planes está vacía. Ejecutar `06_generar_carga_rendimiento.sql`
y abrir la pantalla enseguida. Si se vuelve a vaciar, puede faltar memoria libre en el equipo (ver
`docs/modulo2-rendimiento.md`).

## 4. Módulo 3 – Almacenamiento

Responde a la pregunta: *¿cuánto ocupa cada base, qué tan llenos están sus filegroups, qué ocupa más
espacio y cuánto ha crecido?*

Abrir **Almacenamiento** y elegir la base en la lista **Base de datos**. La portada muestra cuánto ocupa
en disco (datos y log) y si algún filegroup requiere atención. Debajo aparecen:

1. **Espacio por filegroup:** una barra con lo usado, lo disponible y el porcentaje, además de una
   explicación de qué hacer.

   | Estado | Significado |
   |---|---|
   | *Normal* | Menos de 85 % de uso, o filegroup de solo lectura. |
   | *Atención* | 85 % o más, pero algún archivo puede crecer solo. |
   | *Crítico* | 85 % o más y ningún archivo puede crecer: al llenarse, las inserciones fallarán. |

2. **Crecimiento:** gráfico y tabla con el tamaño de la base en cada respaldo completo y la variación
   entre uno y otro. Con menos de dos respaldos, un aviso explica por qué no se puede comparar.
3. **Archivos de datos y log:** nombre, tipo, filegroup, tamaño, máximo, crecimiento automático, estado
   y ruta.
4. **Objetos con mayor consumo:** las 20 tablas e índices que más espacio reservan. *Reservado* es lo
   que ocupa en el archivo y *usado*, lo que realmente tiene datos.
5. **Tamaño de las bases de datos:** todas las bases de la instancia, de la mayor a la menor.

## 5. Módulo 4 – Respaldos

Responde a la pregunta: *¿puedo recuperar cada base si falla ahora, y cuántos datos perdería?*

Abrir **Respaldos**. La portada indica cuántas bases están en riesgo. Debajo aparecen:

1. **Respaldos y restauraciones en curso:** solo aparece si hay uno ejecutándose, con su avance y el
   tiempo estimado que falta.
2. **Estado de los respaldos por base:** modelo de recuperación, último respaldo de cada tipo y nivel de
   riesgo. La última columna explica qué significa y qué hacer.

   | Riesgo | Condición |
   |---|---|
   | *Crítico* | La base nunca tuvo un respaldo completo. |
   | *Atención* | El completo tiene más de 7 días, o la base está en modelo FULL o BULK_LOGGED sin respaldos de log en las últimas 24 horas. |
   | *Adecuado* | Cualquier otro caso. |

3. **Historial de respaldos:** elegir la base en la lista para ver sus últimos 50 respaldos con tipo,
   fechas, duración, tamaño, estado, CHECKSUM, usuario y destino. Al pasar el cursor sobre el estado se
   ve su explicación.

**Limitación:** el estado puede ser *Exitoso*, *Dañado* o *Incompleto*, pero un respaldo que falló no
aparece. SQL Server solo registra en `msdb` los que terminan; los fallidos quedan en su log de errores.
Por eso conviene revisar la fecha del último respaldo: si es antigua, puede que estén fallando.

## 6. Módulo 5 – Auditoría

Responde a la pregunta: *¿quién puede entrar, qué puede hacer y hay algún objeto roto?*

Abrir **Auditoría**. La portada resume cuántos logins hay, cuántos son sysadmin y si hay objetos
inválidos. La pantalla tiene dos niveles, porque un **login** da entrada al servidor y un **usuario**
da entrada a una base.

**En el servidor:**

1. **Logins registrados:** tipo, estado, base predeterminada, fecha de creación y si es sysadmin. El
   campo **Buscar** filtra los logins y sus privilegios por nombre.
2. **Roles de servidor y miembros:** cada rol y quién pertenece a él.
3. **Privilegios por login:** permisos asignados directamente al login. Un login puede tener pocos
   permisos aquí y aun así tener control total si pertenece al rol `sysadmin`.

**En la base elegida** (lista **Base de datos**):

4. **Usuarios de la base:** tipo, login asociado, esquema y fecha de creación. *Sin login* significa
   que el usuario existe en la base, pero no puede conectarse.
5. **Roles:** los creados en la base y los roles fijos que tienen miembros.
6. **Privilegios:** permisos de cada usuario o rol y sobre qué recurso aplican. Un permiso asignado a un
   rol lo reciben todos sus miembros, y un `DENY` prevalece sobre un `GRANT`.
7. **Objetos con dependencias rotas:** vistas, procedimientos o funciones que hacen referencia a algo
   que no existe en la base, en rojo. Para buscar otros errores y actuar sobre ellos, usar el Módulo 6.

## 7. Módulo 6 – Mantenimiento

Responde a la pregunta: *¿qué estadísticas debo recalcular y qué objetos tienen errores?*

Abrir **Mantenimiento** y elegir la base en la lista **Base de datos**. Este módulo **ejecuta acciones**
sobre la base. El resultado de cada una aparece en un mensaje arriba, en verde o en rojo, con el objeto
y la hora. Mientras una acción está en curso, los demás botones quedan deshabilitados.

### Recalcular estadísticas

La tabla **Estadísticas de tablas** muestra primero las que nunca se calcularon y luego las que tienen
más filas modificadas.

1. Buscar las filas marcadas en rojo: *Nunca*, o un 20 % o más de filas modificadas. Su botón aparece
   resaltado.
2. Pulsar **Recalcular**. Se actualizan todas las estadísticas de esa tabla, por lo que pueden cambiar
   varias filas.
3. Revisar el mensaje de resultado. La lista se actualiza sola.

La acción lee toda la tabla (`WITH FULLSCAN`) y se ejecuta sin pedir confirmación, así que en una tabla
grande puede tardar y consumir recursos.

### Recompilar un objeto

La tabla **Objetos inválidos o con errores** lista los objetos a los que les falta una tabla u otro
objeto de la base. Pulsar **Recompilar** en la fila correspondiente.

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| *El objeto se recompiló y no tiene referencias rotas.* | Quedó bien. | Nada. |
| *Se recompiló, pero el objeto sigue inválido porque no existe: …* | Recompilar no repara el objeto: sigue faltando lo indicado. | Crear lo que falta o corregir el objeto con `ALTER`, y volver a recompilar. |
| *No se pudo recompilar el objeto. SQL Server respondió: …* | SQL Server encontró un error, por ejemplo una columna que ya no existe. | Corregir el objeto según el mensaje. |

### Validar objetos

La lista anterior no detecta columnas eliminadas, porque la tabla sigue existiendo. Para encontrarlas,
pulsar **Validar objetos**: se revisa cada vista, procedimiento y función de la base, y debajo aparecen
los que dieron error, con el mensaje de SQL Server (en el idioma de la instancia).

Esta acción no deja cambios en la base. Un objeto puede aparecer en las dos listas, y un procedimiento
al que le falta una tabla solo aparece en la de arriba. Por eso conviene revisar ambas.

## 8. Permisos que usa la aplicación

La aplicación se conecta con el login `dba_monitor`, que no es sysadmin. Dentro de cada base solo puede
ver metadatos y ejecutar las acciones de mantenimiento; no recibe permiso para leer los datos de las
tablas. El detalle está en `docs/arquitectura.md`.

## 9. Mensajes comunes y qué hacer

| Lo que aparece | Causa probable | Qué hacer |
|---|---|---|
| *No se pudo contactar al backend* o *Sin conexión con la API* | El backend no está en ejecución. | Iniciarlo (sección 1). |
| *No se pudo establecer conexión con SQL Server…* | El servicio está detenido o la URL de conexión es incorrecta. | Revisar el servicio y `application-local.properties`. |
| *Ocurrió un error al consultar SQL Server…* | Faltan permisos en esa base o en `msdb`. | Ejecutar los scripts `02`, `03` y `04`. El detalle queda en el log del backend. |
| *No hay bases de usuario en línea a las que la aplicación tenga acceso…* | El login no tiene usuario en ninguna base en línea. | Ejecutar los scripts de permisos sobre la base. |
| Una base no aparece en la lista | Está fuera de línea, es de sistema o la aplicación no tiene acceso. | Revisar su estado en el Módulo 1 y ejecutar los scripts de permisos sobre ella. |
| *… no es un identificador válido* | El nombre tiene caracteres no admitidos, como guiones o espacios. | Limitación actual: solo se admiten letras, números, `_`, `#` y `$`. |
| *No se pudieron recalcular las estadísticas…* | La tabla no existe o falta el permiso `ALTER`. | Ejecutar `02_permisos_mantenimiento.sql` sobre la base. |
| El crecimiento o el historial aparecen vacíos | La base no tiene respaldos registrados. | Hacer respaldos completos o ejecutar `05_generar_historial_respaldos.sql`. |
| Las consultas del Módulo 2 aparecen vacías | La caché de planes está vacía. | Ejecutar `06_generar_carga_rendimiento.sql` (sección 3). |
| Una pantalla tarda varios segundos o más | Puede faltar memoria libre en el equipo. | Cerrar programas y detener servicios que no se usen (ver `docs/modulo2-rendimiento.md`). |

## Anexo: uso directo de la API

La interfaz consume una API REST que también se puede consultar directamente. Las rutas `GET` se pueden
abrir en el navegador. `{base}` es el nombre de una base, por ejemplo `BASE_MONITOR`.

| Módulo | Qué devuelve | Ruta |
|---|---|---|
| General | Estado de la conexión | `GET /api/salud` |
| 1 | Resumen de la instancia | `GET /api/instancia` |
| 1 | Bases de datos | `GET /api/instancia/bases-de-datos` |
| 2 | Sentencias con mayor consumo de CPU | `GET /api/rendimiento/consultas-consumo` |
| 2 | Tiempo promedio por sentencia | `GET /api/rendimiento/tiempo-promedio` |
| 2 | Sesiones activas | `GET /api/rendimiento/sesiones` |
| 2 | Sesiones bloqueadas | `GET /api/rendimiento/bloqueos` |
| 2 | Procesos por CPU o memoria | `GET /api/rendimiento/procesos?orden=cpu` o `?orden=memoria` |
| 3 | Tamaño de todas las bases | `GET /api/almacenamiento/bases-de-datos` |
| 3 | Espacio por filegroup | `GET /api/almacenamiento/filegroups?baseDatos={base}` |
| 3 | Archivos de datos y log | `GET /api/almacenamiento/archivos?baseDatos={base}` |
| 3 | Objetos más grandes | `GET /api/almacenamiento/objetos?baseDatos={base}` |
| 3 | Historial de crecimiento | `GET /api/almacenamiento/crecimiento?baseDatos={base}` |
| 4 | Último respaldo y riesgo por base | `GET /api/respaldos` |
| 4 | Historial de una base | `GET /api/respaldos/historial?baseDatos={base}` |
| 4 | Respaldos en ejecución | `GET /api/respaldos/en-curso` |
| 5 | Logins | `GET /api/auditoria/logins` |
| 5 | Roles de servidor | `GET /api/auditoria/roles` |
| 5 | Privilegios por login | `GET /api/auditoria/privilegios` |
| 5 | Usuarios de una base | `GET /api/auditoria/usuarios-base?baseDatos={base}` |
| 5 | Roles de una base | `GET /api/auditoria/roles-base?baseDatos={base}` |
| 5 | Privilegios en una base | `GET /api/auditoria/privilegios-base?baseDatos={base}` |
| 5 | Objetos con dependencias rotas | `GET /api/auditoria/objetos-invalidos?baseDatos={base}` |
| 6 | Estado de las estadísticas | `GET /api/mantenimiento/estadisticas?baseDatos={base}` |
| 6 | Objetos por recompilar | `GET /api/mantenimiento/objetos-invalidos?baseDatos={base}` |
| 6 | Recalcular estadísticas | `POST /api/mantenimiento/estadisticas/recalcular` |
| 6 | Recompilar un objeto | `POST /api/mantenimiento/objetos/recompilar` |
| 6 | Validar los objetos de una base | `POST /api/mantenimiento/objetos/validar?baseDatos={base}` |

Las acciones de recalcular y recompilar reciben un cuerpo JSON. Por ejemplo, desde la terminal `cmd`:

```
curl -X POST http://localhost:8080/api/mantenimiento/estadisticas/recalcular ^
  -H "Content-Type: application/json" ^
  -d "{\"baseDatos\":\"BASE_MONITOR\",\"esquema\":\"ventas\",\"tabla\":\"Factura\"}"
```

Para recompilar, el cuerpo lleva `baseDatos`, `esquema` y `objeto`. Ambas acciones responden con este
formato; si `exito` es `false`, `mensaje` explica qué ocurrió:

```json
{ "exito": true, "mensaje": "Estadísticas recalculadas correctamente.",
  "objeto": "BASE_MONITOR.ventas.Factura", "fecha": "2026-11-01T22:18:00" }
```
