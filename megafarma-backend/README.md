# Megafarma · Backend

API REST del sistema de ventas e inventario de la botica **Megafarma**. Es el servidor que consume la app móvil (Kotlin Multiplatform + Ktor Client).

- **Spring Boot 4 · Java 21 · Maven**
- **Spring Security + JWT** con roles y permisos
- **Spring Data JPA** sobre **PostgreSQL**, esquema versionado con **Flyway**
- **Swagger / OpenAPI** para documentar y probar
- **71 pruebas automáticas** (JUnit 5, Mockito, MockMvc)

## Arquitectura

```
App móvil (Ktor Client) ── JSON/HTTP + JWT ──▶ Controller ─▶ Service ─▶ Repository ─▶ PostgreSQL
                                                   │            │            │
                                                  DTO     reglas de       Entity (JPA)
                                                          negocio
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controladores | `controller` | Reciben la petición HTTP, validan el DTO (`@Valid`) y devuelven JSON |
| Servicios | `service` (interfaces) y `service.impl` | Reglas de negocio y transacciones |
| Repositorios | `repository` | Acceso a datos con Spring Data JPA |
| Entidades | `entity` | Tablas: Usuario, Rol, Categoria, Producto, Lote, Cliente, Venta, DetalleVenta, MovimientoInventario |
| DTO y mappers | `dto`, `mapper` | Lo que entra y sale de la API; las entidades nunca se exponen |
| Seguridad | `security`, `config` | Filtro JWT, permisos por rol, CORS, Swagger |
| Errores | `exception` | Un solo formato de error para toda la API |

El esquema de la base lo crea Flyway (`src/main/resources/db/migration`); Hibernate solo lo valida (`ddl-auto: validate`).

## Cómo ejecutarlo

Requisito: **Java 21**. Maven no hace falta: el proyecto trae su wrapper (`mvnw`).

### Opción A · PostgreSQL con Docker (perfil `dev`, por defecto)

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

PostgreSQL queda en el puerto **5433** (base, usuario y clave: `megafarma`).

### Opción B · Sin instalar nada (perfil `local`, base H2 en archivo)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Guarda los datos en la carpeta `data/`. Para empezar de cero, bórrela.

En Windows (PowerShell o CMD) use `.\mvnw.cmd` en lugar de `./mvnw`.

### Direcciones

| Qué | Dirección |
|---|---|
| Swagger (documentación y pruebas) | http://localhost:8080/swagger-ui.html |
| Estado del servicio | http://localhost:8080/api/health |
| Desde el emulador de Android | `http://10.0.2.2:8080` |
| Desde un celular en la misma red | `http://<IP de la laptop>:8080` |

### Usuarios de prueba

Se crean solos la primera vez, junto con 20 productos y lotes de demostración.

| Usuario | Contraseña | Rol | Puede |
|---|---|---|---|
| `admin` | `Admin123` | ADMIN | Todo: además anula ventas, ve reportes y administra usuarios |
| `vendedor` | `Venta123` | VENDEDOR | Vender, gestionar clientes, consultar catálogo, stock y alertas |
| `almacen` | `Almacen123` | ALMACENERO | Catálogo, entradas por lote, ajustes, bajas y kardex |

Son solo para desarrollo. En producción se desactivan con `app.datos-demo=false` y la clave del JWT se entrega en la variable de entorno `APP_JWT_SECRET`.

## Pruebas

```bash
./mvnw test
```

| Archivo | Tipo | Qué comprueba |
|---|---|---|
| `VentaServiceImplTest` (14) | Unitaria (Mockito) | FEFO, reparto entre lotes, stock insuficiente, precio del servidor, idempotencia, anulación |
| `InventarioServiceImplTest` (14) | Unitaria (Mockito) | Entradas por lote, lote repetido, vencimientos, ajustes, bajas, cálculo de stock |
| `ProductoServiceImplTest` (7) | Unitaria (Mockito) | Registro, código duplicado, actualización, borrado lógico |
| `JwtServiceTest` (4) | Unitaria | Token válido, firma ajena, token alterado, token expirado |
| `PermisosPorRolTest` (4) | Unitaria | Matriz de permisos de cada rol |
| `UtilidadesTest` (6) | Unitaria | Paginación segura y rangos de fechas |
| `ApiIntegrationTest` (21) | Integración (MockMvc + H2) | La API completa de punta a punta: login, 401/403, CRUD, validaciones, flujo de inventario y venta, alertas y reportes |
| `MegafarmaBackendApplicationTests` (1) | Integración | El contexto arranca y Hibernate valida el esquema de Flyway |

