# Seguridad de Megafarma

Resumen de cómo está protegida la API y la app, ordenado según el **OWASP Top 10**.
Todo lo de esta página está cubierto por pruebas automáticas
(`megafarma-backend/src/test/java/pe/edu/upeu/megafarma/api/SeguridadTest.java`).

## Qué se protegió

| Riesgo (OWASP 2021) | Cómo se protege |
|---|---|
| **A01 Control de acceso roto** | Todas las rutas exigen un token JWT, salvo `/auth/login` y `/api/health`. Cada ruta pide un permiso del rol (`VENTAS_REGISTRAR`, `REPORTES_VER`, `USUARIOS_ADMINISTRAR`…). Un vendedor que entra a reportes recibe **403** y el intento queda en la bitácora. |
| **A02 Fallas criptográficas** | Contraseñas con **BCrypt (costo 12)**. JWT firmado con HMAC-SHA y clave por variable de entorno en producción (`APP_JWT_SECRET`). |
| **A03 Inyección** | Las consultas son **parametrizadas** (JPA): el texto del usuario nunca se pega al SQL. Además se valida la entrada: la búsqueda solo acepta letras, números y `. , ' ( ) / % + -` (sin `--`, `;`, `<`, `>`), con un máximo de 100 caracteres. Paginación y orden también se validan. Un ataque como `' OR '1'='1` responde **400**. |
| **A04 Diseño inseguro** | Límite de intentos de login por IP: tras 10 fallos en 60 s responde **429 Too Many Requests** con `Retry-After`. Las cuentas se bloquean tras varios fallos. |
| **A05 Configuración insegura** | Swagger **desactivado** fuera de desarrollo. Los errores no muestran stacktrace ni mensajes internos. Cabeceras: `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, `Cache-Control: no-store`. CORS solo para los orígenes configurados. |
| **A07 Fallas de autenticación** | Tokens con emisor (`megafarma-api`), identificador único (`jti`) y expiración. **Cerrar sesión revoca el token** en el servidor: aunque alguien lo copie, deja de funcionar. Contraseñas con al menos una letra y un número. |
| **A09 Fallas de registro y monitoreo** | **Bitácora de auditoría** (tabla `auditoria`): logins correctos y fallidos, bloqueos, accesos denegados, ataques de fuerza bruta, ventas, anulaciones, cambios de productos, inventario y usuarios, con usuario, fecha e IP. El administrador la consulta en la app (menú **Seguridad**) o en `GET /api/v1/auditoria`. |

## Perfiles

| Perfil | Swagger | Datos demo | Secretos |
|---|---|---|---|
| `dev` / `local` | Sí | Sí | En el yaml (solo para clase) |
| `prod` | **No** | No | Variables de entorno `DB_URL`, `DB_USUARIO`, `DB_CLAVE`, `APP_JWT_SECRET`, `APP_CORS_ORIGENES` |

## Demostración rápida (PowerShell, con el backend encendido)

Inyección SQL rechazada (400):

```powershell
$t = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' -Body '{"username":"admin","password":"Admin123"}').token
```

```powershell
curl.exe -s -H "Authorization: Bearer $t" "http://localhost:8080/api/v1/productos?q=%27%20OR%20%271%27%3D%271"
```

Sin token (401) y cabeceras de seguridad:

```powershell
curl.exe -i http://localhost:8080/api/v1/productos
```

Cerrar sesión revoca el token (la segunda llamada da 401):

```powershell
curl.exe -s -X POST -H "Authorization: Bearer $t" http://localhost:8080/api/v1/auth/logout; curl.exe -i -H "Authorization: Bearer $t" http://localhost:8080/api/v1/auth/me
```

Fuerza bruta: después de 10 contraseñas incorrectas seguidas responde 429.

```powershell
1..11 | ForEach-Object { curl.exe -s -o NUL -w "%{http_code} " -X POST -H "Content-Type: application/json" -d '{\"username\":\"admin\",\"password\":\"mal\"}' http://localhost:8080/api/v1/auth/login }
```

> Ojo: esta prueba bloquea tu IP durante un minuto.

## Pruebas

```powershell
cd megafarma-backend; .\mvnw.cmd test
```

97 pruebas, 11 de ellas de seguridad: inyección SQL, parámetros inválidos, cabeceras,
Swagger oculto, fuerza bruta, revocación de token, token de otro emisor, política de
contraseñas y bitácora.
