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
import pe.edu.upeu.megafarma.dto.inventario.AjusteRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.EntradaRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExtendWith: activa Mockito para crear los objetos simulados (@Mock)
@ExtendWith(MockitoExtension.class)
// @MockitoSettings: LENIENT: no falla si un when(...) preparado no llega a usarse
@MockitoSettings(strictness = Strictness.LENIENT)
class InventarioServiceImplTest {

    // @Mock: objeto simulado: no toca la base; devuelve lo que se le indique con when(...)
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private UsuarioActual usuarioActual;

    private InventarioServiceImpl service;
    private Producto ibuprofeno;
    // fecha de hoy segun el reloj del servidor
    private final LocalDate hoy = LocalDate.now();

    // @BeforeEach: se ejecuta antes de CADA prueba para dejar todo en estado inicial
    @BeforeEach
    void setUp() {
        service = new InventarioServiceImpl(productoRepository, loteRepository,
                movimientoRepository, usuarioActual, 60);

        Usuario almacenero = new Usuario();
        almacenero.setId(3L);
        almacenero.setUsername("almacen");
        // when(...).thenReturn(x): cuando se llame a ese metodo del mock, devolver x
        when(usuarioActual.obtener()).thenReturn(almacenero);

        ibuprofeno = new Producto();
        ibuprofeno.setId(2L);
        ibuprofeno.setCodigo("MED-0002");
        ibuprofeno.setNombre("Ibuprofeno 400 mg");
        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        ibuprofeno.setPrecioVenta(new BigDecimal("0.40"));
        ibuprofeno.setStockMinimo(80);
        ibuprofeno.setActivo(true);

        when(productoRepository.findById(2L)).thenReturn(Optional.of(ibuprofeno));
        // thenAnswer: calcula la respuesta del mock a partir del argumento recibido
        when(loteRepository.save(any(Lote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // @Nested: agrupa pruebas relacionadas dentro de la clase
    @Nested
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("Registrar una entrada por lote")
    class Entradas {

        // @Test: marca el metodo como un caso de prueba
        @Test
        @DisplayName("crea el lote y deja el movimiento de entrada en el kardex")
        void creaLoteNuevo() {
            when(loteRepository.findByProductoIdAndNumeroLote(2L, "L-100"))
                    .thenReturn(Optional.empty());

            LoteResponseDTO response = service.registrarEntrada(entrada(" l-100 ", hoy.plusDays(90), 50));

            // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
            assertThat(response.getNumeroLote()).isEqualTo("L-100");
            assertThat(response.getCantidadActual()).isEqualTo(50);
            assertThat(response.getCantidadInicial()).isEqualTo(50);
            assertThat(response.isVencido()).isFalse();
            assertThat(response.getDiasParaVencer()).isEqualTo(90);

            ArgumentCaptor<MovimientoInventario> captor =
                    ArgumentCaptor.forClass(MovimientoInventario.class);
            // verify: comprueba que el metodo del mock se llamo (o que nunca se llamo, con never())
            verify(movimientoRepository).save(captor.capture());
            assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
            assertThat(captor.getValue().getCantidad()).isEqualTo(50);
            assertThat(captor.getValue().getStockResultante()).isEqualTo(50);
            assertThat(captor.getValue().getUsuario().getUsername()).isEqualTo("almacen");
        }

        @Test
        @DisplayName("mismo lote y misma fecha: suma la cantidad")
        void mismoLoteSuma() {
            Lote existente = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.findByProductoIdAndNumeroLote(2L, "L-100"))
                    .thenReturn(Optional.of(existente));

            LoteResponseDTO response = service.registrarEntrada(entrada("L-100", hoy.plusDays(90), 30));

            assertThat(response.getCantidadActual()).isEqualTo(80);
            assertThat(response.getCantidadInicial()).isEqualTo(80);
        }

        @Test
        @DisplayName("mismo lote con otra fecha de vencimiento: se rechaza")
        void mismoLoteOtraFecha() {
            Lote existente = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.findByProductoIdAndNumeroLote(2L, "L-100"))
                    .thenReturn(Optional.of(existente));

            // comprueba que el codigo lanza la excepcion esperada
            assertThatThrownBy(() -> service.registrarEntrada(entrada("L-100", hoy.plusDays(120), 30)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("ya existe con vencimiento");

            assertThat(existente.getCantidadActual()).isEqualTo(50);
            verify(movimientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("el vencimiento debe ser posterior a hoy")
        void vencimientoHoyOAntes() {
            assertThatThrownBy(() -> service.registrarEntrada(entrada("L-1", hoy, 10)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("posterior a hoy");

            assertThatThrownBy(() -> service.registrarEntrada(entrada("L-1", hoy.minusDays(1), 10)))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(loteRepository, never()).save(any());
        }

        @Test
        @DisplayName("un producto desactivado no recibe mercaderia")
        void productoDesactivado() {
            ibuprofeno.setActivo(false);

            assertThatThrownBy(() -> service.registrarEntrada(entrada("L-1", hoy.plusDays(30), 10)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("desactivado");
        }

        @Test
        @DisplayName("un producto inexistente responde no encontrado")
        void productoInexistente() {
            EntradaRequestDTO request = entrada("L-1", hoy.plusDays(30), 10);
            request.setProductoId(99L);
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarEntrada(request))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("un envio = una entrada: la misma clave no vuelve a sumar stock")
        void idempotencia() {
            Lote existente = lote(20L, "L-100", hoy.plusDays(90), 50);
            MovimientoInventario previo = new MovimientoInventario();
            previo.setLote(existente);
            when(movimientoRepository.findByClaveIdempotencia("uuid-1"))
                    .thenReturn(Optional.of(previo));

            EntradaRequestDTO request = entrada("L-100", hoy.plusDays(90), 50);
            request.setClaveIdempotencia("uuid-1");

            LoteResponseDTO response = service.registrarEntrada(request);

            assertThat(response.getCantidadActual()).isEqualTo(50);
            verify(loteRepository, never()).save(any());
            verify(movimientoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Ajustes y bajas")
    class AjustesYBajas {

        @Test
        @DisplayName("ajuste por conteo menor registra la diferencia como AJUSTE_NEGATIVO")
        void ajusteNegativo() {
            Lote lote = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.buscarParaActualizar(20L)).thenReturn(Optional.of(lote));

            LoteResponseDTO response = service.ajustar(ajuste(20L, 47, "Frascos rotos"));

            assertThat(response.getCantidadActual()).isEqualTo(47);

            ArgumentCaptor<MovimientoInventario> captor =
                    ArgumentCaptor.forClass(MovimientoInventario.class);
            verify(movimientoRepository).save(captor.capture());
            assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.AJUSTE_NEGATIVO);
            assertThat(captor.getValue().getCantidad()).isEqualTo(3);
            assertThat(captor.getValue().getMotivo()).isEqualTo("Frascos rotos");
        }

        @Test
        @DisplayName("ajuste por conteo mayor registra AJUSTE_POSITIVO")
        void ajustePositivo() {
            Lote lote = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.buscarParaActualizar(20L)).thenReturn(Optional.of(lote));

            service.ajustar(ajuste(20L, 55, "Sobrante en conteo"));

            ArgumentCaptor<MovimientoInventario> captor =
                    ArgumentCaptor.forClass(MovimientoInventario.class);
            verify(movimientoRepository).save(captor.capture());
            assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.AJUSTE_POSITIVO);
            assertThat(captor.getValue().getCantidad()).isEqualTo(5);
        }

        @Test
        @DisplayName("sin diferencia no hay ajuste")
        void ajusteSinDiferencia() {
            Lote lote = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.buscarParaActualizar(20L)).thenReturn(Optional.of(lote));

            assertThatThrownBy(() -> service.ajustar(ajuste(20L, 50, "Conteo")))
                    .isInstanceOf(ReglaNegocioException.class);

            verify(movimientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("solo se da de baja un lote vencido")
        void bajaDeLoteVigente() {
            Lote vigente = lote(20L, "L-100", hoy.plusDays(90), 50);
            when(loteRepository.buscarParaActualizar(20L)).thenReturn(Optional.of(vigente));

            assertThatThrownBy(() -> service.darDeBaja(20L, "Vencido"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Solo se puede dar de baja un lote vencido");

            assertThat(vigente.isDadoDeBaja()).isFalse();
        }

        @Test
        @DisplayName("la baja deja el lote en cero y registra BAJA_VENCIMIENTO")
        void bajaDeLoteVencido() {
            Lote vencido = lote(21L, "L-VIEJO", hoy.minusDays(3), 40);
            when(loteRepository.buscarParaActualizar(21L)).thenReturn(Optional.of(vencido));

            LoteResponseDTO response = service.darDeBaja(21L, "Vencido en estante");

            assertThat(response.getCantidadActual()).isZero();
            assertThat(response.isDadoDeBaja()).isTrue();
            assertThat(response.isVencido()).isTrue();

            ArgumentCaptor<MovimientoInventario> captor =
                    ArgumentCaptor.forClass(MovimientoInventario.class);
            verify(movimientoRepository).save(captor.capture());
            assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.BAJA_VENCIMIENTO);
            assertThat(captor.getValue().getCantidad()).isEqualTo(40);
        }

        @Test
        @DisplayName("un lote ya dado de baja no se da de baja otra vez")
        void bajaRepetida() {
            Lote vencido = lote(21L, "L-VIEJO", hoy.minusDays(3), 0);
            vencido.setDadoDeBaja(true);
            when(loteRepository.buscarParaActualizar(21L)).thenReturn(Optional.of(vencido));

            assertThatThrownBy(() -> service.darDeBaja(21L, "Otra vez"))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Consulta de stock")
    class Stock {

        @Test
        @DisplayName("el stock disponible excluye lotes vencidos y dados de baja")
        void stockDisponible() {
            Lote vigente = lote(20L, "L-OK", hoy.plusDays(90), 50);
            Lote vencido = lote(21L, "L-VENCIDO", hoy.minusDays(1), 30);
            Lote deBaja = lote(22L, "L-BAJA", hoy.minusDays(60), 0);
            deBaja.setDadoDeBaja(true);
            when(loteRepository.findByProductoIdOrderByFechaVencimientoAscIdAsc(2L))
                    .thenReturn(List.of(deBaja, vencido, vigente));

            StockProductoDTO stock = service.stockDeProducto(2L);

            assertThat(stock.getStockRegistrado()).isEqualTo(80);
            assertThat(stock.getStockDisponible()).isEqualTo(50);
            assertThat(stock.getStockMinimo()).isEqualTo(80);
            assertThat(stock.isBajoMinimo()).isTrue();
            assertThat(stock.getLotes()).hasSize(3);
        }
    }

    // ---------- datos de apoyo ----------

    private EntradaRequestDTO entrada(String numeroLote, LocalDate vence, int cantidad) {
        EntradaRequestDTO request = new EntradaRequestDTO();
        request.setProductoId(2L);
        request.setNumeroLote(numeroLote);
        request.setFechaVencimiento(vence);
        request.setCantidad(cantidad);
        request.setPrecioCompra(new BigDecimal("0.18"));
        return request;
    }

    private AjusteRequestDTO ajuste(Long loteId, int contada, String motivo) {
        AjusteRequestDTO request = new AjusteRequestDTO();
        request.setLoteId(loteId);
        request.setCantidadContada(contada);
        request.setMotivo(motivo);
        return request;
    }

    private Lote lote(Long id, String numero, LocalDate vence, int cantidad) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setProducto(ibuprofeno);
        lote.setNumeroLote(numero);
        lote.setFechaVencimiento(vence);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        return lote;
    }
}
