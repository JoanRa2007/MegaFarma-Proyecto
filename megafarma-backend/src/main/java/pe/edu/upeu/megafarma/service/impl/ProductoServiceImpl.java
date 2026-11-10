package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.dto.ProductoRequestDTO;
import pe.edu.upeu.megafarma.dto.ProductoResponseDTO;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.ProductoMapper;
import pe.edu.upeu.megafarma.repository.CategoriaRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.service.ProductoService;
import pe.edu.upeu.megafarma.util.PaginacionUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class ProductoServiceImpl implements ProductoService {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    // constante de la clase: un solo valor compartido que no cambia
    private static final Set<String> CAMPOS_ORDEN =
            Set.of("id", "codigo", "nombre", "precioVenta", "actualizadoEn");

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LoteRepository loteRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               LoteRepository loteRepository) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.loteRepository = loteRepository;
    }

    /* HU-03 Buscar un producto por codigo, nombre o principio activo. */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ProductoResponseDTO> listar(String q, boolean incluirInactivos,
                                                         int pagina, int tamanio,
                                                         String ordenarPor, String direccion) {

        Pageable pageable = PaginacionUtil.crear(
                pagina, tamanio, ordenarPor, direccion, CAMPOS_ORDEN, "nombre");

        Page<Producto> resultado = productoRepository.buscar(
                PaginacionUtil.patron(q), incluirInactivos, pageable);

        Map<Long, Integer> stock = stockDisponible();

        // stream: recorre la coleccion aplicando map/filter y junta el resultado
        List<ProductoResponseDTO> contenido = resultado.getContent().stream()
                .map(p -> ProductoMapper.toResponse(p, stock.getOrDefault(p.getId(), 0)))
                .toList();

        return PaginaResponseDTO.de(resultado, contenido);
    }

    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public ProductoResponseDTO buscar(Long id) {
        return toResponse(obtener(id));
    }

    /* Productos creados o modificados despues de una fecha (sincronizacion de la app). */
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> cambiosDesde(LocalDateTime desde) {
        Map<Long, Integer> stock = stockDisponible();
        return productoRepository.findByActualizadoEnAfterOrderByActualizadoEnAsc(desde).stream()
                .map(p -> ProductoMapper.toResponse(p, stock.getOrDefault(p.getId(), 0)))
                .toList();
    }

    /* HU-01 Registrar un producto. El codigo es unico. */
    @Override
    @Transactional
    public ProductoResponseDTO crear(ProductoRequestDTO request) {

        String codigo = request.getCodigo().trim().toUpperCase();
        if (productoRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException("Ya existe un producto con el código '" + codigo + "'");
        }

        Producto producto = new Producto();
        ProductoMapper.copiar(request, producto, obtenerCategoria(request.getCategoriaId()));
        producto.setActivo(true);

        Producto guardado = productoRepository.save(producto);
        // deja registro de la operacion en el log
        log.info("Producto registrado | id={} codigo={}", guardado.getId(), guardado.getCodigo());

        return ProductoMapper.toResponse(guardado, 0);
    }

    /* HU-02 Actualizar los datos de un producto. */
    @Override
    @Transactional
    public ProductoResponseDTO actualizar(Long id, ProductoRequestDTO request) {

        Producto producto = obtener(id);
        String codigo = request.getCodigo().trim().toUpperCase();

        productoRepository.findByCodigoIgnoreCase(codigo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException(
                            "Ya existe un producto con el código '" + codigo + "'");
                });

        ProductoMapper.copiar(request, producto, obtenerCategoria(request.getCategoriaId()));

        return toResponse(productoRepository.save(producto));
    }

    /*
     * HU-04 Desactivar un producto. No se borra: tiene lotes, ventas y
     * movimientos asociados. Desactivado deja de aparecer en las busquedas
     * y no se puede vender ni recibir mercaderia.
     */
    @Override
    @Transactional
    public void desactivar(Long id) {
        Producto producto = obtener(id);
        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Producto desactivado | id={} codigo={}", id, producto.getCodigo());
    }

    @Override
    @Transactional
    public ProductoResponseDTO activar(Long id) {
        Producto producto = obtener(id);
        producto.setActivo(true);
        return toResponse(productoRepository.save(producto));
    }

    private Producto obtener(Long id) {
        return productoRepository.findById(id)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));
    }

    private Categoria obtenerCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría con id " + id));
    }

    private ProductoResponseDTO toResponse(Producto producto) {
        return ProductoMapper.toResponse(
                producto, stockDisponible().getOrDefault(producto.getId(), 0));
    }

    /* productoId -> unidades vendibles hoy. */
    private Map<Long, Integer> stockDisponible() {
        Map<Long, Integer> stock = new HashMap<>();
        // fecha de hoy segun el reloj del servidor
        for (Object[] fila : loteRepository.stockVendiblePorProducto(LocalDate.now())) {
            stock.put((Long) fila[0], ((Number) fila[1]).intValue());
        }
        return stock;
    }
}
