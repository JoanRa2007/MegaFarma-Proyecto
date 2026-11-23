package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Historial de ventas de demostracion: genera las ventas de los ultimos meses
 * (con sus detalles, pagos y comprobantes) para que el dashboard y los reportes
 * muestren graficos con datos reales de la base.
 *
 *  - Solo se activa con app.historial-demo=true (variable APP_HISTORIAL_DEMO=true).
 *  - Se carga una sola vez: las ventas llevan la clave "demo-..." y, si ya existen,
 *    no se vuelve a generar nada.
 *  - No descuenta stock: son ventas pasadas y el stock actual ya es el que se ve.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.entity.Cliente;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.Pago;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.MedioPago;
import pe.edu.upeu.megafarma.enums.TipoComprobante;
import pe.edu.upeu.megafarma.repository.ClienteRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;
import pe.edu.upeu.megafarma.repository.VentaRepository;
import pe.edu.upeu.megafarma.service.ComprobanteService;
import pe.edu.upeu.megafarma.util.Igv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.historial-demo", havingValue = "true")
// Corre despues de DatosIniciales (necesita usuarios, productos y lotes).
@Order(2)
public class HistorialDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HistorialDemo.class);

    /* Prefijo de la clave de idempotencia: marca las ventas generadas aqui. */
    private static final String PREFIJO = "demo-";
    private static final int DIAS = 180;
    /* Horas de atencion y su peso: mas ventas a media manana y al salir del trabajo. */
    private static final int[] HORAS = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static final int[] PESOS = {2, 4, 6, 6, 5, 3, 3, 3, 4, 6, 7, 7, 5, 2};

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ComprobanteService comprobanteService;

    /* Semilla fija: el historial sale igual en cada base nueva. */
    private final Random azar = new Random(2026);

    public HistorialDemo(VentaRepository ventaRepository, ProductoRepository productoRepository,
                         LoteRepository loteRepository, UsuarioRepository usuarioRepository,
                         ClienteRepository clienteRepository, ComprobanteService comprobanteService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.comprobanteService = comprobanteService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (ventaRepository.existsByClaveIdempotenciaStartingWith(PREFIJO)) {
            log.info("Historial de demostración ya cargado");
            return;
        }

        // Productos activos que tienen al menos un lote, con su lote principal.
        Map<Producto, Lote> catalogo = new HashMap<>();
        for (Producto p : productoRepository.findAll()) {
            if (!p.isActivo()) {
                continue;
            }
            List<Lote> lotes = loteRepository.findByProductoIdOrderByFechaVencimientoAscIdAsc(p.getId());
            if (!lotes.isEmpty()) {
                catalogo.put(p, lotes.get(lotes.size() - 1));
            }
        }
        List<Usuario> vendedores = usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .filter(u -> !"ALMACENERO".equals(u.getRol().getNombre()))
                .toList();
        if (catalogo.isEmpty() || vendedores.isEmpty()) {
            log.warn("Historial de demostración omitido: faltan productos con lotes o vendedores");
            return;
        }
        List<Producto> productos = new ArrayList<>(catalogo.keySet());
        productos.sort((a, b) -> a.getCodigo().compareTo(b.getCodigo()));

        List<Cliente> empresas = clientes("RUC", new String[][]{
                {"20600000011", "Consultorio Médico Santa Rosa S.A.C."},
                {"20600000029", "Clínica Dental Sonrisas E.I.R.L."},
                {"20600000037", "Transportes del Norte S.A."}});
        List<Cliente> personas = clientes("DNI", new String[][]{
                {"70000001", "María Quispe Huamán"},
                {"70000002", "José Ramírez Torres"},
                {"70000003", "Lucía Fernández Rojas"},
                {"70000004", "Carlos Mendoza Vargas"}});

        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        int creadas = 0;
        for (int atras = DIAS; atras >= 0; atras--) {
            LocalDate dia = hoy.minusDays(atras);
            for (int i = 0; i < ventasDelDia(dia, atras); i++) {
                LocalDateTime fecha = dia.atTime(hora(), azar.nextInt(60), azar.nextInt(60));
                if (fecha.isAfter(ahora)) {
                    continue;
                }
                crearVenta(fecha, productos, catalogo, vendedores, empresas, personas, ++creadas);
            }
        }
        log.info("Historial de demostración cargado: {} ventas", creadas);
    }

    /* Mas ventas los sabados, menos los domingos, y una ligera subida en los ultimos meses. */
    private int ventasDelDia(LocalDate dia, int atras) {
        int base = 6 + (int) Math.round(4.0 * (DIAS - atras) / DIAS);
        if (dia.getDayOfWeek() == DayOfWeek.SATURDAY) {
            base += 4;
        } else if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            base -= 3;
        }
        return Math.max(1, base + azar.nextInt(5) - 2);
    }

    private int hora() {
        int total = 0;
        for (int p : PESOS) {
            total += p;
        }
        int r = azar.nextInt(total);
        for (int i = 0; i < HORAS.length; i++) {
            r -= PESOS[i];
            if (r < 0) {
                return HORAS[i];
            }
        }
        return 12;
    }

    private void crearVenta(LocalDateTime fecha, List<Producto> productos, Map<Producto, Lote> catalogo,
                            List<Usuario> vendedores, List<Cliente> empresas, List<Cliente> personas, int n) {
        Venta venta = new Venta();
        venta.setFecha(fecha);
        venta.setUsuario(vendedores.get(azar.nextInt(vendedores.size())));
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setClaveIdempotencia(PREFIJO + n);

        // De 1 a 4 productos distintos; las tabletas se venden por varias unidades.
        Set<Producto> elegidos = new LinkedHashSet<>();
        int lineas = 1 + azar.nextInt(azar.nextInt(10) < 6 ? 2 : 4);
        while (elegidos.size() < Math.min(lineas, productos.size())) {
            elegidos.add(productos.get(azar.nextInt(productos.size())));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Producto p : elegidos) {
            boolean unitario = p.getPrecioVenta().compareTo(new BigDecimal("1.50")) < 0;
            int cantidad = unitario ? 2 + azar.nextInt(19) : 1 + azar.nextInt(3);
            BigDecimal subtotal = p.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
            DetalleVenta d = new DetalleVenta();
            d.setProducto(p);
            d.setLote(catalogo.get(p));
            d.setCantidad(cantidad);
            d.setPrecioUnitario(p.getPrecioVenta());
            d.setSubtotal(subtotal);
            venta.agregarDetalle(d);
            total = total.add(subtotal);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        venta.setTotal(total);
        venta.setSubtotal(venta.getDetalles().stream()
                .map(d -> Igv.valorSinIgv(d.getSubtotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        venta.setIgv(total.subtract(venta.getSubtotal()));

        // Comprobante: algunas facturas a empresas y boletas con DNI.
        TipoComprobante tipo = TipoComprobante.BOLETA;
        int r = azar.nextInt(100);
        if (r < 10 && !empresas.isEmpty()) {
            tipo = TipoComprobante.FACTURA;
            venta.setCliente(empresas.get(azar.nextInt(empresas.size())));
        } else if (r < 25 && !personas.isEmpty()) {
            venta.setCliente(personas.get(azar.nextInt(personas.size())));
        }

        pagar(venta, total);

        Venta guardada = ventaRepository.save(venta);
        guardada.setNumero(String.format("V-%08d", guardada.getId()));
        comprobanteService.emitir(guardada, tipo);

        // Unas pocas ventas anuladas, para que el indicador no quede siempre en cero.
        if (azar.nextInt(100) < 3) {
            guardada.setEstado(EstadoVenta.ANULADA);
            guardada.setMotivoAnulacion("El cliente desistió de la compra");
            guardada.setAnuladaEn(fecha.plusMinutes(10));
            comprobanteService.anular(guardada.getId());
        }
    }

    /* Medios de pago como en una botica de barrio: sobre todo efectivo y Yape. */
    private void pagar(Venta venta, BigDecimal total) {
        int r = azar.nextInt(100);
        if (r < 48) {
            BigDecimal recibido = redondearBillete(total);
            venta.agregarPago(pago(MedioPago.EFECTIVO, total, null));
            venta.setMontoRecibido(recibido);
            venta.setVuelto(recibido.subtract(total));
            venta.setMedioPago(MedioPago.EFECTIVO);
        } else if (r < 70) {
            unSoloPago(venta, MedioPago.YAPE, total, operacion());
        } else if (r < 80) {
            unSoloPago(venta, MedioPago.PLIN, total, operacion());
        } else if (r < 90) {
            unSoloPago(venta, MedioPago.TARJETA, total, String.format("%04d", azar.nextInt(10000)));
        } else if (r < 94 || total.compareTo(new BigDecimal("2.00")) < 0) {
            unSoloPago(venta, MedioPago.TRANSFERENCIA, total, operacion());
        } else {
            // Pago mixto: una parte en efectivo y el resto con Yape.
            BigDecimal efectivo = total.divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN).max(BigDecimal.ONE);
            venta.agregarPago(pago(MedioPago.YAPE, total.subtract(efectivo), operacion()));
            venta.agregarPago(pago(MedioPago.EFECTIVO, efectivo, null));
            venta.setMontoRecibido(efectivo.setScale(2, RoundingMode.HALF_UP));
            venta.setVuelto(BigDecimal.ZERO.setScale(2));
            venta.setMedioPago(MedioPago.MIXTO);
        }
    }

    private void unSoloPago(Venta venta, MedioPago medio, BigDecimal total, String referencia) {
        venta.agregarPago(pago(medio, total, referencia));
        venta.setMedioPago(medio);
    }

    private static Pago pago(MedioPago medio, BigDecimal monto, String referencia) {
        Pago p = new Pago();
        p.setMedio(medio);
        p.setMonto(monto.setScale(2, RoundingMode.HALF_UP));
        p.setReferencia(referencia);
        return p;
    }

    /* Billete con el que paga el cliente: el siguiente multiplo de 5, 10, 20, 50 o 100. */
    private BigDecimal redondearBillete(BigDecimal total) {
        int[] billetes = {5, 10, 20, 50, 100, 200};
        for (int b : billetes) {
            if (total.compareTo(BigDecimal.valueOf(b)) <= 0 && azar.nextBoolean()) {
                return BigDecimal.valueOf(b).setScale(2);
            }
        }
        return total.setScale(0, RoundingMode.CEILING).setScale(2);
    }

    private String operacion() {
        return String.valueOf(10_000_000 + azar.nextInt(89_999_999));
    }

    /* Busca o crea los clientes de demostracion. */
    private List<Cliente> clientes(String tipo, String[][] datos) {
        List<Cliente> lista = new ArrayList<>();
        for (String[] d : datos) {
            Cliente c = clienteRepository.findByNumeroDocumento(d[0]).orElseGet(() -> {
                Cliente nuevo = new Cliente();
                nuevo.setTipoDocumento(tipo);
                nuevo.setNumeroDocumento(d[0]);
                nuevo.setNombre(d[1]);
                return clienteRepository.save(nuevo);
            });
            lista.add(c);
        }
        return lista;
    }
}
