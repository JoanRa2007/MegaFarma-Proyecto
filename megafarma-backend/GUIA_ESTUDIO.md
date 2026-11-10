# Guía de estudio · Backend Megafarma

Para el grupo: léanla antes de abrir el código. Explica cómo está armado el proyecto, en qué orden leerlo y qué responder si el profe pregunta.

Dentro del código, cada archivo empieza con un bloque **PARA ESTUDIAR** que dice qué capa es, y cada anotación (`@Entity`, `@GetMapping`, `@Transactional`...) tiene un comentario la primera vez que aparece en el archivo.

## 1. La idea en una frase

La app móvil no toca la base de datos. Le pide las cosas a este servidor por HTTP con JSON, y el servidor decide si se puede (seguridad y reglas de negocio) y guarda en PostgreSQL.

## 2. Qué pasa cuando llega una petición

Ejemplo: el vendedor registra una venta con `POST /api/v1/ventas`.

| Paso | Clase | Qué hace |
|---|---|---|
| 1 | `JwtAuthenticationFilter` | Lee la cabecera `Authorization: Bearer <token>`, valida la firma del token y deja al usuario identificado |
| 2 | `SecurityConfig` | Revisa si el rol del usuario tiene el permiso `VENTAS_REGISTRAR`. Si no, responde 403 |
| 3 | `VentaController` | Convierte el JSON en `VentaRequestDTO` y lo valida (`@Valid`). Si hay datos malos, responde 400 |
| 4 | `VentaServiceImpl` | Aplica las reglas: busca los lotes por orden de vencimiento (FEFO), verifica el stock, descuenta y calcula el total |
| 5 | `LoteRepository`, `VentaRepository` | Leen y guardan en la base de datos |
| 6 | `VentaMapper` | Convierte la entidad `Venta` en `VentaResponseDTO` |
| 7 | `VentaController` | Responde `201 Created` con el JSON de la venta |

Si en el paso 4 algo falla (por ejemplo, stock insuficiente), se lanza una excepción, `@Transactional` deshace todo lo que se había cambiado y `GlobalExceptionHandler` arma el JSON de error.

## 3. Orden recomendado para leer el código

1. `src/main/resources/db/migration/V1__esquema_inicial.sql` — las tablas. Entender los datos primero.
2. `entity/Producto.java`, `entity/Lote.java`, `entity/Venta.java` — las tablas como clases Java.
3. `repository/LoteRepository.java` — cómo se consulta la base, incluida la consulta FEFO.
4. `controller/ProductoController.java` → `service/impl/ProductoServiceImpl.java` — un CRUD completo y sencillo.
5. `service/impl/InventarioServiceImpl.java` — entradas por lote, ajustes y bajas.
6. `service/impl/VentaServiceImpl.java` — la lógica más importante del proyecto.
7. `security/JwtService.java`, `security/JwtAuthenticationFilter.java`, `config/SecurityConfig.java` — la seguridad.
8. `exception/GlobalExceptionHandler.java` — cómo se responden los errores.
9. `src/test/.../VentaServiceImplTest.java` y `api/ApiIntegrationTest.java` — las pruebas.

## 4. Conceptos que hay que poder explicar

**Arquitectura en capas.** Controller → Service → Repository → Entity. Cada capa tiene una sola responsabilidad y solo habla con la siguiente. Ventaja: se puede cambiar la base de datos sin tocar los controladores, y probar las reglas sin levantar el servidor.

**DTO.** Son los objetos del JSON. No devolvemos las entidades directamente porque expondríamos datos internos (por ejemplo, el hash de la contraseña) y ataríamos la API a la forma de las tablas.

**Inyección de dependencias.** Ninguna clase hace `new ProductoRepository()`. Declara lo que necesita en su constructor y Spring se lo entrega. Por eso en las pruebas podemos entregarle objetos simulados.

**JWT.** Al hacer login, el servidor firma un token con una clave secreta. El token lleva el usuario, sus permisos y la fecha de expiración. La app lo envía en cada petición. El servidor no guarda sesiones: solo verifica la firma. Si alguien altera el token, la firma deja de coincidir y se rechaza.

