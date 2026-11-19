package pe.edu.upeu.megafarma.api;

/*
 * PARA ESTUDIAR · PRUEBA DE INTEGRACION
 * Mejoras del punto de venta: pagos mixtos y vuelto, IGV, boleta y factura
 * electronica (modo demostracion), dashboard, reportes por dia/mes/año y
 * alertas de vencimiento por nivel.
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PuntoDeVentaTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        admin = bearer("admin", "Admin123");
    }

    // ===================== Pagos =====================

    @Test
    @DisplayName("efectivo con vuelto: paga S/ 20 por S/ 15 y recibe S/ 5 de vuelto")
    void efectivoConVuelto() throws Exception {
        long producto = productoConStock("7.50", 10);

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, "[{\"medio\":\"EFECTIVO\",\"monto\":20.00}]", null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(15.00))
                .andExpect(jsonPath("$.medioPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.montoRecibido").value(20.00))
                .andExpect(jsonPath("$.vuelto").value(5.00))
                .andExpect(jsonPath("$.pagos", hasSize(1)))
                .andExpect(jsonPath("$.pagos[0].monto").value(15.00));
    }

    @Test
    @DisplayName("pago mixto: Yape con número de operación + efectivo con vuelto")
    void pagoMixto() throws Exception {
        long producto = productoConStock("7.50", 10);

        MvcResult r = mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 4, """
                                [{"medio":"YAPE","monto":20.00,"referencia":"OP123456"},
                                 {"medio":"EFECTIVO","monto":15.00}]""", null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(30.00))
                .andExpect(jsonPath("$.medioPago").value("MIXTO"))
                .andExpect(jsonPath("$.vuelto").value(5.00))
                .andExpect(jsonPath("$.pagos", hasSize(2)))
                .andReturn();

        BigDecimal subtotal = new BigDecimal(JsonPath.read(cuerpo(r), "$.subtotal").toString());
        BigDecimal igv = new BigDecimal(JsonPath.read(cuerpo(r), "$.igv").toString());
        assertThat(subtotal.add(igv)).isEqualByComparingTo("30.00");
        assertThat(igv).isEqualByComparingTo("4.58");
    }

    @Test
    @DisplayName("pagos inválidos: monto insuficiente, Yape sin operación, tarjeta sin 4 dígitos, tarjeta mayor al total")
    void pagosInvalidos() throws Exception {
        long producto = productoConStock("7.50", 10);

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, "[{\"medio\":\"EFECTIVO\",\"monto\":10.00}]", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", startsWith("Falta pagar S/ 5.00")));

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, "[{\"medio\":\"YAPE\",\"monto\":15.00}]", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("número de operación")));

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, "[{\"medio\":\"TARJETA\",\"monto\":15.00,\"referencia\":\"12\"}]", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("4 dígitos")));

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, "[{\"medio\":\"TARJETA\",\"monto\":20.00,\"referencia\":\"4242\"}]", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("superan el total")));

        assertThat(stockDisponible(producto)).isEqualTo(10);
    }

    // ===================== Comprobantes SUNAT (demostración) =====================

    @Test
    @DisplayName("cada venta emite una boleta B001 con correlativo, IGV, XML UBL 2.1 y QR")
    void boletaElectronica() throws Exception {
        long producto = productoConStock("11.80", 10);

        MvcResult primera = mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(venta(producto, 1, null, null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoComprobante").value("BOLETA"))
                .andExpect(jsonPath("$.numeroComprobante", startsWith("B001-")))
                .andReturn();
        MvcResult segunda = mvc.perform(post("/api/v1/ventas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(venta(producto, 1, null, null, null)))
                .andExpect(status().isCreated()).andReturn();

        int n1 = Integer.parseInt(((String) JsonPath.read(cuerpo(primera), "$.numeroComprobante")).substring(5));
        int n2 = Integer.parseInt(((String) JsonPath.read(cuerpo(segunda), "$.numeroComprobante")).substring(5));
        assertThat(n2).isEqualTo(n1 + 1);

        long ventaId = idDe(primera);
        mvc.perform(get("/api/v1/ventas/" + ventaId + "/comprobante").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("BOLETA"))
                .andExpect(jsonPath("$.codigoSunat").value("03"))
                .andExpect(jsonPath("$.emisorRuc").value("20612345678"))
                .andExpect(jsonPath("$.clienteNombre").value("CLIENTES VARIOS"))
                .andExpect(jsonPath("$.opGravada").value(10.00))
                .andExpect(jsonPath("$.igv").value(1.80))
                .andExpect(jsonPath("$.total").value(11.80))
                .andExpect(jsonPath("$.totalEnLetras").value("ONCE CON 80/100 SOLES"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hash").isNotEmpty())
                .andExpect(jsonPath("$.textoQr", startsWith("20612345678|03|B001|")))
                .andExpect(jsonPath("$.modoDemostracion").value(true));

        mvc.perform(get("/api/v1/ventas/" + ventaId + "/comprobante/xml").header("Authorization", admin)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>")))
                .andExpect(content().string(containsString("<cbc:InvoiceTypeCode listID=\"0101\">03</cbc:InvoiceTypeCode>")))
                .andExpect(content().string(containsString("<cbc:TaxAmount currencyID=\"PEN\">1.80</cbc:TaxAmount>")));

        // Al anular la venta, el comprobante queda ANULADO.
        mvc.perform(post("/api/v1/ventas/" + ventaId + "/anular").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/ventas/" + ventaId + "/comprobante").header("Authorization", admin))
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    @DisplayName("la factura exige un cliente con RUC; con RUC se emite F001")
    void facturaConRuc() throws Exception {
        long producto = productoConStock("7.50", 10);

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 1, null, "FACTURA", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La factura requiere un cliente con RUC"));

        String ruc = "20" + String.valueOf(100000000L + (long) (Math.random() * 899999999L));
        long cliente = idDe(mvc.perform(post("/api/v1/clientes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"RUC\",\"numeroDocumento\":\"%s\",\"nombre\":\"Clínica San Juan S.A.C.\"}"
                                .formatted(ruc)))
                .andExpect(status().isCreated()).andReturn());

        MvcResult r = mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 1, null, "FACTURA", cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoComprobante").value("FACTURA"))
                .andExpect(jsonPath("$.numeroComprobante", startsWith("F001-")))
                .andReturn();

        mvc.perform(get("/api/v1/ventas/" + idDe(r) + "/comprobante").header("Authorization", admin))
                .andExpect(jsonPath("$.codigoSunat").value("01"))
                .andExpect(jsonPath("$.clienteNumDoc").value(ruc))
                .andExpect(jsonPath("$.clienteNombre").value("Clínica San Juan S.A.C."));
    }

    @Test
    @DisplayName("una boleta mayor a S/ 700 debe identificar al cliente")
    void boletaMayorA700() throws Exception {
        long producto = productoConStock("400.00", 5);

        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 2, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("S/ 700.00")));

        assertThat(stockDisponible(producto)).isEqualTo(5);
    }

    // ===================== Dashboard y reportes =====================

    @Test
    @DisplayName("dashboard de hoy: indicadores, 24 horas, top de productos, categorías y medios de pago")
    void dashboardHoy() throws Exception {
        long producto = productoConStock("5.00", 20);
        mvc.perform(post("/api/v1/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(venta(producto, 3, null, null, null)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/dashboard").param("periodo", "HOY").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("HOY"))
                .andExpect(jsonPath("$.agrupacion").value("HORA"))
                .andExpect(jsonPath("$.serie", hasSize(24)))
                .andExpect(jsonPath("$.cantidadVentas").isNumber())
                .andExpect(jsonPath("$.ticketPromedio").isNumber())
                .andExpect(jsonPath("$.topProductos").isArray())
                .andExpect(jsonPath("$.porCategoria[*].etiqueta").isArray())
                .andExpect(jsonPath("$.porMedioPago[*].etiqueta", hasItem("EFECTIVO")));

        mvc.perform(get("/api/v1/dashboard").param("periodo", "ANIO").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agrupacion").value("MES"));

        // El vendedor no ve el dashboard.
        mvc.perform(get("/api/v1/dashboard").header("Authorization", bearer("vendedor", "Venta123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("reportes por día (24 horas), mes (días del mes) y año (12 meses)")
    void reportesPorPeriodo() throws Exception {
        mvc.perform(get("/api/v1/reportes/resumen").param("tipo", "DIA").param("fecha", "2026-11-19")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie", hasSize(24)));

        mvc.perform(get("/api/v1/reportes/resumen").param("tipo", "MES").param("fecha", "2026-02-10")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-02-01"))
                .andExpect(jsonPath("$.hasta").value("2026-02-28"))
                .andExpect(jsonPath("$.serie", hasSize(28)));

        mvc.perform(get("/api/v1/reportes/resumen").param("tipo", "ANIO").param("fecha", "2026-06-01")
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie", hasSize(12)))
                .andExpect(jsonPath("$.serie[0].etiqueta").value("ene"));
    }

    // ===================== Alertas por nivel =====================

    @Test
    @DisplayName("las alertas de vencimiento se clasifican en crítico, medio y bajo")
    void alertasPorNivel() throws Exception {
        long producto = productoConStock("1.00", 1);
        LocalDate hoy = LocalDate.now();
        entrada(producto, "L-CRIT", hoy.plusDays(10), 5);
        entrada(producto, "L-MED", hoy.plusDays(45), 5);
        entrada(producto, "L-BAJ", hoy.plusDays(80), 5);

        MvcResult r = mvc.perform(get("/api/v1/alertas/vencimientos").param("dias", "90").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn();
        String json = cuerpo(r);
        assertThat(nivelDe(json, "L-CRIT")).isEqualTo("CRITICO");
        assertThat(nivelDe(json, "L-MED")).isEqualTo("MEDIO");
        assertThat(nivelDe(json, "L-BAJ")).isEqualTo("BAJO");
        // Todo lote vencido que siga en el estante es crítico.
        java.util.List<String> nivelesVencidos = JsonPath.read(json, "$[?(@.vencido == true)].nivelAlerta");
        assertThat(nivelesVencidos).allMatch("CRITICO"::equals);

        mvc.perform(get("/api/v1/alertas/resumen").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotesCriticos").isNumber())
                .andExpect(jsonPath("$.lotesMedios").isNumber())
                .andExpect(jsonPath("$.lotesBajos").isNumber());
    }

    // ===================== Apoyo =====================

    private static String nivelDe(String json, String numeroLote) {
        java.util.List<String> niveles = JsonPath.read(json, "$[?(@.numeroLote == '" + numeroLote + "')].nivelAlerta");
        return niveles.isEmpty() ? null : niveles.get(0);
    }

    private long productoConStock(String precio, int cantidad) throws Exception {
        MvcResult categorias = mvc.perform(get("/api/v1/categorias").header("Authorization", admin)).andReturn();
        long categoria = ((Number) JsonPath.read(cuerpo(categorias), "$[0].id")).longValue();
        String codigo = "POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long producto = idDe(mvc.perform(post("/api/v1/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","nombre":"Producto POS %s","precioVenta":%s,
                                 "stockMinimo":0,"requiereReceta":false,"categoriaId":%d}
                                """.formatted(codigo, codigo, precio, categoria)))
                .andExpect(status().isCreated()).andReturn());
        entrada(producto, "L-" + codigo, LocalDate.now().plusDays(300), cantidad);
        return producto;
    }

    private void entrada(long producto, String lote, LocalDate vence, int cantidad) throws Exception {
        mvc.perform(post("/api/v1/inventario/entradas").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":%d,\"numeroLote\":\"%s\",\"fechaVencimiento\":\"%s\",\"cantidad\":%d}"
                                .formatted(producto, lote, vence, cantidad)))
                .andExpect(status().isCreated());
    }

    private int stockDisponible(long producto) throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/inventario/stock/" + producto).header("Authorization", admin))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(cuerpo(r), "$.stockDisponible");
    }

    private String bearer(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(cuerpo(r), "$.token");
    }

    private static String venta(long producto, int cantidad, String pagos, String tipo, Long cliente) {
        StringBuilder json = new StringBuilder("{\"detalles\":[{\"productoId\":%d,\"cantidad\":%d}]".formatted(producto, cantidad));
        json.append(pagos == null ? ",\"medioPago\":\"EFECTIVO\"" : ",\"pagos\":" + pagos);
        if (tipo != null) json.append(",\"tipoComprobante\":\"").append(tipo).append("\"");
        if (cliente != null) json.append(",\"clienteId\":").append(cliente);
        return json.append("}").toString();
    }

    private static String cuerpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static long idDe(MvcResult r) throws Exception {
        return ((Number) JsonPath.read(cuerpo(r), "$.id")).longValue();
    }
}
