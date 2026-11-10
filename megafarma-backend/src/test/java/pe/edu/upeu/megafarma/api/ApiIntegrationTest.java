package pe.edu.upeu.megafarma.api;

/*
 * PARA ESTUDIAR · PRUEBA DE INTEGRACION
 * Levanta la aplicacion completa con una base H2 en memoria y llama a la API
 * como lo haria la app movil. Comprueba que todas las capas funcionan juntas.
 */

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Pruebas de integracion: levantan la aplicacion completa (seguridad JWT,
 * controladores, servicios, JPA y Flyway) sobre una base H2 en memoria y
 * llaman a la API por HTTP simulado, igual que lo haria la app movil.
 */
// @SpringBootTest: levanta el contexto completo de Spring para la prueba
@SpringBootTest
// @ActiveProfiles: usa application-test.yaml (base H2 en memoria)
@ActiveProfiles("test")
class ApiIntegrationTest {

    // @Autowired: Spring inyecta aqui el objeto que administra
    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    // @BeforeEach: se ejecuta antes de CADA prueba para dejar todo en estado inicial
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ===================== Autenticacion y permisos =====================

    // @Test: marca el metodo como un caso de prueba
    @Test
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("el estado del servicio es publico")
    void healthEsPublico() throws Exception {
        // mvc.perform: simula una peticion HTTP contra la API
        mvc.perform(get("/api/health"))
                // andExpect: verifica el codigo HTTP o un campo del JSON de respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    @DisplayName("sin token la API responde 401 en el formato de error")
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/productos"));
    }

