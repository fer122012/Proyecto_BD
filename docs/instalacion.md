# Instalación detallada y solución de problemas

Complemento del [README](../README.md). Aquí están los pasos con más detalle, las alternativas
por consola y los errores comunes.

## SQL Server

- Sirve **Developer** o **Express**. La instancia del grupo es Express, que **no incluye SQL
  Server Agent**.
- **Autenticación mixta**: en SSMS, clic derecho sobre el servidor → *Properties* → *Security* →
  *SQL Server and Windows Authentication mode*. Luego reiniciar el servicio.
- Si hay varias instancias (por ejemplo `MSSQLSERVER` y `SQLEXPRESS`), elegir **una** y usar esa
  en todos los pasos. Solo una puede escuchar en el puerto 1433. Para ver si una instancia tiene
  autenticación mixta (`solo_windows = 0`):

```
sqlcmd -S localhost\SQLEXPRESS -E -C -Q "SELECT @@SERVERNAME, SERVERPROPERTY('IsIntegratedSecurityOnly') AS solo_windows"
```

## TCP/IP en el puerto 1433

Con **SQL Server Configuration Manager**:
1. *SQL Server Network Configuration* → *Protocols for SQLEXPRESS*.
2. Clic derecho sobre **TCP/IP** → *Enable*.
3. Propiedades de TCP/IP → pestaña *IP Addresses* → sección **IPAll**: dejar vacío
   *TCP Dynamic Ports* y poner `1433` en *TCP Port*.
4. *SQL Server Services* → reiniciar el servicio de SQL Server.

**Alternativa por consola** (cmd **como administrador**). `MSSQL17.SQLEXPRESS` cambia según la
versión e instancia; el nombre correcto se ve con
`reg query "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL"`.

```
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp" /v Enabled /t REG_DWORD /d 1 /f
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp\IPAll" /v TcpPort /t REG_SZ /d 1433 /f
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp\IPAll" /v TcpDynamicPorts /t REG_SZ /d "" /f
net stop MSSQL$SQLEXPRESS
net start MSSQL$SQLEXPRESS
```

Para comprobarlo: `netstat -ano | findstr :1433` debe mostrar una línea `LISTENING`.

## Login de la aplicación

`database/scripts/01_crear_login_monitor.sql` crea el login `dba_monitor` y le da
`VIEW SERVER STATE` y `VIEW ANY DEFINITION`. Antes de ejecutarlo hay que cambiar
`<CAMBIAR_CONTRASENA>` por una contraseña, **sin guardar ese cambio en el repositorio**.
Para cambiarla después:

```sql
ALTER LOGIN dba_monitor WITH PASSWORD = N'<NUEVA_CONTRASENA>';
```

## Base de práctica BASE_MONITOR

Simula los sistemas de recursos humanos, ventas e inventario de la empresa del caso de estudio,
para que cada módulo tenga algo real que mostrar:

| Contenido | Módulo |
|---|---|
| Esquemas `rrhh`, `ventas` e `inventario` con unas 260 000 filas | 3 y 6 |
| Filegroups `PRIMARY`, `FG_DATOS` y `FG_HISTORICO` (este último de tamaño fijo y casi lleno: estado CRITICO) | 3 |
| Modelo de recuperación FULL e historial de respaldos completos, diferenciales y de log | 3 |
| Una vista y dos procedimientos con dependencias rotas | 5 y 6 |
| Estadísticas desactualizadas y otras que nunca se calcularon | 6 |
| Roles de base de datos y usuarios sin login (sin contraseñas) | 5 |

| Script | Qué hace |
|---|---|
| `00_crear_base_monitor.sql` | Crea la base con datos de ejemplo. **Si ya existe, la borra y la vuelve a crear** (también su historial de respaldos). |
| `02_permisos_mantenimiento.sql` | Usuario `dba_monitor` en la base, con lectura de las dependencias (Módulos 5 y 6) y el rol `db_ddladmin` para recalcular y recompilar (Módulo 6). |
| `03_permisos_almacenamiento.sql` | Usuario `dba_monitor` en la base y en `msdb`, con lectura del historial de respaldos (Módulo 3). |
| `05_generar_historial_respaldos.sql` | Hace respaldos reales con ventas nuevas entre ellos. Ejecutarlo en días distintos da un promedio diario de crecimiento. Cada ejecución ocupa unos 250 MB en la carpeta `Backup` de la instancia. |

`-f 65001` en `sqlcmd` hace que lea los archivos como UTF-8; sin eso las tildes quedan mal
guardadas.

## Backend desde IntelliJ IDEA

1. Abrir la carpeta del repositorio y cargar `backend/pom.xml` como proyecto Maven
   (pestaña *Maven* → **+** → `backend/pom.xml`).
2. En la pestaña *Maven*, pulsar **Reload All Maven Projects**.
3. Ejecutar `DbamonitorApplication`. En *Run → Edit Configurations…*, **Use classpath of module**
   debe ser `dbamonitor`.

Las consultas SQL se cargan al arrancar: después de cambiar un `.sql` o el código, hay que
**reiniciar el backend**.

## Frontend

Por defecto consulta la API en `http://localhost:8080`. Para otra dirección, copiar
`frontend/.env.example` como `frontend/.env.local` y cambiar `VITE_API_URL`.

Estructura:
- `src/api/`: cliente HTTP y rutas de la API por módulo.
- `src/hooks/useConsulta.js`: estado de cada consulta (cargando, error, datos, recargar).
- `src/components/`: navegación, portada, tablas, indicadores y mensajes de estado.
- `src/modulos/<modulo>/`: una página por módulo.
- `src/index.css`: tema visual; los colores están en variables al inicio.

## Solución de problemas

| Error | Causa | Solución |
|---|---|---|
| `Failed to configure a DataSource: 'url' attribute is not specified` | Falta `application-local.properties`. | Crearlo a partir de `application-example.properties`. |
| `Login failed for user 'dba_monitor'` | No existe el login, la contraseña no coincide o la instancia solo acepta autenticación de Windows. | Revisar autenticación mixta y el script 01 en la misma instancia. |
| `The TCP/IP connection to the host localhost, port 1433 has failed` | La instancia no escucha por TCP en el 1433. | Habilitar TCP/IP y reiniciar el servicio. |
| "Ocurrió un error al consultar SQL Server" en un módulo | Faltan permisos en la base de práctica. | Volver a ejecutar los scripts 02 y 03. |
| Un cambio no se refleja en la pantalla | El backend sigue con el código anterior. | Reiniciar el backend. |
| `java: package org.springframework... does not exist` (solo IntelliJ) | IntelliJ no usa el módulo Maven o tiene librerías viejas. | Pasos de *Backend desde IntelliJ*; si persiste, *File → Invalidate Caches → Invalidate and Restart*. |
| `"$base" no se reconoce como un comando interno o externo` | Se pegaron comandos de PowerShell en cmd. | Usar los comandos `reg add` de arriba. |
| Dependencias de Maven corruptas | Descarga incompleta en `~/.m2`. | Desde `backend`: `.\mvnw.cmd -U clean compile`. |
