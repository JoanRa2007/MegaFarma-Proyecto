package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Clases que Spring lee al arrancar para armar la aplicacion: seguridad, CORS,
 * Swagger y datos de demostracion.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Rol;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.RolRepository;
import pe.edu.upeu.megafarma.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * Datos de demostracion. Solo actua sobre una base vacia (sin usuarios /
 * sin lotes), asi que reiniciar la aplicacion no duplica nada.
 *
 * Los usuarios se crean aqui y no en Flyway porque la contrasena debe
 * pasar por BCrypt, y los lotes porque sus vencimientos son relativos a
 * la fecha de hoy (para que siempre haya lotes por vencer y vencidos).
 *
 * Se desactiva con app.datos-demo=false (produccion).
 */
// @Component: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Component
// @ConditionalOnProperty: la clase solo se activa si la propiedad tiene ese valor en application.yaml
@ConditionalOnProperty(name = "app.datos-demo", havingValue = "true")
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class DatosIniciales implements CommandLineRunner {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PasswordEncoder passwordEncoder;

    public DatosIniciales(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          ProductoRepository productoRepository,
                          LoteRepository loteRepository,
                          MovimientoInventarioRepository movimientoRepository,
                          PasswordEncoder passwordEncoder) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional
    public void run(String... args) {

        if (usuarioRepository.count() == 0) {
            crearUsuario("admin", "Admin123", "Administrador Megafarma", "ADMIN");
            crearUsuario("vendedor", "Venta123", "Vendedor de Mostrador", "VENDEDOR");
            crearUsuario("almacen", "Almacen123", "Encargado de Almacén", "ALMACENERO");
            // deja registro de la operacion en el log
            log.info("Usuarios de prueba creados: admin, vendedor, almacen");
        }

        if (loteRepository.count() == 0) {
            Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
            if (admin == null) {
                return;
            }
            // fecha de hoy segun el reloj del servidor
            LocalDate hoy = LocalDate.now();

            // Dos lotes del mismo producto: la venta debe salir del que vence antes (FEFO).
            crearLote("MED-0001", "L-PARA-01", hoy.plusDays(20), 150, "0.08", admin);
            crearLote("MED-0001", "L-PARA-02", hoy.plusDays(400), 300, "0.08", admin);
            crearLote("MED-0002", "L-IBU-01", hoy.plusDays(300), 200, "0.18", admin);
            // Por debajo del stock minimo (60): aparece en la alerta de reposicion.
            crearLote("MED-0003", "L-AMOX-01", hoy.plusDays(200), 30, "0.25", admin);
            // Un lote ya vencido (no se vende, se puede dar de baja) y uno vigente.
            crearLote("MED-0005", "L-LORA-01", hoy.minusDays(5), 40, "0.12", admin);
            crearLote("MED-0005", "L-LORA-02", hoy.plusDays(250), 120, "0.12", admin);
            // Vence pronto: aparece en la alerta de vencimientos.
            crearLote("MED-0007", "L-OME-01", hoy.plusDays(45), 180, "0.20", admin);
            crearLote("MED-0006", "L-CETI-01", hoy.plusDays(365), 150, "0.14", admin);
            crearLote("MED-0008", "L-AND-01", hoy.plusDays(365), 90, "0.45", admin);
            crearLote("MED-0009", "L-NAP-01", hoy.plusDays(365), 120, "0.35", admin);
            crearLote("MED-0010", "L-DICLO-01", hoy.plusDays(365), 160, "0.10", admin);
            crearLote("MED-0011", "L-VITC-01", hoy.plusDays(500), 25, "9.50", admin);
            crearLote("MED-0012", "L-COMB-01", hoy.plusDays(365), 110, "0.20", admin);
            crearLote("MED-0014", "L-CLOR-01", hoy.plusDays(365), 140, "0.06", admin);
            crearLote("MED-0015", "L-META-01", hoy.plusDays(365), 130, "0.12", admin);
            crearLote("CUI-0001", "L-ALC-01", hoy.plusDays(700), 60, "2.10", admin);
            crearLote("CUI-0003", "L-MASC-01", hoy.plusDays(700), 400, "0.20", admin);
            crearLote("CUI-0004", "L-SUE-01", hoy.plusDays(180), 48, "3.80", admin);
            log.info("Lotes de demostración creados");
        }
    }

    private void crearUsuario(String username, String password, String nombre, String nombreRol) {
        // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
        Rol rol = rolRepository.findByNombre(nombreRol).orElseThrow();
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        // BCrypt convierte la contrasena en un hash irreversible antes de guardarla
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNombreCompleto(nombre);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    private void crearLote(String codigoProducto, String numeroLote, LocalDate vencimiento,
                           int cantidad, String precioCompra, Usuario usuario) {

        Producto producto = productoRepository.findByCodigoIgnoreCase(codigoProducto).orElse(null);
        if (producto == null) {
            return;
        }

        Lote lote = new Lote();
        lote.setProducto(producto);
        lote.setNumeroLote(numeroLote);
        lote.setFechaVencimiento(vencimiento);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        lote.setPrecioCompra(new BigDecimal(precioCompra));
        loteRepository.save(lote);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setLote(lote);
        movimiento.setTipo(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(cantidad);
        movimiento.setMotivo("Carga inicial de demostración");
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());
        movimientoRepository.save(movimiento);
    }
}