Las pruebas usan una base H2 en memoria con las mismas migraciones; no necesitan Docker.

Para probar a mano: Swagger (botón **Authorize** con el token del login) o el archivo [`pruebas.http`](pruebas.http), que se ejecuta desde IntelliJ IDEA.

## Endpoints

Todas las rutas, salvo el login y el estado, exigen la cabecera `Authorization: Bearer <token>`.

| Método y ruta | Historia | Para qué sirve | Rol |
|---|---|---|---|
| `POST /api/v1/auth/login` | HU-21 | Iniciar sesión; devuelve el JWT, el rol y los permisos | Público |
| `GET /api/v1/auth/me` | HU-21 | Datos del usuario del token | Todos |
| `GET /api/v1/productos?q=` | HU-03 | Buscar por código, nombre o principio activo (paginado) | Todos |
| `GET /api/v1/productos/{id}` | — | Detalle de un producto | Todos |
| `GET /api/v1/productos/cambios?desde=` | — | Productos modificados desde una fecha (sincronización) | Todos |
| `POST /api/v1/productos` | HU-01 | Registrar un producto | Admin, Almacenero |
| `PUT /api/v1/productos/{id}` | HU-02 | Actualizar un producto | Admin, Almacenero |
| `DELETE /api/v1/productos/{id}` | HU-04 | Desactivar un producto (borrado lógico) | Admin, Almacenero |
| `PATCH /api/v1/productos/{id}/activar` | — | Reactivar un producto | Admin, Almacenero |
| `GET·POST·PUT·DELETE /api/v1/categorias` | — | CRUD de categorías | Leer: todos · Escribir: Admin, Almacenero |
| `GET·POST·PUT·DELETE /api/v1/clientes` | — | CRUD de clientes | Admin, Vendedor |
| `POST /api/v1/inventario/entradas` | HU-05 | Registrar una entrada por lote | Admin, Almacenero |
| `GET /api/v1/inventario/stock` | HU-06 | Stock de todos los productos | Todos |
| `GET /api/v1/inventario/stock/{productoId}` | HU-06 | Stock de un producto con sus lotes | Todos |
| `POST /api/v1/inventario/ajustes` | HU-07 | Ajustar un lote según el conteo físico | Admin, Almacenero |
| `POST /api/v1/inventario/lotes/{id}/baja` | HU-16 | Dar de baja un lote vencido | Admin, Almacenero |
| `GET /api/v1/inventario/kardex/{productoId}` | HU-37 | Historial de movimientos de un producto | Todos |
| `GET /api/v1/alertas/stock-minimo` | HU-08 | Productos en o bajo su stock mínimo | Todos |
| `GET /api/v1/alertas/vencimientos?dias=` | HU-13 | Lotes vencidos o por vencer | Todos |
| `GET /api/v1/alertas/resumen` | HU-41 | Contadores para el panel de inicio | Todos |
| `POST /api/v1/ventas` | HU-09, 15, 29, 34 | Registrar una venta (FEFO, medio de pago) | Admin, Vendedor |
| `GET /api/v1/ventas?desde=&hasta=` | HU-11 | Buscar ventas por fecha | Admin, Vendedor |
| `GET /api/v1/ventas/{id}` | HU-10 | Detalle de una venta | Admin, Vendedor |
| `POST /api/v1/ventas/{id}/anular` | HU-12 | Anular una venta y devolver el stock | Admin |
| `GET /api/v1/reportes/ventas` | HU-17 | Ventas por día y por medio de pago | Admin |
| `GET /api/v1/reportes/mas-vendidos` | HU-19 | Productos más vendidos | Admin |
| `GET /api/v1/reportes/inventario` | HU-18 | Inventario valorizado | Admin |
| `GET /api/v1/reportes/bajas` | HU-20 | Bajas por vencimiento | Admin |
| `GET·POST·PUT /api/v1/usuarios` | HU-22, 23 | Crear cuentas y asignar rol | Admin |
| `PATCH /api/v1/usuarios/{id}/estado?activo=` | HU-24 | Activar o desactivar una cuenta | Admin |
| `GET /api/v1/roles` | HU-23 | Roles con sus permisos | Admin |

