# Megafarma · Proyecto

Sistema de ventas e inventario de la botica **Megafarma**. Proyecto del curso Desarrollo de Aplicaciones Móviles (UPeU, Ingeniería de Sistemas, ciclo VI).

| Carpeta | Contenido |
|---|---|
| [`megafarma-backend/`](megafarma-backend) | API REST en Spring Boot 4 (Java 21) con JWT, PostgreSQL, Flyway, Swagger y 71 pruebas automáticas |

Para ejecutar y probar el backend, ver su [README](megafarma-backend/README.md). Para entender el código, empezar por la [guía de estudio](megafarma-backend/GUIA_ESTUDIO.md).

## Ramas

| Rama | Para qué |
|---|---|
| `main` | Versión estable, la que se presenta |
| `develop` | Integración del trabajo terminado |
| `feature/<módulo>` | Una rama por módulo; al terminar se une a `develop` |

Módulos: `configuracion-base`, `modelo-datos`, `seguridad-jwt`, `catalogo`, `inventario`, `ventas`, `reportes`, `pruebas` y `documentacion`.