    @Test
    @DisplayName("un token invalido responde 401")
    void tokenInvalidoResponde401() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer no.es.un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login correcto devuelve token, rol y permisos")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "Admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.permisos", hasItem("USUARIOS_ADMINISTRAR")));
    }

    @Test
    @DisplayName("login incorrecto responde 401 sin revelar que fallo")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "clave-mala")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("no-existe", "Admin123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    @DisplayName("login sin datos responde 400 con el error por campo")
    void loginSinDatos() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("/auth/me devuelve el usuario del token sin exponer la contraseña")
    void perfil() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer("vendedor", "Venta123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("vendedor"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("el vendedor no puede crear productos, ver reportes ni administrar usuarios (403)")
    void vendedorSinPermisos() throws Exception {
        String vendedor = bearer("vendedor", "Venta123");

        mvc.perform(post("/api/v1/productos").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo(), "Producto prohibido", "1.00", categoriaId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(get("/api/v1/reportes/ventas").header("Authorization", vendedor))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/usuarios").header("Authorization", vendedor))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el almacenero registra entradas pero no vende (403)")
    void almaceneroNoVende() throws Exception {
        String almacen = bearer("almacen", "Almacen123");

        mvc.perform(post("/api/v1/ventas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(1L, 1, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("la cuenta se bloquea tras 5 intentos fallidos y el admin la reactiva")
    void bloqueoPorIntentosFallidos() throws Exception {
        String admin = bearer("admin", "Admin123");
        String username = "temp" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult creado = mvc.perform(post("/api/v1/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Temporal123",
                                 "nombreCompleto":"Usuario Temporal","rol":"VENDEDOR"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        long id = idDe(creado);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(credenciales(username, "incorrecta")))
                    .andExpect(status().isUnauthorized());
        }

        // Con la clave correcta ya no entra: esta bloqueada.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(username, "Temporal123")))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/usuarios/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.bloqueado").value(true));

        mvc.perform(patch("/api/v1/usuarios/" + id + "/estado").param("activo", "true")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueado").value(false));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(username, "Temporal123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un usuario desactivado pierde el acceso aunque su token siga vigente")
    void usuarioDesactivadoPierdeAcceso() throws Exception {
        String admin = bearer("admin", "Admin123");
        String username = "baja" + UUID.randomUUID().toString().substring(0, 8);

        long id = idDe(mvc.perform(post("/api/v1/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Temporal123",
                                 "nombreCompleto":"Usuario De Baja","rol":"VENDEDOR"}
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andReturn());

        String token = bearer(username, "Temporal123");
        mvc.perform(get("/api/v1/productos").header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/usuarios/" + id + "/estado").param("activo", "false")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/v1/productos").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    // ===================== CRUD de productos =====================

    @Test
    @DisplayName("CRUD completo de producto: crear, leer, buscar, actualizar y desactivar")
    void crudDeProducto() throws Exception {
        String admin = bearer("admin", "Admin123");
        String codigo = codigo();
        long categoria = categoriaId();

        // POST
        MvcResult creado = mvc.perform(post("/api/v1/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo, "Aspirina Prueba 100 mg", "0.50", categoria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.stockDisponible").value(0))
                .andReturn();
        long id = idDe(creado);

        // POST duplicado -> 409
        mvc.perform(post("/api/v1/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo, "Otro nombre", "0.50", categoria)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString(codigo)));

        // GET por id
        mvc.perform(get("/api/v1/productos/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Aspirina Prueba 100 mg"));

        // GET busqueda por nombre, sin importar mayusculas
        mvc.perform(get("/api/v1/productos").param("q", "aspirina prueba")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].codigo", hasItem(codigo)))
                .andExpect(jsonPath("$.totalElementos").value(1));

        // PUT
        mvc.perform(put("/api/v1/productos/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo, "Aspirina Prueba 500 mg", "0.90", categoria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Aspirina Prueba 500 mg"))
                .andExpect(jsonPath("$.precioVenta").value(0.90));

        // DELETE = desactivar
        mvc.perform(delete("/api/v1/productos/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/productos").param("q", codigo).header("Authorization", admin))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        mvc.perform(get("/api/v1/productos").param("q", codigo).param("incluirInactivos", "true")
                        .header("Authorization", admin))
                .andExpect(jsonPath("$.contenido[0].activo").value(false));
    }

    @Test
    @DisplayName("un producto invalido responde 400 con los errores por campo")
    void productoInvalido() throws Exception {
        mvc.perform(post("/api/v1/productos").header("Authorization", bearer("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\",\"precioVenta\":0,\"stockMinimo\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Existen errores de validación"))
                .andExpect(jsonPath("$.validationErrors.codigo").exists())
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.precioVenta").exists())
                .andExpect(jsonPath("$.validationErrors.stockMinimo").exists())
                .andExpect(jsonPath("$.validationErrors.categoriaId").exists());
    }

    @Test
    @DisplayName("un id inexistente responde 404 y un JSON mal formado responde 400")
    void erroresComunes() throws Exception {
        String admin = bearer("admin", "Admin123");

        mvc.perform(get("/api/v1/productos/999999").header("Authorization", admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));

        mvc.perform(get("/api/v1/productos/abc").header("Authorization", admin))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
                .andExpect(status().isBadRequest());
    }

    // ===================== Inventario y ventas =====================

    @Test
    @DisplayName("flujo completo: entradas por lote, venta FEFO, kardex, idempotencia y anulacion")
    void flujoDeInventarioYVenta() throws Exception {
        String admin = bearer("admin", "Admin123");
        String almacen = bearer("almacen", "Almacen123");
        String vendedor = bearer("vendedor", "Venta123");

        long productoId = crearProducto(admin, "2.50");
        // fecha de hoy segun el reloj del servidor
        LocalDate hoy = LocalDate.now();

        // El almacenero registra dos lotes: uno vence pronto y otro tarde.
        long loteCercano = idDe(mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-CERCA", hoy.plusDays(15), 10, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadActual").value(10))
                .andReturn());

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-LEJOS", hoy.plusDays(300), 50, null)))
                .andExpect(status().isCreated());

        // Mismo lote y misma fecha: suma. Mismo lote y otra fecha: 409.
        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-LEJOS", hoy.plusDays(300), 10, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadActual").value(60));

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-LEJOS", hoy.plusDays(301), 10, null)))
                .andExpect(status().isConflict());

        // Reintento con la misma clave: no vuelve a sumar.
        String claveEntrada = UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(entrada(productoId, "L-EXTRA", hoy.plusDays(500), 5, claveEntrada)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cantidadActual").value(5));
        }

        mvc.perform(get("/api/v1/inventario/stock/" + productoId).header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDisponible").value(75))
                .andExpect(jsonPath("$.lotes", hasSize(3)))
                .andExpect(jsonPath("$.lotes[0].numeroLote").value("L-CERCA"));

        // El vendedor vende 15: FEFO toma 10 del lote cercano y 5 del siguiente.
        String claveVenta = UUID.randomUUID().toString();
        MvcResult vendida = mvc.perform(post("/api/v1/ventas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(productoId, 15, claveVenta)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.vendedor").value("vendedor"))
                .andExpect(jsonPath("$.total").value(37.50))
                .andExpect(jsonPath("$.detalles", hasSize(2)))
                .andExpect(jsonPath("$.detalles[0].numeroLote").value("L-CERCA"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(10))
                .andExpect(jsonPath("$.detalles[1].numeroLote").value("L-LEJOS"))
                .andExpect(jsonPath("$.detalles[1].cantidad").value(5))
                .andReturn();
        long ventaId = idDe(vendida);
        String numero = JsonPath.read(cuerpo(vendida), "$.numero");
        // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
        assertThat(numero).startsWith("V-");

        // Reenvio de la misma venta (sin conexion, reintento): devuelve la misma.
        MvcResult repetida = mvc.perform(post("/api/v1/ventas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(productoId, 15, claveVenta)))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(idDe(repetida)).isEqualTo(ventaId);

        mvc.perform(get("/api/v1/inventario/stock/" + productoId).header("Authorization", vendedor))
                .andExpect(jsonPath("$.stockDisponible").value(60));

        // No se puede vender mas de lo que hay: 409 y el stock no cambia.
        mvc.perform(post("/api/v1/ventas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(productoId, 61, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente")));

        // Kardex: entradas y salidas del producto, lo mas reciente primero.
        mvc.perform(get("/api/v1/inventario/kardex/" + productoId).header("Authorization", almacen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", hasItem("ENTRADA")))
                .andExpect(jsonPath("$[*].tipo", hasItem("SALIDA_VENTA")))
                .andExpect(jsonPath("$[0].referencia").value(numero));

        // Consulta de la venta y busqueda por fecha.
        mvc.perform(get("/api/v1/ventas/" + ventaId).header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(numero));

        mvc.perform(get("/api/v1/ventas").param("desde", hoy.toString()).param("hasta", hoy.toString())
                        .header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].numero", hasItem(numero)));

        // Anular: el vendedor no puede (403); el admin si y el stock vuelve.
        mvc.perform(post("/api/v1/ventas/" + ventaId + "/anular").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Me equivoqué\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/ventas/" + ventaId + "/anular").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cliente devolvió el producto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.motivoAnulacion").value("Cliente devolvió el producto"));

        mvc.perform(post("/api/v1/ventas/" + ventaId + "/anular").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Otra vez\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/inventario/stock/" + productoId).header("Authorization", admin))
                .andExpect(jsonPath("$.stockDisponible").value(75))
                .andExpect(jsonPath("$.lotes[0].cantidadActual").value(10));

        // Ajuste por conteo fisico del lote cercano: de 10 a 8.
        mvc.perform(post("/api/v1/inventario/ajustes").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loteId\":%d,\"cantidadContada\":8,\"motivo\":\"Dos cajas dañadas\"}"
                                .formatted(loteCercano)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadActual").value(8));

        // Un lote vigente no se puede dar de baja.
        mvc.perform(post("/api/v1/inventario/lotes/" + loteCercano + "/baja").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Vencido\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("no se registra una entrada con vencimiento pasado ni con datos invalidos")
    void entradaInvalida() throws Exception {
        String almacen = bearer("almacen", "Almacen123");
        long productoId = crearProducto(bearer("admin", "Admin123"), "1.00");

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-VIEJO", LocalDate.now().minusDays(1), 10, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("posterior a hoy")));

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", almacen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":%d,\"numeroLote\":\"\",\"cantidad\":0}".formatted(productoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.numeroLote").exists())
                .andExpect(jsonPath("$.validationErrors.cantidad").exists())
                .andExpect(jsonPath("$.validationErrors.fechaVencimiento").exists());
    }

    @Test
    @DisplayName("una venta sin productos o con medio de pago invalido responde 400")
    void ventaInvalida() throws Exception {
        String vendedor = bearer("vendedor", "Venta123");

        mvc.perform(post("/api/v1/ventas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioPago\":\"EFECTIVO\",\"detalles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.detalles").exists());

        mvc.perform(post("/api/v1/ventas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioPago\":\"BITCOIN\",\"detalles\":[{\"productoId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    // ===================== Alertas, reportes y catalogos =====================

    @Test
    @DisplayName("los datos de demostracion generan alertas de stock minimo y de vencimiento")
    void alertas() throws Exception {
        String vendedor = bearer("vendedor", "Venta123");

        mvc.perform(get("/api/v1/alertas/stock-minimo").header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("MED-0003")))
                .andExpect(jsonPath("$[*].codigo", not(hasItem("MED-0002"))));

        mvc.perform(get("/api/v1/alertas/vencimientos").param("dias", "30")
                        .header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroLote", hasItem("L-LORA-01")))
                .andExpect(jsonPath("$[*].numeroLote", hasItem("L-PARA-01")))
                .andExpect(jsonPath("$[*].numeroLote", not(hasItem("L-PARA-02"))));

        mvc.perform(get("/api/v1/alertas/resumen").header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotesVencidos").isNumber())
                .andExpect(jsonPath("$.diasConsiderados").value(60));
    }

    @Test
    @DisplayName("un lote vencido no se vende y se puede dar de baja")
    void loteVencido() throws Exception {
        String admin = bearer("admin", "Admin123");

        // Loratadina (MED-0005): 40 vencidas + 120 vigentes en los datos de demostracion.
        MvcResult busqueda = mvc.perform(get("/api/v1/productos").param("q", "MED-0005")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn();
        long productoId = ((Number) JsonPath.read(cuerpo(busqueda), "$.contenido[0].id")).longValue();

        MvcResult stock = mvc.perform(get("/api/v1/inventario/stock/" + productoId)
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotes[0].numeroLote").value("L-LORA-01"))
                .andExpect(jsonPath("$.lotes[0].vencido").value(true))
                .andReturn();
        int disponible = JsonPath.read(cuerpo(stock), "$.stockDisponible");
        int registrado = JsonPath.read(cuerpo(stock), "$.stockRegistrado");
        long loteVencido = ((Number) JsonPath.read(cuerpo(stock), "$.lotes[0].id")).longValue();

        // El stock disponible no cuenta el lote vencido.
        assertThat(registrado - disponible).isEqualTo(40);

        // Pedir mas que lo vigente falla aunque "en el estante" haya unidades vencidas.
        mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(productoId, disponible + 1, null)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/inventario/lotes/" + loteVencido + "/baja").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Vencido en estante\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dadoDeBaja").value(true))
                .andExpect(jsonPath("$.cantidadActual").value(0));

        mvc.perform(get("/api/v1/reportes/bajas").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroLote", hasItem("L-LORA-01")))
                .andExpect(jsonPath("$[0].tipo").value("BAJA_VENCIMIENTO"));
    }

    @Test
    @DisplayName("el admin consulta los reportes de ventas, mas vendidos e inventario")
    void reportes() throws Exception {
        String admin = bearer("admin", "Admin123");
        long productoId = crearProducto(admin, "3.00");

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entrada(productoId, "L-REP", LocalDate.now().plusDays(100), 20, null)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(productoId, 4, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(12.00));

        mvc.perform(get("/api/v1/reportes/ventas").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadVentas").isNumber())
                .andExpect(jsonPath("$.totalVendido").isNumber())
                .andExpect(jsonPath("$.porDia[0].etiqueta").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.porMedioPago[*].etiqueta", hasItem("EFECTIVO")));

        mvc.perform(get("/api/v1/reportes/mas-vendidos").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productoId", hasItem((int) productoId)));

        mvc.perform(get("/api/v1/reportes/inventario").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadesTotales").isNumber())
                .andExpect(jsonPath("$.valorAPrecioVenta").isNumber());

        mvc.perform(get("/api/v1/reportes/ventas").param("desde", "2026-12-31").param("hasta", "2026-01-01")
                        .header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CRUD de categorias y clientes")
    void categoriasYClientes() throws Exception {
        String admin = bearer("admin", "Admin123");
        String sufijo = UUID.randomUUID().toString().substring(0, 6);

        long categoria = idDe(mvc.perform(post("/api/v1/categorias").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dermatología %s\",\"descripcion\":\"Cremas\"}".formatted(sufijo)))
                .andExpect(status().isCreated())
                .andReturn());

        mvc.perform(put("/api/v1/categorias/" + categoria).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dermatología %s\",\"descripcion\":\"Cremas y lociones\"}".formatted(sufijo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Cremas y lociones"));

        mvc.perform(delete("/api/v1/categorias/" + categoria).header("Authorization", admin))
                .andExpect(status().isNoContent());

        // Una categoria con productos no se elimina.
        mvc.perform(delete("/api/v1/categorias/" + categoriaId()).header("Authorization", admin))
                .andExpect(status().isConflict());

        String dni = String.valueOf(10000000 + (int) (Math.random() * 89999999));
        long cliente = idDe(mvc.perform(post("/api/v1/clientes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"%s\",\"nombre\":\"Ana Pérez %s\"}"
                                .formatted(dni, sufijo)))
                .andExpect(status().isCreated())
                .andReturn());

        mvc.perform(post("/api/v1/clientes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"%s\",\"nombre\":\"Repetida\"}"
                                .formatted(dni)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/clientes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"123456789\",\"nombre\":\"DNI largo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("8 dígitos")));

        mvc.perform(get("/api/v1/clientes").param("q", dni).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value((int) cliente));
    }

    // ===================== Apoyo =====================

    private String bearer(String username, String password) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(cuerpo(resultado), "$.token");
        return "Bearer " + token;
    }

    private long categoriaId() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/categorias")
                        .header("Authorization", bearer("admin", "Admin123")))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(cuerpo(resultado), "$[0].id")).longValue();
    }

    private long crearProducto(String token, String precio) throws Exception {
        ResultActions acciones = mvc.perform(post("/api/v1/productos").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(producto(codigo(), "Producto de prueba " + UUID.randomUUID(), precio, categoriaId())));
        return idDe(acciones.andExpect(status().isCreated()).andReturn());
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static long idDe(MvcResult resultado) throws Exception {
        return ((Number) JsonPath.read(cuerpo(resultado), "$.id")).longValue();
    }

    private static String codigo() {
        return "TST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String credenciales(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    private static String producto(String codigo, String nombre, String precio, long categoriaId) {
        return """
                {"codigo":"%s","nombre":"%s","principioActivo":"Compuesto de prueba",
                 "presentacion":"Caja x 10","laboratorio":"Lab Test",
                 "precioVenta":%s,"stockMinimo":5,"requiereReceta":false,"categoriaId":%d}
                """.formatted(codigo, nombre, precio, categoriaId);
    }

    private static String entrada(long productoId, String lote, LocalDate vence, int cantidad, String clave) {
        return """
                {"productoId":%d,"numeroLote":"%s","fechaVencimiento":"%s","cantidad":%d,
                 "precioCompra":1.10,"claveIdempotencia":%s}
                """.formatted(productoId, lote, vence, cantidad, clave == null ? "null" : "\"" + clave + "\"");
    }

    private static String venta(long productoId, int cantidad, String clave) {
        return """
                {"medioPago":"EFECTIVO","detalles":[{"productoId":%d,"cantidad":%d}],
                 "claveIdempotencia":%s}
                """.formatted(productoId, cantidad, clave == null ? "null" : "\"" + clave + "\"");
    }
}
