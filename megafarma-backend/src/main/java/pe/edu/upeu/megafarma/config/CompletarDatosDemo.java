package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Completa los datos de demostracion para que todas las pantallas se vean llenas:
 *  - datos del trabajador (nombres, apellidos, DNI, telefono, correo) de cada cuenta;
 *  - mas trabajadores de ejemplo (vendedores y almacenero);
 *  - telefono de los clientes y mas clientes (personas y empresas);
 *  - mas productos en el catalogo y al menos un lote para cada producto.
 *
 * Solo rellena lo que esta vacio: nunca cambia un dato que alguien ya escribio.
 * Se puede ejecutar en cada arranque sin duplicar nada. Se activa junto con el
 * historial de demostracion (APP_HISTORIALDEMO=true).
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.entity.Cliente;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.repository.CategoriaRepository;
import pe.edu.upeu.megafarma.repository.ClienteRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.RolRepository;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.historial-demo", havingValue = "true")
// Corre despues de DatosIniciales (1) y del historial de ventas (2).
@Order(3)
public class CompletarDatosDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CompletarDatosDemo.class);

    /* Datos de las cuentas de prueba: usuario -> nombres, apellidos, DNI, telefono. */
    private static final Map<String, String[]> TRABAJADORES = Map.of(
            "admin", new String[]{"Carlos Alberto", "Mendoza Ríos", "70451236", "987654321"},
            "vendedor", new String[]{"Lucía Fernanda", "Quispe Huamán", "71234568", "956123478"},
            "almacen", new String[]{"Jorge Luis", "Paredes Salazar", "72345679", "945612378"});

    /* Trabajadores extra: usuario, rol, nombres, apellidos, DNI, telefono. */
    private static final String[][] NUEVOS_TRABAJADORES = {
            {"mrojas", "VENDEDOR", "María Rosa", "Rojas Castillo", "73456780", "934567812"},
            {"dcastro", "VENDEDOR", "Diego Andrés", "Castro Vega", "74567891", "923456781"},
            {"aflores", "ALMACENERO", "Ana Sofía", "Flores Ticona", "75678902", "912345678"}};

    /* Clientes: tipo, documento, nombre, telefono. */
    private static final String[][] CLIENTES = {
            {"DNI", "71000001", "Rosa Elena Vargas Huamán", "981234567"},
            {"DNI", "71000002", "Luis Miguel Torres Quispe", "982345678"},
            {"DNI", "71000003", "Carmen Julia Salazar Ríos", "983456789"},
            {"DNI", "71000004", "Pedro Pablo Ccama Flores", "984567890"},
            {"DNI", "71000005", "Valeria Nicole Huanca Mamani", "985678901"},
            {"DNI", "71000006", "Jorge Enrique Chávez León", "986789012"},
            {"RUC", "20600000045", "Policlínico San Juan S.A.C.", "014567890"},
            {"RUC", "20600000053", "Colegio Adventista Unión E.I.R.L.", "016789012"},
            {"RUC", "20600000061", "Minimarket El Ahorro S.R.L.", "017890123"}};

    /* Productos: codigo, nombre, principio activo, presentacion, laboratorio, precio, stock minimo, receta, categoria. */
    private static final String[][] PRODUCTOS = {
            {"MED-0017", "Ketorolaco 10 mg", "Ketorolaco trometamol", "Caja x 100 tabletas", "Medifarma", "0.60", "40", "no", "Analgésicos"},
            {"MED-0018", "Cefalexina 500 mg", "Cefalexina", "Caja x 100 cápsulas", "Genfar", "0.70", "40", "si", "Antibióticos"},
            {"MED-0019", "Dexametasona 4 mg", "Dexametasona", "Caja x 100 tabletas", "Portugal", "0.50", "30", "si", "Antiinflamatorios"},
            {"MED-0020", "Hidróxido de aluminio y magnesio", "Hidróxido de aluminio + hidróxido de magnesio", "Suspensión 360 ml", "Farmindustria", "12.50", "10", "no", "Gastrointestinal"},
            {"MED-0021", "Panadol Antigripal", "Paracetamol + fenilefrina + clorfenamina", "Caja x 100 tabletas", "GSK", "0.90", "50", "no", "Analgésicos"},
            {"MED-0022", "Multivitamínico adultos", "Multivitaminas y minerales", "Frasco x 30 tabletas", "Hersil", "45.00", "6", "no", "Vitaminas"},
            {"MED-0023", "Sulfato ferroso 300 mg", "Sulfato ferroso", "Caja x 100 tabletas", "Hersil", "0.25", "40", "no", "Vitaminas"},
            {"MED-0024", "Desloratadina 5 mg", "Desloratadina", "Caja x 10 tabletas", "Medifarma", "1.20", "20", "no", "Antialérgicos"},
            {"MED-0025", "Metronidazol 500 mg", "Metronidazol", "Caja x 100 tabletas", "Portugal", "0.35", "40", "si", "Antibióticos"},
            {"DER-0001", "Clotrimazol crema 1 %", "Clotrimazol", "Tubo 20 g", "Genfar", "7.50", "10", "no", "Dermatológicos"},
            {"DER-0002", "Hidrocortisona crema 1 %", "Hidrocortisona", "Tubo 15 g", "Medifarma", "9.00", "8", "no", "Dermatológicos"},
            {"CUI-0005", "Alcohol etílico 96 grados 1 L", null, "Frasco 1 L", "Portugal", "12.00", "10", "no", "Cuidado personal"},
            {"CUI-0006", "Toallitas húmedas x 80", null, "Paquete x 80 unidades", "Babysec", "8.90", "12", "no", "Cuidado personal"},
            {"CUI-0007", "Gasa estéril 10 x 10 cm", null, "Sobre x 10 unidades", "CKF", "2.50", "20", "no", "Primeros auxilios"},
            {"CUI-0008", "Esparadrapo 2.5 cm x 5 m", null, "Rollo", "3M", "4.20", "15", "no", "Primeros auxilios"},
            {"CUI-0009", "Termómetro digital", null, "Unidad", "Omron", "25.00", "5", "no", "Primeros auxilios"}};

    /* Categorias nuevas: nombre, descripcion. */
    private static final String[][] CATEGORIAS = {
            {"Dermatológicos", "Cremas y tratamientos para la piel"},
            {"Primeros auxilios", "Curaciones, vendajes e instrumentos"}};

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PasswordEncoder passwordEncoder;

    public CompletarDatosDemo(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                              ClienteRepository clienteRepository, CategoriaRepository categoriaRepository,
                              ProductoRepository productoRepository, LoteRepository loteRepository,
                              MovimientoInventarioRepository movimientoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        completarTrabajadores();
        crearTrabajadores();
        completarClientes();
        crearCategorias();
        crearProductos();
        crearLotesFaltantes();
        log.info("Datos de demostración completados");
    }

    /* Rellena los datos del trabajador que falten en cada cuenta. */
    private void completarTrabajadores() {
        for (Usuario u : usuarioRepository.findAll()) {
            String[] datos = TRABAJADORES.get(u.getUsername());
            if (datos != null && vacio(u.getNombres()) && vacio(u.getApellidos())) {
                u.setNombres(datos[0]);
                u.setApellidos(datos[1]);
                u.setNombreCompleto(datos[0] + " " + datos[1]);
            } else if (vacio(u.getNombres()) && !vacio(u.getNombreCompleto())) {
                // Separa "Nombre Apellido Apellido": las dos ultimas palabras son los apellidos.
                String[] partes = u.getNombreCompleto().trim().split("\\s+");
                int corte = partes.length > 2 ? partes.length - 2 : Math.max(1, partes.length - 1);
                u.setNombres(String.join(" ", java.util.Arrays.copyOfRange(partes, 0, corte)));
                if (vacio(u.getApellidos()) && corte < partes.length) {
                    u.setApellidos(String.join(" ", java.util.Arrays.copyOfRange(partes, corte, partes.length)));
                }
            }
            if (vacio(u.getDni())) {
                String dni = datos != null ? datos[2] : String.format("76%06d", u.getId() % 1_000_000);
                if (!usuarioRepository.existsByDni(dni)) {
                    u.setDni(dni);
                }
            }
            if (vacio(u.getTelefono())) {
                u.setTelefono(datos != null ? datos[3] : String.format("9%08d", 10_000_000 + u.getId()));
            }
            if (vacio(u.getCorreo())) {
                u.setCorreo(u.getUsername() + "@megafarma.pe");
            }
        }
    }

    /*
     * Crea trabajadores de ejemplo con una contrasena temporal al azar que nadie conoce:
     * para usarlos, el admin les restablece la contrasena desde Usuarios (llave).
     */
    private void crearTrabajadores() {
        SecureRandom azar = new SecureRandom();
        for (String[] t : NUEVOS_TRABAJADORES) {
            if (usuarioRepository.existsByUsername(t[0]) || usuarioRepository.existsByDni(t[4])) {
                continue;
            }
            Usuario u = new Usuario();
            u.setUsername(t[0]);
            u.setRol(rolRepository.findByNombre(t[1]).orElseThrow());
            u.setNombres(t[2]);
            u.setApellidos(t[3]);
            u.setNombreCompleto(t[2] + " " + t[3]);
            u.setDni(t[4]);
            u.setTelefono(t[5]);
            u.setCorreo(t[0] + "@megafarma.pe");
            u.setPasswordHash(passwordEncoder.encode(Long.toString(azar.nextLong(), 36) + "Aa1"));
            u.setDebeCambiarPassword(true);
            u.setActivo(true);
            usuarioRepository.save(u);
        }
    }

    /* Telefono a los clientes que no tienen y mas clientes de ejemplo. */
    private void completarClientes() {
        for (Cliente c : clienteRepository.findAll()) {
            if (vacio(c.getTelefono())) {
                c.setTelefono(String.format("9%08d", 70_000_000 + c.getId() * 137 % 9_999_999));
            }
        }
        for (String[] d : CLIENTES) {
            if (clienteRepository.findByNumeroDocumento(d[1]).isPresent()) {
                continue;
            }
            Cliente c = new Cliente();
            c.setTipoDocumento(d[0]);
            c.setNumeroDocumento(d[1]);
            c.setNombre(d[2]);
            c.setTelefono(d[3]);
            clienteRepository.save(c);
        }
    }

    private void crearCategorias() {
        for (String[] d : CATEGORIAS) {
            if (categoriaRepository.findByNombreIgnoreCase(d[0]).isEmpty()) {
                Categoria c = new Categoria();
                c.setNombre(d[0]);
                c.setDescripcion(d[1]);
                categoriaRepository.save(c);
            }
        }
    }

    private void crearProductos() {
        for (String[] d : PRODUCTOS) {
            if (productoRepository.findByCodigoIgnoreCase(d[0]).isPresent()) {
                continue;
            }
            Categoria categoria = categoriaRepository.findByNombreIgnoreCase(d[8]).orElse(null);
            if (categoria == null) {
                continue;
            }
            Producto p = new Producto();
            p.setCodigo(d[0]);
            p.setNombre(d[1]);
            p.setPrincipioActivo(d[2]);
            p.setPresentacion(d[3]);
            p.setLaboratorio(d[4]);
            p.setPrecioVenta(new BigDecimal(d[5]));
            p.setStockMinimo(Integer.parseInt(d[6]));
            p.setRequiereReceta("si".equals(d[7]));
            p.setCategoria(categoria);
            p.setActivo(true);
            productoRepository.save(p);
        }
    }

    /*
     * Cada producto activo sin lotes recibe dos: uno con vencimiento lejano y otro
     * mas cercano, para que se vean el FEFO y las alertas de vencimiento.
     */
    private void crearLotesFaltantes() {
        Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        int i = 0;
        for (Producto p : productoRepository.findAll()) {
            if (!p.isActivo() || !loteRepository.findByProductoIdOrderByFechaVencimientoAscIdAsc(p.getId()).isEmpty()) {
                continue;
            }
            i++;
            String base = "L-" + p.getCodigo().replace("-", "");
            int cantidad = Math.max(p.getStockMinimo() * 3, 30);
            BigDecimal costo = p.getPrecioVenta().multiply(new BigDecimal("0.55")).setScale(2, java.math.RoundingMode.HALF_UP)
                    .max(new BigDecimal("0.05"));
            crearLote(p, base + "-01", hoy.plusDays(i % 3 == 0 ? 40 : 120 + i * 7), cantidad / 3, costo, admin);
            crearLote(p, base + "-02", hoy.plusDays(420 + i * 11), cantidad, costo, admin);
        }
    }

    private void crearLote(Producto p, String numero, LocalDate vence, int cantidad, BigDecimal costo, Usuario usuario) {
        Lote lote = new Lote();
        lote.setProducto(p);
        lote.setNumeroLote(numero);
        lote.setFechaVencimiento(vence);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        lote.setPrecioCompra(costo);
        loteRepository.save(lote);

        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(p);
        m.setLote(lote);
        m.setTipo(TipoMovimiento.ENTRADA);
        m.setCantidad(cantidad);
        m.setStockResultante(cantidad);
        m.setMotivo("Ingreso de mercadería (demostración)");
        m.setUsuario(usuario);
        m.setFecha(LocalDateTime.now());
        movimientoRepository.save(m);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
