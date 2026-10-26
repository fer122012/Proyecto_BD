# DBA Monitor – Proyecto EIF402

Herramienta de monitoreo, administración y auditoría de SQL Server.
Proyecto Integrador, Administración de Bases de Datos, UNA, II Ciclo 2026.

## Integrantes
- Mariela Orozco Rayo 
- Paulina Porras Núñez
- Katherine Valverde Fallas
- Alex Quesada 

## Tecnologías
- SQL Server
- Backend: Java + Spring Boot
- Frontend: React

## Estructura del repositorio
- `backend/`: API en Spring Boot
- `frontend/`: interfaz en React
- `database/scripts/`: scripts de creación y permisos
- `database/queries/`: consultas administrativas documentadas
- `docs/`: documentación técnica, manual de usuario, pruebas y bitácora

## Resumen rápido
Para correr el proyecto desde cero, en este orden:
1. SQL Server con autenticación mixta ([paso 1](#1-sql-server)).
2. TCP/IP habilitado en el puerto 1433 ([paso 2](#2-tcpip-en-el-puerto-1433)).
3. Login `dba_monitor` creado ([paso 3](#3-login-de-la-aplicación)).
4. Java 17 y Node.js instalados ([paso 4](#4-herramientas)).
5. `application-local.properties` con la contraseña ([paso 5](#5-configuración-local-del-backend)).
6. Base de práctica `BASE_MONITOR`, sus permisos y su historial de respaldos ([Base de datos de práctica](#base-de-datos-de-práctica)).
7. Levantar el backend y luego el frontend ([Instalación y ejecución](#instalación-y-ejecución)).

> El proyecto solo usa **SQL Server**. No hace falta tener ningún servicio de Oracle encendido.

## Requisitos del entorno

### 1. SQL Server
- SQL Server **Developer** o **Express**. La instancia de desarrollo del grupo es Express,
  que **no incluye SQL Server Agent**.
- **Autenticación mixta** (SQL Server y Windows). En SSMS: clic derecho sobre el servidor →
  *Properties* → *Security* → *SQL Server and Windows Authentication mode*. Luego reiniciar
  el servicio de SQL Server.
- Si hay varias instancias instaladas (por ejemplo `MSSQLSERVER` y `SQLEXPRESS`), elegir **una**
  y usar esa misma en los pasos 2 y 3. Solo una instancia puede escuchar en el puerto 1433.
  Para ver las instancias y si tienen autenticación mixta (`solo_windows = 0`):

```
sqlcmd -S localhost\SQLEXPRESS -E -C -Q "SELECT @@SERVERNAME, SERVERPROPERTY('IsIntegratedSecurityOnly') AS solo_windows"
```

### 2. TCP/IP en el puerto 1433
1. Abrir **SQL Server Configuration Manager**.
2. Ir a *SQL Server Network Configuration* → *Protocols for <NOMBRE_INSTANCIA>*
   (en Express normalmente `SQLEXPRESS`).
3. Clic derecho sobre **TCP/IP** → *Enable*.
4. Abrir las propiedades de TCP/IP → pestaña *IP Addresses* → sección **IPAll**:
   - Dejar vacío *TCP Dynamic Ports*.
   - Poner `1433` en *TCP Port*.
5. Ir a *SQL Server Services* y **reiniciar el servicio** de SQL Server
   (los cambios no se aplican hasta reiniciar).

**Alternativa por consola** (equivale a los pasos anteriores). Abrir **cmd como administrador**
(Inicio → "cmd" → clic derecho → *Ejecutar como administrador*). `MSSQL17.SQLEXPRESS` cambia
según la versión e instancia; el nombre correcto se ve con:

```
reg query "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL"
```

Luego, reemplazando `MSSQL17.SQLEXPRESS` y `MSSQL$SQLEXPRESS` si la instancia es otra:

```
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp" /v Enabled /t REG_DWORD /d 1 /f
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp\IPAll" /v TcpPort /t REG_SZ /d 1433 /f
reg add "HKLM\SOFTWARE\Microsoft\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQLServer\SuperSocketNetLib\Tcp\IPAll" /v TcpDynamicPorts /t REG_SZ /d "" /f
net stop MSSQL$SQLEXPRESS
net start MSSQL$SQLEXPRESS
```

Para comprobar que ya escucha en el puerto 1433 (debe mostrar una línea `LISTENING`):

```
netstat -ano | findstr :1433
```

### 3. Login de la aplicación
Ejecutar `database/scripts/01_crear_login_monitor.sql` con un usuario administrador, conectado a
la instancia elegida en el paso 1 (en SSMS, o con `sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i <archivo>`).
Antes de ejecutarlo, reemplazar el placeholder `<CAMBIAR_CONTRASENA>` por una contraseña
segura **sin guardar ese cambio en el repositorio**. El script crea el login `dba_monitor`
y le otorga `VIEW SERVER STATE` y `VIEW ANY DEFINITION`.

Para cambiar la contraseña después:

```sql
ALTER LOGIN dba_monitor WITH PASSWORD = N'<NUEVA_CONTRASENA>';
```

Los permisos que necesita cada módulo sobre la base de práctica (scripts 02 y 03) se explican en
[Base de datos de práctica](#base-de-datos-de-práctica).

### 4. Herramientas
- **Java 17** (JDK)
- **Node.js** (versión LTS) con npm

### 5. Configuración local del backend
Las credenciales nunca se suben a Git. En `backend/src/main/resources/`:
1. Copiar `application-example.properties` y renombrar la copia como
   `application-local.properties` (este archivo está en `.gitignore`).
2. Completar los valores reales en la copia local. La contraseña es la que se usó en el paso 3.
   Si no se crea este archivo, el backend no arranca y muestra
   `Failed to configure a DataSource: 'url' attribute is not specified`.

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true
spring.datasource.username=dba_monitor
spring.datasource.password=<CAMBIAR_CONTRASENA>
```

> **Nota sobre la conexión:** la URL usa `encrypt=true;trustServerCertificate=true`.
> La conexión va cifrada, pero se acepta el certificado autofirmado de la instancia local
> sin validarlo. Es adecuado para desarrollo, no para producción.

## Base de datos de práctica
El grupo usa **`BASE_MONITOR`**, una base propia que simula los sistemas de recursos humanos,
ventas e inventario de la empresa del caso de estudio. Está pensada para que cada módulo tenga
algo real que mostrar:

| Contenido | Módulo |
|---|---|
| Esquemas `rrhh`, `ventas` e `inventario` con unas 260 000 filas | 3 y 6 |
| Filegroups `PRIMARY`, `FG_DATOS` y `FG_HISTORICO` (este último de tamaño fijo y casi lleno: estado CRITICO) | 3 |
| Modelo de recuperación FULL e historial de respaldos completos, diferenciales y de log | 3 (crecimiento) |
| Una vista y dos procedimientos con dependencias rotas | 5 y 6 |
| Estadísticas con muchas filas modificadas desde su última actualización | 6 |
| Roles de base de datos y usuarios sin login (sin contraseñas) | 5 |

Ejecutar los scripts **en este orden**, con un usuario administrador y desde la raíz del
repositorio. `-f 65001` hace que `sqlcmd` lea los archivos como UTF-8 (sin eso, las tildes quedan
mal guardadas). Si la instancia es otra, cambiar `localhost\SQLEXPRESS`.

```
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\00_crear_base_monitor.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\02_permisos_mantenimiento.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\03_permisos_almacenamiento.sql
sqlcmd -S localhost\SQLEXPRESS -E -C -f 65001 -i database\scripts\05_generar_historial_respaldos.sql
```

| Script | Qué hace |
|---|---|
| `00_crear_base_monitor.sql` | Crea `BASE_MONITOR` con datos de ejemplo. **Si ya existe, la elimina y la vuelve a crear** (también borra su historial de respaldos). |
| `02_permisos_mantenimiento.sql` | Usuario `dba_monitor` en la base con el rol `db_ddladmin`, para recalcular estadísticas y recompilar (Módulo 6). |
| `03_permisos_almacenamiento.sql` | Usuario `dba_monitor` en la base y en `msdb`, con lectura del historial de respaldos (Módulo 3). |
| `05_generar_historial_respaldos.sql` | Hace respaldos reales (completo, diferencial y de log) con ventas nuevas entre ellos, para que haya historial de crecimiento. Se puede repetir en días distintos para obtener un promedio diario. |

Los respaldos se guardan en la carpeta de respaldos predeterminada de la instancia (por ejemplo
`C:\Program Files\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQL\Backup`) y ocupan unos 250 MB por
cada ejecución del script 05.

## Instalación y ejecución

### Backend
```
cd backend
./mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux/macOS
```
Queda escuchando en `http://localhost:8080`. Para comprobarlo: `GET http://localhost:8080/api/salud`.

**Desde IntelliJ IDEA:**
1. Abrir la carpeta del repositorio y, si lo pide, cargar `backend/pom.xml` como proyecto Maven
   (o en la pestaña *Maven* → **+** → elegir `backend/pom.xml`).
2. En la pestaña *Maven*, pulsar **Reload All Maven Projects**.
3. Ejecutar `DbamonitorApplication`. En *Run → Edit Configurations…* el campo
   **Use classpath of module** debe ser `dbamonitor` (el módulo Maven).

### Frontend
```
cd frontend
npm install
npm run dev
```
Abrir `http://localhost:5173`. El backend debe estar en ejecución; si no responde, cada pantalla
muestra el error y un botón para reintentar.

Por defecto el frontend consulta la API en `http://localhost:8080`. Para usar otra dirección,
copiar `frontend/.env.example` como `frontend/.env.local` y cambiar `VITE_API_URL`.

### Estructura del frontend
- `src/api/`: cliente HTTP y rutas de la API por módulo.
- `src/hooks/useConsulta.js`: estado de cada consulta (cargando, error, datos, recargar).
- `src/components/`: barra de navegación, portada, tablas, indicadores y mensajes de estado.
- `src/modulos/<modulo>/`: una página por módulo (`instancia`, `almacenamiento`, `auditoria`, `mantenimiento`).
- `src/index.css`: tema visual "Celosía" (grafito y dorado); los colores están en variables al inicio.

Los módulos 2 y 4 aparecen en la navegación como "pronto" hasta que tengan endpoints.

## Solución de problemas

| Error | Causa | Solución |
|---|---|---|
| `Failed to configure a DataSource: 'url' attribute is not specified` | Falta `application-local.properties`. | Crearlo como indica el [paso 5](#5-configuración-local-del-backend). |
| `Login failed for user 'dba_monitor'` | No existe el login, la contraseña no coincide o la instancia solo acepta autenticación de Windows. | Revisar los pasos [1](#1-sql-server) y [3](#3-login-de-la-aplicación) en la misma instancia. |
| `The TCP/IP connection to the host localhost, port 1433 has failed` o tiempo de espera agotado | La instancia no escucha por TCP en el puerto 1433. | Hacer el [paso 2](#2-tcpip-en-el-puerto-1433) y reiniciar el servicio. |
| `java: package org.springframework.web.servlet.config.annotation does not exist` (solo en IntelliJ) | IntelliJ compila con un módulo que no es el de Maven, o tiene librerías desactualizadas. Con `mvnw.cmd clean compile` sí compila. | Seguir los pasos de *Desde IntelliJ IDEA*. Si persiste: *File → Invalidate Caches… → Invalidate and Restart*. |
| `"$base" no se reconoce como un comando interno o externo` | Se pegaron comandos de PowerShell en cmd. | Usar los comandos `reg add` del paso 2, que son para cmd. |
| Dependencias de Maven corruptas | Descarga incompleta en `~/.m2`. | Desde `backend`: `.\mvnw.cmd -U clean compile`. |
