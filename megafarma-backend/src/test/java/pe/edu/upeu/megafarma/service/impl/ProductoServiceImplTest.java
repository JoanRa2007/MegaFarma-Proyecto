package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// ArgumentCaptor: captura el objeto con que se llamo al mock para revisarlo
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.upeu.megafarma.dto.ProductoRequestDTO;
import pe.edu.upeu.megafarma.dto.ProductoResponseDTO;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.repository.CategoriaRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;

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
class ProductoServiceImplTest {

    // @Mock: objeto simulado: no toca la base; devuelve lo que se le indique con when(...)
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private LoteRepository loteRepository;

    private ProductoServiceImpl service;
    private Categoria analgesicos;

    // @BeforeEach: se ejecuta antes de CADA prueba para dejar todo en estado inicial
    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(productoRepository, categoriaRepository, loteRepository);

        analgesicos = new Categoria();
        analgesicos.setId(1L);
        analgesicos.setNombre("Analgésicos");

        // when(...).thenReturn(x): cuando se llame a ese metodo del mock, devolver x
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(analgesicos));
        // thenAnswer: calcula la respuesta del mock a partir del argumento recibido
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto producto = inv.getArgument(0);
            if (producto.getId() == null) {
                producto.setId(100L);
            }
            return producto;
        });
        when(loteRepository.stockVendiblePorProducto(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 75L}));
    }

    // @Test: marca el metodo como un caso de prueba
    @Test
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("registra un producto y normaliza el codigo a mayusculas")
    void crear() {
        when(productoRepository.findByCodigoIgnoreCase("MED-9000")).thenReturn(Optional.empty());

        ProductoResponseDTO response = service.crear(request(" med-9000 ", "  Aspirina 100 mg "));

        // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getCodigo()).isEqualTo("MED-9000");
        assertThat(response.getNombre()).isEqualTo("Aspirina 100 mg");
        assertThat(response.getCategoriaNombre()).isEqualTo("Analgésicos");
        assertThat(response.isActivo()).isTrue();
        assertThat(response.getStockDisponible()).isZero();
    }

    @Test
    @DisplayName("rechaza un codigo que ya existe")
    void codigoDuplicado() {
        when(productoRepository.findByCodigoIgnoreCase("MED-9000"))
                .thenReturn(Optional.of(existente()));

        // comprueba que el codigo lanza la excepcion esperada
        assertThatThrownBy(() -> service.crear(request("MED-9000", "Otro")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("MED-9000");

        // verify: comprueba que el metodo del mock se llamo (o que nunca se llamo, con never())
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechaza una categoria que no existe")
    void categoriaInexistente() {
        ProductoRequestDTO request = request("MED-9001", "Otro");
        request.setCategoriaId(77L);
        when(productoRepository.findByCodigoIgnoreCase("MED-9001")).thenReturn(Optional.empty());
        when(categoriaRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("77");
    }

    @Test
    @DisplayName("actualizar conserva el id y permite mantener su propio codigo")
    void actualizar() {
        Producto existente = existente();
        when(productoRepository.findById(100L)).thenReturn(Optional.of(existente));
        when(productoRepository.findByCodigoIgnoreCase("MED-9000")).thenReturn(Optional.of(existente));

        ProductoRequestDTO request = request("MED-9000", "Aspirina 500 mg");
        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        request.setPrecioVenta(new BigDecimal("0.90"));

        ProductoResponseDTO response = service.actualizar(100L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getNombre()).isEqualTo("Aspirina 500 mg");
        assertThat(response.getPrecioVenta()).isEqualByComparingTo("0.90");
        assertThat(response.getStockDisponible()).isEqualTo(75);
    }

    @Test
    @DisplayName("actualizar rechaza el codigo de otro producto")
    void actualizarConCodigoAjeno() {
        Producto otro = existente();
        otro.setId(200L);
        when(productoRepository.findById(100L)).thenReturn(Optional.of(existente()));
        when(productoRepository.findByCodigoIgnoreCase("MED-9000")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.actualizar(100L, request("MED-9000", "X")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("desactivar es un borrado logico: el producto queda inactivo")
    void desactivar() {
        Producto existente = existente();
        when(productoRepository.findById(100L)).thenReturn(Optional.of(existente));

        service.desactivar(100L);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
        verify(productoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("buscar un id inexistente responde no encontrado")
    void buscarInexistente() {
        when(productoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private Producto existente() {
        Producto producto = new Producto();
        producto.setId(100L);
        producto.setCodigo("MED-9000");
        producto.setNombre("Aspirina 100 mg");
        producto.setPrecioVenta(new BigDecimal("0.50"));
        producto.setCategoria(analgesicos);
        producto.setActivo(true);
        return producto;
    }

    private ProductoRequestDTO request(String codigo, String nombre) {
        ProductoRequestDTO request = new ProductoRequestDTO();
        request.setCodigo(codigo);
        request.setNombre(nombre);
        request.setPrincipioActivo("Ácido acetilsalicílico");
        request.setPrecioVenta(new BigDecimal("0.50"));
        request.setStockMinimo(20);
        request.setCategoriaId(1L);
        return request;
    }
}
