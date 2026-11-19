# Megafarma · App móvil y página web

Frontend del **sistema de punto de venta** de la botica Megafarma. Es **un solo proyecto Kotlin Multiplatform + Compose Multiplatform** que genera:

- la **app Android**,
- la **página web** (Kotlin/Wasm en el navegador),
- y la app iOS (se compila en una Mac con Xcode).

Todas consumen la API del backend (`../megafarma-backend`) con Ktor Client.

## Arquitectura (Clean + MVVM)

```
shared/src/commonMain/kotlin/pe/edu/upeu/megafarma/app/
├── core/            Resultado (sealed class de éxito/error) y formatos
├── domain/
│   ├── model/       Producto, Lote, Venta, Sesion... (sin dependencias técnicas)
│   ├── repository/  Interfaces de los repositorios
│   └── usecase/     IniciarSesion, RegistrarVenta, RegistrarEntrada
├── data/
│   ├── remote/      Ktor Client (plugins: JSON, logging, timeout, token), API y DTO
│   ├── local/       Sesión guardada en el dispositivo
│   └── repository/  Implementaciones: llaman a la API y convierten DTO -> dominio
├── presentation/    Pantallas Compose + ViewModel + UiState (StateFlow) por pantalla
└── di/              Módulos de Koin
```

Lo propio de cada plataforma está en `androidMain`, `wasmJsMain` e `iosMain` con **expect/actual**: el motor HTTP (OkHttp, fetch del navegador, Darwin), dónde se guarda la sesión (SharedPreferences, localStorage, NSUserDefaults) y la dirección del backend por defecto.

## Pantallas

| Pantalla | Qué hace | Roles |
|---|---|---|
| Login | Inicia sesión con JWT; permite cambiar la dirección del servidor | Todos |
| Inicio | Alertas de stock bajo, lotes por vencer y vencidos; accesos rápidos | Todos |
| Productos y stock | Búsqueda; detalle con stock por lote y cuál sale primero (FEFO) | Todos |
| — Nuevo / editar / desactivar producto | | Admin, Almacenero |
| — Registrar entrada por lote, dar de baja lote vencido | | Admin, Almacenero |
| Nueva venta (caja) | Carrito, cliente, boleta o factura, pagos mixtos con vuelto; al cobrar muestra el comprobante electrónico | Admin, Vendedor |
| Dashboard de ventas | Hoy, semana, mes o año: total, ticket promedio, comparación con el período anterior, gráfico, top 5, categorías y medios de pago | Admin |
| Ventas | Búsqueda por fechas con páginas, detalle, comprobante y XML; anulación con confirmación | Admin, Vendedor (anular: Admin) |
| Alertas | Stock mínimo y vencimientos por nivel: crítico, medio y bajo | Todos |
| Reportes | Reporte del día, mes o año (con gráfico), más vendidos, categorías, medios de pago e inventario valorizado | Admin |
| Usuarios | Crear usuarios, cambiar rol o contraseña, activar y desactivar cuentas | Admin |

El menú solo muestra lo que el rol puede usar. Tiene modo claro/oscuro y se adapta al ancho: menú fijo en pantallas anchas (web) y menú desplegable en el celular.

## Cómo ejecutarlo

Primero encender el backend (ver `../megafarma-backend/README.md`).

### Página web

```bash
.\gradlew.bat :shared:wasmJsBrowserDevelopmentRun
```

Se abre en http://localhost:5173 (ese puerto está permitido por CORS en el backend).

### App Android

Abrir la carpeta `megafarma-app` en Android Studio y ejecutar `androidApp` en un emulador o celular. Para generar el APK:

```bash
.\gradlew.bat :androidApp:assembleDebug
```

Queda en `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

| Dónde corre la app | Servidor (se cambia en el login) |
|---|---|
| Emulador de Android | `http://10.0.2.2:8080` (por defecto) |
| Celular real en el mismo wifi | `http://<IP de la laptop>:8080` |

### Usuarios de prueba

`admin` / `Admin123` · `vendedor` / `Venta123` · `almacen` / `Almacen123`
