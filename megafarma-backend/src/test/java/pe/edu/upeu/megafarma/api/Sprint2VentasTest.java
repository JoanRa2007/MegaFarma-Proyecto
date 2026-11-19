package pe.edu.upeu.megafarma.api;

/*
 * PARA ESTUDIAR · PRUEBA DE INTEGRACION
 * Casos del Sprint 2 (Ingenieria de Software) sobre ventas: concurrencia,
 * idempotencia, validaciones de cantidad y fechas, y paginacion completa.
 * Levanta la aplicacion con H2 en memoria y llama a la API por HTTP simulado.
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class Sprint2VentasTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ===================== Validaciones (HU-09, HU-11) =====================

    @Test
    @DisplayName("CP-S2-40: una cantidad decimal se rechaza con 400 y el stock no cambia")
    void cantidadDecimal() throws Exception {
        String admin = bearer("admin", "Admin123");
        long producto = productoConStock(admin, 10);

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medioPago\":\"EFECTIVO\",\"detalles\":[{\"productoId\":%d,\"cantidad\":1.5}]}"
                                .formatted(producto)))
                .andExpect(status().isBadRequest());

        assertThat(stockDisponible(admin, producto)).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-S2-47 y CP-S2-48: fecha imposible y rango invertido responden 400")
    void fechasInvalidas() throws Exception {
        String admin = bearer("admin", "Admin123");

        mvc.perform(get("/api/v1/ventas").param("desde", "2026-02-30").param("hasta", "2026-03-01")
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/ventas").param("desde", "2026-11-20").param("hasta", "2026-11-19")
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha 'desde' no puede ser posterior a la fecha 'hasta'"));
    }

    @Test
    @DisplayName("CP-S2-24: 25 ventas en páginas de 20 se recorren sin omitir ni duplicar")
    void paginacionCompleta() throws Exception {
        String admin = bearer("admin", "Admin123");
        long producto = productoConStock(admin, 100);

        Set<Long> creadas = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            creadas.add(idDe(mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(venta(producto, 1, null)))
                    .andExpect(status().isCreated())
                    .andReturn()));
        }

        String hoy = LocalDate.now().toString();
        List<Long> vistas = new ArrayList<>();
        int pagina = 0;
        boolean ultima;
        do {
            MvcResult r = mvc.perform(get("/api/v1/ventas").header("Authorization", admin)
                            .param("desde", hoy).param("hasta", hoy)
                            .param("pagina", String.valueOf(pagina)).param("tamanio", "20"))
                    .andExpect(status().isOk())
                    .andReturn();
            List<Number> ids = JsonPath.read(cuerpo(r), "$.contenido[*].id");
            ids.forEach(n -> vistas.add(n.longValue()));
            ultima = JsonPath.read(cuerpo(r), "$.ultima");
            pagina++;
        } while (!ultima && pagina < 50);

        assertThat(vistas).doesNotHaveDuplicates();
        assertThat(vistas).containsAll(creadas);
    }

    // ===================== Concurrencia (HU-09, HU-12) =====================

    @Test
    @DisplayName("CP-S2-30: dos anulaciones simultáneas reponen el stock una sola vez")
    void dobleAnulacionSimultanea() throws Exception {
        String admin = bearer("admin", "Admin123");
        long producto = productoConStock(admin, 10);
        long venta = idDe(mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(venta(producto, 4, null)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(stockDisponible(admin, producto)).isEqualTo(6);

        List<Integer> estados = enParalelo(2, () -> mvc.perform(post("/api/v1/ventas/" + venta + "/anular")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Prueba de concurrencia\"}"))
                .andReturn().getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(200, 409);
        assertThat(stockDisponible(admin, producto)).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-S2-12: dos envíos simultáneos con la misma clave crean una sola venta")
    void mismaClaveSimultanea() throws Exception {
        String admin = bearer("admin", "Admin123");
        long producto = productoConStock(admin, 20);
        String clave = UUID.randomUUID().toString();

        List<String> respuestas = enParalelo(2, () -> {
            MvcResult r = mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON).content(venta(producto, 3, clave)))
                    .andReturn();
            return r.getResponse().getStatus() + "|" + cuerpo(r);
        });

        Set<Long> ids = new HashSet<>();
        for (String r : respuestas) {
            assertThat(r).startsWith("201|");
            ids.add(((Number) JsonPath.read(r.substring(4), "$.id")).longValue());
        }
        assertThat(ids).hasSize(1);
        assertThat(stockDisponible(admin, producto)).isEqualTo(17);
    }

    @Test
    @DisplayName("CP-S2-11: dos ventas simultáneas distintas no dejan stock negativo")
    void ventasDistintasSimultaneas() throws Exception {
        String admin = bearer("admin", "Admin123");
        long producto = productoConStock(admin, 5);

        List<Integer> estados = enParalelo(2, () -> mvc.perform(post("/api/v1/ventas")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 4, UUID.randomUUID().toString())))
                .andReturn().getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(stockDisponible(admin, producto)).isEqualTo(1);
    }

    // ===================== Apoyo =====================

    /* Ejecuta la misma tarea en varios hilos que arrancan a la vez. */
    private <T> List<T> enParalelo(int hilos, Callable<T> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<T> resultados = new ArrayList<>();
            for (Future<T> f : futuros) {
                resultados.add(f.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private long productoConStock(String token, int cantidad) throws Exception {
        MvcResult categorias = mvc.perform(get("/api/v1/categorias").header("Authorization", token)).andReturn();
        long categoria = ((Number) JsonPath.read(cuerpo(categorias), "$[0].id")).longValue();
        String codigo = "S2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        long producto = idDe(mvc.perform(post("/api/v1/productos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","nombre":"Producto Sprint 2 %s","precioVenta":7.50,
                                 "stockMinimo":1,"requiereReceta":false,"categoriaId":%d}
                                """.formatted(codigo, codigo, categoria)))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"numeroLote":"L-%s","fechaVencimiento":"%s","cantidad":%d}
                                """.formatted(producto, codigo, LocalDate.now().plusDays(200), cantidad)))
                .andExpect(status().isCreated());
        return producto;
    }

    private int stockDisponible(String token, long producto) throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/inventario/stock/" + producto).header("Authorization", token))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(cuerpo(r), "$.stockDisponible");
    }

    private String bearer(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(cuerpo(r), "$.token");
    }

    private static String venta(long producto, int cantidad, String clave) {
        return """
                {"medioPago":"EFECTIVO","detalles":[{"productoId":%d,"cantidad":%d}],"claveIdempotencia":%s}
                """.formatted(producto, cantidad, clave == null ? "null" : "\"" + clave + "\"");
    }

    private static String cuerpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static long idDe(MvcResult r) throws Exception {
        return ((Number) JsonPath.read(cuerpo(r), "$.id")).longValue();
    }
}
