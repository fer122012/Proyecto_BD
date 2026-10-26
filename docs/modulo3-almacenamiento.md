# Módulo 3 – Gestión del almacenamiento

Supervisa el espacio físico asignado a las bases de datos y ayuda a detectar problemas por el
crecimiento del almacenamiento: qué tan llenos están los filegroups, cuánto ocupa cada base y cada
archivo, qué objetos consumen más espacio y cómo ha crecido la base en el tiempo.

- Consultas: `database/queries/modulo3_almacenamiento/` (01 a 05)
- Código: paquete `cr.ac.una.dbamonitor.almacenamiento` (controller, service, repository y `dto/`)
- Permisos: `database/scripts/03_permisos_almacenamiento.sql`
- Pantalla: `frontend/src/modulos/almacenamiento/`

## Cobertura del enunciado

| Funcionalidad del enunciado | Endpoint | Consulta |
|---|---|---|
| Espacio utilizado y disponible en filegroups | `GET /api/almacenamiento/filegroups?baseDatos=X` | `02_espacio_filegroups.sql` |
| Tamaño actual de la base de datos | `GET /api/almacenamiento/bases-de-datos` | `01_tamano_bases_datos.sql` |
| Tamaño de los archivos de datos | `GET /api/almacenamiento/archivos?baseDatos=X` | `03_archivos_datos.sql` |
| Objetos con mayor consumo de almacenamiento | `GET /api/almacenamiento/objetos?baseDatos=X` | `04_objetos_mayor_tamano.sql` |
| Crecimiento de la base de datos | `GET /api/almacenamiento/crecimiento?baseDatos=X` | `05_crecimiento_base_datos.sql` |

El tamaño de las bases se consulta para toda la instancia. Las demás rutas reciben `baseDatos`
porque los filegroups, archivos y objetos son propios de cada base y la conexión del backend
apunta a `master`. Igual que en el Módulo 6, el nombre se valida con `SqlIdentificadores` antes de
insertarlo en la consulta (un nombre con caracteres no permitidos devuelve un 400).

## Indicadores

| Indicador (campo JSON) | Fuente | Cómo interpretarlo |
|---|---|---|
| Tamaño de datos, log y total (`tamanoDatosMb`, `tamanoLogMb`, `tamanoTotalMb`) | `sys.master_files.size` (páginas de 8 KB) | Espacio que ocupan en disco los archivos de la base, tengan datos o no. Es el "tamaño actual" de la base. |
| Tamaño del filegroup (`tamanoMb`) | Suma de `sys.database_files.size` de sus archivos | Espacio asignado al filegroup. |
| Usado (`usadoMb`) | Suma de `sys.allocation_units.total_pages` del filegroup | Páginas reservadas por tablas e índices. Ya no están disponibles para datos nuevos. |
| Disponible y % de uso (`disponibleMb`, `porcentajeUso`) | Calculado en el backend | Tamaño − usado, y usado / tamaño × 100. |
| Estado (`estado`, `interpretacion`) | Calculado en el backend | `NORMAL` por debajo de 85 %. `ATENCION` si supera 85 % pero algún archivo puede crecer solo. `CRITICO` si supera 85 % y ningún archivo puede crecer: al llenarse, las inserciones fallan con el error 1105. |
| Archivos (`/archivos`) | `sys.database_files` + `sys.filegroups` | Nombre lógico, tipo (datos o log), filegroup, ruta física, estado, tamaño, máximo (`Ilimitado` si `max_size = -1`) y crecimiento automático (`8 MB`, `10 %` o `Sin crecimiento automático`). |
| Objetos de mayor tamaño (`/objetos`) | `sys.partitions` + `sys.allocation_units` + `sys.indexes` | Las 20 tablas e índices que más espacio reservan, con filas, espacio reservado y usado. Cada fila es un índice (o el `HEAP` de una tabla sin índice clúster), porque el espacio se asigna por índice. |
| Crecimiento (`/crecimiento`) | `msdb.dbo.backupset` + `msdb.dbo.backupfile` | Tamaño asignado y usado de la base en cada uno de los últimos 30 respaldos completos, la variación entre ellos, el crecimiento total y el promedio diario. |

### Por qué el crecimiento sale de los respaldos

