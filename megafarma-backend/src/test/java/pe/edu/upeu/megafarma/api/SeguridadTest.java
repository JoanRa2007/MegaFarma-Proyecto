package pe.edu.upeu.megafarma.api;

/*
 * PARA ESTUDIAR · PRUEBA DE INTEGRACION
 * Pruebas de seguridad de la API:
 *  - inyeccion SQL en busquedas (rechazada y, aunque pasara, inofensiva),
 *  - validacion de parametros, cabeceras de seguridad, fuerza bruta (429),
 *  - revocacion del token al cerrar sesion, tokens de otro emisor,
 *  - politica de contrasenas, bitacora de auditoria y Swagger no expuesto.
 */

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pe.edu.upeu.megafarma.repository.ProductoRepository;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SeguridadTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ProductoRepository productoRepository;

    @Value("${app.jwt.secret}")
    private String secretoJwt;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ===================== Inyeccion SQL (OWASP A03) =====================

    @Test
    @DisplayName("búsquedas con inyección SQL o scripts se rechazan con 400 y la tabla sigue intacta")
    void inyeccionSqlEnBusqueda() throws Exception {
        String admin = bearer("admin", "Admin123");
        List<String> ataques = List.of(
                "' OR '1'='1",
                "1; DROP TABLE productos",
                "x' UNION SELECT username, password_hash FROM usuarios --",
                "<script>alert(1)</script>",
                "admin'--");

        for (String ataque : ataques) {
            mvc.perform(get("/api/v1/productos").param("q", ataque).header("Authorization", admin))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("q")));
        }

        mvc.perform(get("/api/v1/clientes").param("q", "' OR 1=1 --").header("Authorization", admin))
                .andExpect(status().isBadRequest());

        // La tabla sigue existiendo y con sus datos.
        mvc.perform(get("/api/v1/productos").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(15)));
    }

    @Test
    @DisplayName("una búsqueda legítima con apóstrofo funciona")
    void busquedaLegitima() throws Exception {
        mvc.perform(get("/api/v1/productos").param("q", "d'onofrio").header("Authorization", bearer("admin", "Admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aunque el texto llegara a la consulta, es un parámetro: no altera el SQL")
    void consultasParametrizadas() {
        long total = productoRepository.count();
        var resultado = productoRepository.buscar("%' or '1'='1%", true, false, PageRequest.of(0, 50));
        assertThat(resultado.getTotalElements()).isZero();
        assertThat(productoRepository.count()).isEqualTo(total);
    }

    @Test
    @DisplayName("parámetros fuera de rango o con caracteres raros responden 400")
    void parametrosInvalidos() throws Exception {
        String admin = bearer("admin", "Admin123");
        mvc.perform(get("/api/v1/productos").param("tamanio", "1000").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/productos").param("pagina", "-1").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/productos").param("ordenarPor", "nombre;drop").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alertas/vencimientos").param("dias", "99999").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    // ===================== Cabeceras (OWASP A05) =====================

    @Test
    @DisplayName("las respuestas llevan las cabeceras de seguridad")
    void cabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    @DisplayName("Swagger no está expuesto fuera de desarrollo")
    void swaggerNoExpuesto() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
    }

    // ===================== Autenticación (OWASP A07) =====================

    @Test
    @DisplayName("fuerza bruta: tras 15 fallos desde la misma IP responde 429; otra IP sigue entrando")
    void fuerzaBrutaPorIp() throws Exception {
        RequestPostProcessor atacante = desdeIp("10.9.9.9");
        for (int i = 0; i < 15; i++) {
            mvc.perform(post("/api/v1/auth/login").with(atacante).contentType(MediaType.APPLICATION_JSON)
                            .content(credenciales("usuario" + i, "incorrecta")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").with(atacante).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "Admin123")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));

        mvc.perform(post("/api/v1/auth/login").with(desdeIp("10.8.8.8")).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("admin", "Admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("al cerrar sesión el token queda revocado aunque no haya expirado")
    void logoutRevocaToken() throws Exception {
        String token = bearer("vendedor", "Venta123");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/productos").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token firmado con la clave correcta pero de otro emisor se rechaza")
    void tokenDeOtroEmisor() throws Exception {
        String falso = Jwts.builder()
                .issuer("otro-sistema")
                .subject("admin")
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoJwt)))
                .compact();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("una contraseña sin números se rechaza al crear un usuario")
    void politicaDeContrasenas() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin", "Admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"debil1\",\"password\":\"soloLetras\",\"nombreCompleto\":\"Clave Débil\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password", containsString("letra y un número")));
    }

    // ===================== Auditoría (OWASP A09) =====================

    @Test
    @DisplayName("la bitácora registra logins fallidos y accesos denegados; solo el admin la ve")
    void bitacoraDeAuditoria() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(desdeIp("10.7.7.7")).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("vendedor", "clave-equivocada")))
                .andExpect(status().isUnauthorized());

        String vendedor = bearer("vendedor", "Venta123");
        mvc.perform(get("/api/v1/reportes/inventario").header("Authorization", vendedor))
                .andExpect(status().isForbidden());

        String admin = bearer("admin", "Admin123");
        mvc.perform(get("/api/v1/auditoria").param("usuario", "vendedor").param("accion", "LOGIN_FALLIDO")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].ip", hasItem("10.7.7.7")))
                .andExpect(jsonPath("$.contenido[0].exito").value(false));

        mvc.perform(get("/api/v1/auditoria").param("accion", "ACCESO_DENEGADO").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].detalle", hasItem("GET /api/v1/reportes/inventario")));

        mvc.perform(get("/api/v1/auditoria").header("Authorization", vendedor))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/auditoria").param("usuario", "x' OR '1'='1").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }

    // ===================== Apoyo =====================

    private static RequestPostProcessor desdeIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private String bearer(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(username, password)))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");
    }

    private static String credenciales(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }
}
