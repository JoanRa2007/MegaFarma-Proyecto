package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// ArgumentCaptor: captura el objeto con que se llamo al mock para revisarlo
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.upeu.megafarma.dto.venta.DetalleVentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaResponseDTO;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.MedioPago;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.repository.ClienteRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.VentaRepository;
import pe.edu.upeu.megafarma.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Pruebas unitarias de las reglas de venta. Los repositorios son mocks:
 * no se levanta Spring ni base de datos, solo se prueba la logica.
 */
// @ExtendWith: activa Mockito para crear los objetos simulados (@Mock)
@ExtendWith(MockitoExtension.class)
// @MockitoSettings: LENIENT: no falla si un when(...) preparado no llega a usarse
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceImplTest {

    // @Mock: objeto simulado: no toca la base; devuelve lo que se le indique con when(...)
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private UsuarioActual usuarioActual;

    private VentaServiceImpl service;

    private Producto paracetamol;
    private Lote loteCercano;
    private Lote loteLejano;

    // @BeforeEach: se ejecuta antes de CADA prueba para dejar todo en estado inicial
    @BeforeEach
    void setUp() {
        service = new VentaServiceImpl(ventaRepository, clienteRepository, productoRepository,
                loteRepository, movimientoRepository, usuarioActual);

        Usuario vendedor = new Usuario();
        vendedor.setId(7L);
        vendedor.setUsername("vendedor");
        // when(...).thenReturn(x): cuando se llame a ese metodo del mock, devolver x
        when(usuarioActual.obtener()).thenReturn(vendedor);

        paracetamol = producto(1L, "Paracetamol 500 mg", "2.50");
        // El lote cercano vence antes: FEFO debe usarlo primero.
        loteCercano = lote(10L, paracetamol, "L-CERCA", LocalDate.now().plusDays(10), 10);
        // fecha de hoy segun el reloj del servidor
        loteLejano = lote(11L, paracetamol, "L-LEJOS", LocalDate.now().plusDays(300), 50);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(paracetamol));
        when(loteRepository.lotesVendiblesFefo(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(loteCercano, loteLejano));
        when(loteRepository.buscarParaActualizar(10L)).thenReturn(Optional.of(loteCercano));
        when(loteRepository.buscarParaActualizar(11L)).thenReturn(Optional.of(loteLejano));

        // Simula el id que asigna la base al insertar.
        // thenAnswer: calcula la respuesta del mock a partir del argumento recibido
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta venta = inv.getArgument(0);
            venta.setId(500L);
            return venta;
        });
    }

    // @Nested: agrupa pruebas relacionadas dentro de la clase
    @Nested
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("Registrar una venta")
    class Registrar {

        // @Test: marca el metodo como un caso de prueba
        @Test
        @DisplayName("descuenta del lote que vence primero (FEFO)")
        void usaElLoteQueVencePrimero() {
            VentaResponseDTO response = service.registrar(request(linea(1L, 4)));

            // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
            assertThat(response.getId()).isEqualTo(500L);
            assertThat(response.getNumero()).isEqualTo("V-00000500");
            assertThat(response.getEstado()).isEqualTo("REGISTRADA");
            assertThat(response.getVendedor()).isEqualTo("vendedor");
            assertThat(response.getTotal()).isEqualByComparingTo("10.00");
            assertThat(response.getDetalles()).hasSize(1);
            assertThat(response.getDetalles().get(0).getNumeroLote()).isEqualTo("L-CERCA");

            assertThat(loteCercano.getCantidadActual()).isEqualTo(6);
            assertThat(loteLejano.getCantidadActual()).isEqualTo(50);
        }

        @Test
        @DisplayName("si el primer lote no alcanza, completa con el siguiente")
        void reparteEntreLotes() {
            VentaResponseDTO response = service.registrar(request(linea(1L, 15)));

            assertThat(response.getDetalles()).hasSize(2);
            assertThat(response.getDetalles().get(0).getNumeroLote()).isEqualTo("L-CERCA");
            assertThat(response.getDetalles().get(0).getCantidad()).isEqualTo(10);
            assertThat(response.getDetalles().get(1).getNumeroLote()).isEqualTo("L-LEJOS");
            assertThat(response.getDetalles().get(1).getCantidad()).isEqualTo(5);
            assertThat(response.getTotal()).isEqualByComparingTo("37.50");

            assertThat(loteCercano.getCantidadActual()).isZero();
            assertThat(loteLejano.getCantidadActual()).isEqualTo(45);
        }

        @Test
        @DisplayName("deja un movimiento de salida en el kardex por cada lote usado")
        void registraMovimientosDeKardex() {
            service.registrar(request(linea(1L, 15)));

            // @SuppressWarnings: silencia una advertencia del compilador que aqui no es un problema
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MovimientoInventario>> captor = ArgumentCaptor.forClass(List.class);
            // verify: comprueba que el metodo del mock se llamo (o que nunca se llamo, con never())
            verify(movimientoRepository).saveAll(captor.capture());

            List<MovimientoInventario> movimientos = captor.getValue();
            assertThat(movimientos).hasSize(2);
            assertThat(movimientos).allSatisfy(m -> {
                assertThat(m.getTipo()).isEqualTo(TipoMovimiento.SALIDA_VENTA);
                assertThat(m.getReferencia()).isEqualTo("V-00000500");
            });
            assertThat(movimientos.get(0).getCantidad()).isEqualTo(10);
            assertThat(movimientos.get(0).getStockResultante()).isZero();
            assertThat(movimientos.get(1).getCantidad()).isEqualTo(5);
            assertThat(movimientos.get(1).getStockResultante()).isEqualTo(45);
        }

        @Test
        @DisplayName("el precio lo pone el servidor, no el cliente")
        void usaElPrecioDelProducto() {
            service.registrar(request(linea(1L, 2)));

            ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
            verify(ventaRepository).save(captor.capture());

            DetalleVenta detalle = captor.getValue().getDetalles().get(0);
            assertThat(detalle.getPrecioUnitario()).isEqualByComparingTo("2.50");
            assertThat(detalle.getSubtotal()).isEqualByComparingTo("5.00");
            assertThat(captor.getValue().getMedioPago()).isEqualTo(MedioPago.EFECTIVO);
        }

        @Test
        @DisplayName("suma las lineas repetidas del mismo producto")
        void sumaLineasRepetidas() {
            VentaResponseDTO response = service.registrar(request(linea(1L, 3), linea(1L, 4)));

            assertThat(response.getDetalles()).hasSize(1);
            assertThat(response.getDetalles().get(0).getCantidad()).isEqualTo(7);
            assertThat(loteCercano.getCantidadActual()).isEqualTo(3);
        }

        @Test
        @DisplayName("rechaza la venta si el stock no alcanza y no descuenta nada")
        void stockInsuficiente() {
            // comprueba que el codigo lanza la excepcion esperada
            assertThatThrownBy(() -> service.registrar(request(linea(1L, 61))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Stock insuficiente")
                    .hasMessageContaining("disponible 60")
                    .hasMessageContaining("solicitado 61");

            assertThat(loteCercano.getCantidadActual()).isEqualTo(10);
            assertThat(loteLejano.getCantidadActual()).isEqualTo(50);
            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("no vende lotes vencidos: sin lotes vendibles no hay stock")
        void sinLotesVendibles() {
            // El repositorio ya excluye vencidos y dados de baja.
            when(loteRepository.lotesVendiblesFefo(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.registrar(request(linea(1L, 1))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("disponible 0");
        }

        @Test
        @DisplayName("rechaza un producto que no existe")
        void productoInexistente() {
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrar(request(linea(99L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza un producto desactivado")
        void productoDesactivado() {
            paracetamol.setActivo(false);

            assertThatThrownBy(() -> service.registrar(request(linea(1L, 1))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("desactivado");
        }

        @Test
        @DisplayName("rechaza un cliente que no existe")
        void clienteInexistente() {
            VentaRequestDTO request = request(linea(1L, 1));
            request.setClienteId(404L);
            when(clienteRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrar(request))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("reenviar la misma venta (misma clave) no la duplica")
        void idempotencia() {
            Venta existente = ventaExistente();
            when(ventaRepository.findByClaveIdempotencia("clave-1"))
                    .thenReturn(Optional.of(existente));

            VentaRequestDTO request = request(linea(1L, 4));
            request.setClaveIdempotencia("clave-1");

            VentaResponseDTO response = service.registrar(request);

            assertThat(response.getId()).isEqualTo(900L);
            assertThat(loteCercano.getCantidadActual()).isEqualTo(10);
            verify(ventaRepository, never()).save(any());
            verify(movimientoRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Anular una venta")
    class Anular {

        @Test
        @DisplayName("devuelve cada unidad al lote del que salio")
        void devuelveElStock() {
            Venta venta = ventaExistente();
            loteCercano.setCantidadActual(6);
            when(ventaRepository.findById(900L)).thenReturn(Optional.of(venta));

            VentaResponseDTO response = service.anular(900L, "Error de digitación");

            assertThat(response.getEstado()).isEqualTo("ANULADA");
            assertThat(response.getMotivoAnulacion()).isEqualTo("Error de digitación");
            assertThat(response.getAnuladaEn()).isNotNull();
            assertThat(loteCercano.getCantidadActual()).isEqualTo(10);
            assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        }

        @Test
        @DisplayName("no permite anular dos veces")
        void yaAnulada() {
            Venta venta = ventaExistente();
            venta.setEstado(EstadoVenta.ANULADA);
            when(ventaRepository.findById(900L)).thenReturn(Optional.of(venta));

            assertThatThrownBy(() -> service.anular(900L, "Otra vez"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("ya está anulada");

            assertThat(loteCercano.getCantidadActual()).isEqualTo(10);
        }

        @Test
        @DisplayName("responde no encontrado si la venta no existe")
        void ventaInexistente() {
            when(ventaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.anular(1L, "x"))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // ---------- datos de apoyo ----------

    private Venta ventaExistente() {
        Usuario vendedor = new Usuario();
        vendedor.setUsername("vendedor");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(paracetamol);
        detalle.setLote(loteCercano);
        detalle.setCantidad(4);
        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        detalle.setPrecioUnitario(new BigDecimal("2.50"));
        detalle.setSubtotal(new BigDecimal("10.00"));

        Venta venta = new Venta();
        venta.setId(900L);
        venta.setNumero("V-00000900");
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setMedioPago(MedioPago.EFECTIVO);
        venta.setUsuario(vendedor);
        venta.setTotal(new BigDecimal("10.00"));
        venta.agregarDetalle(detalle);
        return venta;
    }

    private static VentaRequestDTO request(DetalleVentaRequestDTO... lineas) {
        VentaRequestDTO request = new VentaRequestDTO();
        request.setMedioPago(MedioPago.EFECTIVO);
        request.setDetalles(new ArrayList<>(List.of(lineas)));
        return request;
    }

    private static DetalleVentaRequestDTO linea(Long productoId, int cantidad) {
        return new DetalleVentaRequestDTO(productoId, cantidad);
    }

    private static Producto producto(Long id, String nombre, String precio) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Analgésicos");

        Producto producto = new Producto();
        producto.setId(id);
        producto.setCodigo("MED-" + id);
        producto.setNombre(nombre);
        producto.setPrecioVenta(new BigDecimal(precio));
        producto.setCategoria(categoria);
        producto.setActivo(true);
        return producto;
    }

    private static Lote lote(Long id, Producto producto, String numero, LocalDate vence, int cantidad) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setNumeroLote(numero);
        lote.setFechaVencimiento(vence);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        return lote;
    }
}
