# Bitácora del proyecto

Registro de seguimiento de DBA Monitor (EIF402, II Ciclo 2026).

> **Borrador.** Las secciones 1 y 2 salen del historial de Git de la rama `main`. Las secciones
> marcadas *Por completar* dependen del grupo: Git no registra reuniones, acuerdos ni el reparto
> de tareas, así que solo deben llenarse con lo que realmente ocurrió.
> Quiten esta nota cuando la bitácora esté completa.

## 1. Avance por fecha

| Fecha | Responsable | Trabajo realizado |
|---|---|---|
| 2026-09-28 | Mariela Orozco | Creación del repositorio, estructura inicial, `.gitignore` y README. |
| 2026-09-29 | Katherine Valverde | Módulo 2: consultas de mayor consumo, tiempo promedio y sesiones activas. |
| 2026-10-01 | Katherine Valverde | Módulo 2: consulta de sesiones bloqueadas y su sesión bloqueadora. |
| 2026-10-03 | Katherine Valverde | Módulo 2: script para provocar un bloqueo y validar la consulta. |
| 2026-10-03 | Mariela Orozco | Limpieza del repositorio, script del login `dba_monitor`, esqueleto del backend (paquetes por módulo, manejo global de errores, CORS y `/api/salud`) y Módulo 1 (consultas, endpoints y documentación). |
| 2026-10-11 | Paulina Porras | Módulo 5: consultas de auditoría. Módulo 6 y manual de usuario. |
| 2026-10-19 | Katherine Valverde | Módulo 2: consulta de procesos con mayor consumo de CPU y memoria. |
| 2026-10-25 | Alex Quesada | Frontend de los módulos 2, 5 y 6. |
| 2026-10-26 | Alex Quesada | Módulo 3 completo, base de práctica `BASE_MONITOR` con historial de respaldos, Módulo 4, correcciones de los módulos 5 y 6, y simplificación del README. |
| 2026-10-26 | Katherine Valverde | Módulo 2: repository, service, controller, pruebas unitarias, pantalla, script de carga y documentación. |
| 2026-10-27 | Alex Quesada | Reversión de un commit del Módulo 4 e integración del frontend del Módulo 2. |
| 2026-11-01 | Mariela Orozco | Selectores según el acceso del login, corrección de objetos inválidos, usuarios, roles y privilegios por base (Módulo 5), verificación de la recompilación y validación de objetos (Módulo 6), reescritura del manual de usuario y documento de arquitectura. |

## 2. Incidencias registradas

| Fecha | Incidencia | Atención |
|---|---|---|
| 2026-10-25 | Una contraseña quedó escrita en `database/scripts/01_crear_login_monitor.sql`. | El 2026-10-26 se sustituyó por un placeholder. Sigue visible en commits antiguos del historial. *Por completar: confirmar que se cambió en todo equipo donde se usó.* |
| 2026-10-27 | Un commit del Módulo 4 tuvo que revertirse. | *Por completar: motivo de la reversión (Alex).* |

## 3. Distribución de responsabilidades

*Por completar: confirmar o corregir. Esta tabla se dedujo de los commits y no refleja trabajo que
no haya pasado por Git.*

| Integrante | Usuario en Git | Aportes visibles en el historial |
|---|---|---|
| Mariela Orozco | `fer122012` | Estructura del repositorio, base del backend, Módulo 1, ajustes finales de los módulos 5 y 6, documentación. |
| Katherine Valverde | `kathe valverde`, `Katherine Valverde` | Módulo 2: consultas, API, pantalla, pruebas, script de carga y documentación. |
| Paulina Porras | `paulinaporras` | Primera versión de los módulos 5 y 6 y del manual de usuario. |
| Alex Quesada | `Alex Quesada` | Módulos 3 y 4, base de práctica, frontend de los módulos 2, 5 y 6. |

## 4. Reuniones y acuerdos

*Por completar con las reuniones que hayan ocurrido.*

| Fecha | Participantes | Acuerdos |
|---|---|---|
|  |  |  |

## 5. Seguimientos con el docente

*Por completar. El enunciado menciona un primer seguimiento.*

| Fecha | Observaciones recibidas | Acciones tomadas |
|---|---|---|
|  |  |  |

## 6. Trabajo final antes de la entrega

| Tarea | Responsable | Estado |
|---|---|---|
| Completar las evidencias de pruebas (`docs/evidencias-pruebas.md`) |  | Pendiente |
| Agregar la presentación al repositorio |  | Pendiente |
| Ensayar la demostración con los seis módulos |  | Pendiente |
| Crear la etiqueta de la versión final en `main` |  | Pendiente |
| Confirmar el enlace en el Aula Virtual y el acceso del docente |  | Pendiente |