**BCrypt.** La contraseña nunca se guarda. Se guarda un hash que no se puede revertir; en el login se compara el hash.

**Roles y permisos.** Tres roles: ADMIN (todo), VENDEDOR (vender, clientes, consultar), ALMACENERO (catálogo e inventario). La matriz está en `PermisosPorRol` y las rutas en `SecurityConfig`.

**FEFO (First Expired, First Out).** Sale primero el lote que vence antes. En una botica evita que la mercadería se venza en el estante. Si el primer lote no alcanza, se completa con el siguiente. Los lotes vencidos nunca se venden.

**Transacción.** `@Transactional` hace que todo el método sea "todo o nada". Si una venta tiene tres productos y el tercero no tiene stock, no se descuenta ninguno.

**Flyway.** Las tablas se crean con archivos SQL numerados (`V1__`, `V2__`). Flyway recuerda cuáles ya ejecutó. Así todos los del grupo y el servidor tienen exactamente la misma base.

**Idempotencia.** La app genera un identificador único por cada venta o entrada. Si se cae la conexión y reintenta, el servidor reconoce el identificador y no duplica la operación. Es lo que permite el modo offline-first de la app.

**Kardex.** Cada movimiento de stock (entrada, venta, anulación, ajuste, baja) queda registrado con usuario, fecha y stock resultante. Sirve para auditar.

**Borrado lógico.** Productos, clientes y usuarios no se borran: se marcan como inactivos. Si se borraran, las ventas antiguas quedarían apuntando a algo que no existe.

**Códigos HTTP.** 200 bien · 201 creado · 204 bien sin contenido · 400 datos inválidos · 401 no identificado · 403 sin permiso · 404 no existe · 409 una regla de negocio lo impide.

## 5. Preguntas probables del profe

**¿Por qué Spring Boot y no Ktor Server?**
Es el backend que se usó en las guías prácticas del curso; la app lo consume con Ktor Client.

**¿Dónde se valida que no se venda un producto vencido?**
En `LoteRepository.lotesVendiblesFefo`: la consulta solo trae lotes con `fechaVencimiento > hoy`, con stock y no dados de baja. `VentaServiceImpl` solo descuenta de esos.

**¿Qué pasa si dos vendedores venden la última caja al mismo tiempo?**
La consulta FEFO bloquea las filas de los lotes (`@Lock(PESSIMISTIC_WRITE)`). La segunda venta espera a la primera y, al leer, ya ve el stock en cero y responde 409.

**¿La app puede cambiar el precio de una venta?**
No. El request solo lleva producto y cantidad. El precio lo toma el servidor de la tabla de productos.

**¿Cómo se prueba sin base de datos?**
Las pruebas unitarias usan Mockito para simular los repositorios. Las de integración usan H2 en memoria con las mismas migraciones.

**¿Cómo se conecta la app móvil?**
Con Ktor Client apuntando a `http://10.0.2.2:8080` desde el emulador de Android. Primero `POST /api/v1/auth/login`, se guarda el token de forma segura y se envía en la cabecera `Authorization` de cada petición.

**¿Qué falta?**
Compras y proveedores, caja y comprobantes electrónicos, descuentos, devoluciones y notificaciones. Están listados al final del README.

## 6. Cómo trabajamos con Git

| Rama | Para qué |
|---|---|
| `main` | Versión estable, la que se presenta |
| `develop` | Integración del trabajo terminado |
| `feature/<módulo>` | Una rama por módulo; al terminar se une a `develop` |

Para aportar un cambio:

```bash
git checkout develop
```

```bash
git pull
```

```bash
git checkout -b feature/nombre-del-cambio
```

Después de programar y probar (`.\mvnw.cmd test`):

```bash
git add .
```

```bash
git commit -m "feat(modulo): descripción corta del cambio"
```

```bash
git push -u origin feature/nombre-del-cambio
```

Y en GitHub se abre un Pull Request hacia `develop`.