SQL Server no guarda un historial del tamaño de las bases. Lo que sí guarda es el historial de
respaldos en `msdb`, y cada respaldo registra el tamaño que tenía cada archivo (`backupfile.file_size`)
y cuántos datos contenía (`backupset.backup_size`). Con los respaldos completos se obtiene una serie
**real** del tamaño de la base, sin crear tablas ni jobs adicionales (la instancia Express no tiene
SQL Server Agent). El enunciado acepta "información histórica real o un mecanismo documentado para
registrar y comparar el crecimiento".

Si la base nunca se respaldó, la pantalla lo explica en lugar de mostrar un gráfico vacío. Para
generar historial basta con hacer respaldos completos de vez en cuando (ver Módulo 4):

```sql
BACKUP DATABASE [MiBase] TO DISK = N'C:\Respaldos\MiBase.bak' WITH INIT;
```

### Por qué las consultas usan `{BASE}.sys.schemas` y no `SCHEMA_NAME()`

Las consultas por base usan nombres de tres partes (`[MiBase].sys.objects`) porque la conexión
sigue en `master`. Funciones como `SCHEMA_NAME()` u `OBJECT_NAME()` (sin el parámetro de base)
resuelven el nombre en la base **actual** de la conexión (`master`) y devolverían nombres
equivocados o `NULL`. Por eso el esquema se obtiene uniendo `{BASE}.sys.schemas`.

## Permisos que necesita este módulo

| Consulta | Permiso |
|---|---|
| Tamaño de las bases (`sys.master_files`) | `VIEW ANY DEFINITION`, ya otorgado por `01_crear_login_monitor.sql`. |
| Filegroups, archivos y objetos (`{BASE}.sys.*`) | Un usuario `dba_monitor` dentro de la base, sin roles. `VIEW ANY DEFINITION` ya le deja ver los metadatos. |
| Crecimiento (`msdb.dbo.backupset`, `backupfile`) | Un usuario en `msdb` con `SELECT` solo sobre esas dos tablas. |

`database/scripts/03_permisos_almacenamiento.sql` crea ambos usuarios para la base de práctica
`BASE_MONITOR`. Para analizar otra base, cambiar el `USE` y ejecutarlo una vez por cada base. Si la base ya tiene
el usuario por el script 02 (Módulo 6), esa parte no hace nada. Sin estos permisos, las rutas por
base responden con un 500 y el mensaje de permisos insuficientes.

## Preguntas probables en la defensa

**¿Qué es un filegroup y en qué se parece a un tablespace?**
Es un contenedor lógico de archivos de datos. Cada tabla o índice se crea en un filegroup y sus
páginas se reparten entre los archivos de ese filegroup. Cumple el mismo papel que un tablespace en
Oracle. Toda base tiene el filegroup `PRIMARY`, y se pueden crear otros para separar datos (por
ejemplo, históricos en otro disco).

**¿Por qué el log de transacciones no aparece en los filegroups?**
Porque el log no pertenece a ningún filegroup: guarda registros de transacciones, no páginas de
tablas. Se muestra en la tabla de archivos con tipo `Log`.

**¿Cuál es la diferencia entre espacio reservado y usado?**
SQL Server asigna espacio en extensiones de 8 páginas. Reservado es todo lo asignado al objeto;
usado son las páginas que realmente tienen datos. La diferencia es espacio asignado pero vacío
dentro del objeto, y se recupera reconstruyendo el índice.

**¿Un filegroup al 95 % es un problema?**
Depende de si puede crecer. Si algún archivo tiene crecimiento automático y no llegó a su máximo,
SQL Server lo agranda solo cuando haga falta (estado `ATENCION`: hay que vigilar el disco). Si ningún
archivo puede crecer, al llenarse las inserciones fallan con el error 1105 (estado `CRITICO`).

**¿Por qué no conviene un crecimiento automático en porcentaje?**
Porque cada crecimiento es más grande que el anterior y, en bases grandes, puede tardar y bloquear
la base mientras se reserva el espacio. Se recomienda un crecimiento fijo en MB.

**¿Cómo se mide el crecimiento si SQL Server no guarda historial de tamaño?**
Con el tamaño que cada respaldo completo registra en `msdb`. Es información real que el motor
guarda por su cuenta, y no requiere SQL Server Agent.

**¿Qué limitación tiene SQL Server Express en este módulo?**
Cada base puede tener como máximo 10 GB de datos (el log no cuenta). Si una base de usuario se
acerca a ese tamaño, ya no puede crecer aunque haya espacio en el disco.