### Formato de error

Todos los errores, incluidos los 401 y 403, tienen la misma forma:

```json
{
  "timestamp": "2026-11-10T00:37:34.28",
  "status": 409,
  "error": "Conflict",
  "message": "Stock insuficiente para 'Paracetamol 500 mg': disponible 60, solicitado 61",
  "path": "/api/v1/ventas",
  "validationErrors": null
}
```

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos; `validationErrors` trae el mensaje de cada campo |
| 401 | Sin token, token inválido o expirado, credenciales incorrectas |
| 403 | El rol no tiene permiso |
| 404 | El recurso no existe |
| 409 | Una regla de negocio lo impide (stock insuficiente, código duplicado, lote vencido) |

## Reglas de negocio

- **FEFO:** la venta sale del lote que vence primero; si no alcanza, completa con el siguiente.
- **Vencidos:** un lote vencido o dado de baja nunca se vende ni cuenta como stock disponible.
- **Todo o nada:** si un producto de la venta no tiene stock suficiente, no se descuenta nada.
- **Precio del servidor:** el importe se calcula con el precio del producto, no con lo que envía la app.
- **Lote repetido:** mismo número y misma fecha de vencimiento suma la cantidad; con otra fecha se rechaza.
- **Vencimiento:** una entrada debe vencer después de hoy.
- **Idempotencia:** entradas y ventas aceptan `claveIdempotencia` (un UUID de la app). Si la app reintenta el envío al recuperar la conexión, no se duplica nada. Es la base de la sincronización offline-first.
- **Kardex:** cada entrada, venta, anulación, ajuste y baja deja un movimiento con usuario, fecha y stock resultante.
- **Anulación:** la venta no se borra; queda ANULADA con su motivo y cada unidad vuelve a su lote.
- **Borrado lógico:** productos, clientes y usuarios se desactivan, no se eliminan, para conservar el historial.
- **Seguridad:** contraseñas con BCrypt; la cuenta se bloquea tras 5 intentos fallidos (HU-46); un usuario desactivado pierde el acceso aunque su token siga vigente; el error de login no revela si falló el usuario o la contraseña.

## Guion de demostración

1. Abrir Swagger y mostrar que `GET /api/v1/productos` sin token responde **401**.
2. `POST /auth/login` con `vendedor` / `Venta123`; copiar el token en **Authorize**.
3. `GET /productos?q=para`: Paracetamol con 450 unidades disponibles.
4. `GET /inventario/stock/1`: dos lotes; `L-PARA-01` vence en 20 días.
5. `POST /ventas` con 160 unidades de Paracetamol: salen 150 de `L-PARA-01` y 10 de `L-PARA-02` (FEFO).
6. `POST /ventas` con 9999 unidades: **409** stock insuficiente.
7. `GET /reportes/ventas` como vendedor: **403**.
8. `GET /alertas/resumen` y `GET /alertas/vencimientos`: aparece el lote vencido `L-LORA-01`.
9. Login como `almacen` / `Almacen123`: `POST /inventario/entradas` y `POST /inventario/lotes/{id}/baja` del lote vencido.
10. Login como `admin` / `Admin123`: `POST /ventas/1/anular`, `GET /inventario/kardex/1` y los reportes.
11. En la terminal, `./mvnw test`: 71 pruebas en verde.

## Fuera de este alcance

Del backlog de 48 historias quedan para siguientes sprints: compras y proveedores (EP-07), descuentos, devoluciones y venta con receta (HU-30 a 32), caja y comprobantes electrónicos (EP-09), inventario físico masivo (HU-38), alertas por correo o WhatsApp (HU-42), sugerencia de reposición (HU-43), bitácora de acciones (HU-45), recuperación de contraseña (HU-47) y respaldos (HU-48).
