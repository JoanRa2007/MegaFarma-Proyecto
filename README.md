# Megafarma · Proyecto

Sistema de ventas e inventario de la botica **Megafarma**. Proyecto del curso Desarrollo de Aplicaciones Móviles (UPeU, Ingeniería de Sistemas, ciclo VI).

| Carpeta | Parte | Contenido |
|---|---|---|
| [`megafarma-backend/`](megafarma-backend) | Backend | API REST en Spring Boot 4 (Java 21) con JWT, PostgreSQL, Flyway, Swagger y 71 pruebas automáticas |
| [`megafarma-app/`](megafarma-app) | Frontend (app + web) | Kotlin Multiplatform + Compose Multiplatform: un solo código que genera la app Android, la página web (Kotlin/Wasm) y la app iOS |

```
App Android ─┐
Página web  ─┼── JSON/HTTP + token JWT ──▶ Backend (Spring Boot, :8080) ──▶ PostgreSQL
App iOS     ─┘
```

- Backend: cómo ejecutarlo y probarlo en su [README](megafarma-backend/README.md); para entender el código, la [guía de estudio](megafarma-backend/GUIA_ESTUDIO.md).
- Frontend: arquitectura, pantallas y cómo ejecutarlo en su [README](megafarma-app/README.md).

## Ramas

| Rama | Para qué |
|---|---|
| `main` | Versión estable, la que se presenta |
| `develop` | Integración del trabajo terminado |
| `feature/<módulo>` | Una rama por módulo; al terminar se une a `develop` |

Backend: `configuracion-base`, `modelo-datos`, `seguridad-jwt`, `catalogo`, `inventario`, `ventas`, `reportes`, `pruebas` y `documentacion`.

Frontend: `frontend-app-web`.
