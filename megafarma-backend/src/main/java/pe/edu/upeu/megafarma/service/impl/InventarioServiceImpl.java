package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.inventario.AjusteRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.EntradaRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.ResumenAlertasDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.InventarioMapper;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.security.UsuarioActual;
import pe.edu.upeu.megafarma.service.InventarioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class InventarioServiceImpl implements InventarioService {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(InventarioServiceImpl.class);

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioActual usuarioActual;
    private final int diasAlertaPorDefecto;

    public InventarioServiceImpl(ProductoRepository productoRepository,
                                 LoteRepository loteRepository,
                                 MovimientoInventarioRepository movimientoRepository,
                                 UsuarioActual usuarioActual,
                                 // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
                                 @Value("${app.inventario.dias-alerta-vencimiento:60}") int diasAlertaPorDefecto) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioActual = usuarioActual;
        this.diasAlertaPorDefecto = diasAlertaPorDefecto;
    }

    /*
     * HU-05 Registrar entradas por lote.
     *  - El vencimiento debe ser posterior a hoy.
     *  - Lote repetido: mismo numero y misma fecha suma la cantidad;
     *    mismo numero con otra fecha se rechaza (seria otro lote).
     *  - Un envio = una entrada: si llega la misma clave de idempotencia
     *    no se vuelve a sumar el stock.
     */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional
    public LoteResponseDTO registrarEntrada(EntradaRequestDTO request) {

        // fecha de hoy segun el reloj del servidor
        LocalDate hoy = LocalDate.now();
        String clave = limpiar(request.getClaveIdempotencia());

        if (clave != null) {
            // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
            Optional<MovimientoInventario> previo = movimientoRepository.findByClaveIdempotencia(clave);
            if (previo.isPresent()) {
                // deja registro de la operacion en el log
                log.info("Entrada repetida ignorada | clave={}", clave);
                return InventarioMapper.toResponse(previo.get().getLote(), hoy);
            }
        }

        Producto producto = obtenerProducto(request.getProductoId());
        if (!producto.isActivo()) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException(
                    "El producto '" + producto.getNombre() + "' está desactivado y no recibe mercadería");
        }
        if (!request.getFechaVencimiento().isAfter(hoy)) {
            throw new ReglaNegocioException("La fecha de vencimiento debe ser posterior a hoy");
        }

        String numeroLote = request.getNumeroLote().trim().toUpperCase();
        int cantidad = request.getCantidad();

        Lote lote = loteRepository.findByProductoIdAndNumeroLote(producto.getId(), numeroLote)
                .orElse(null);

        if (lote == null) {
            lote = new Lote();
            lote.setProducto(producto);
            lote.setNumeroLote(numeroLote);
            lote.setFechaVencimiento(request.getFechaVencimiento());
            lote.setCantidadInicial(cantidad);
            lote.setCantidadActual(cantidad);
            lote.setPrecioCompra(request.getPrecioCompra());
        } else {
            if (!lote.getFechaVencimiento().equals(request.getFechaVencimiento())) {
                throw new ReglaNegocioException(
                        "El lote " + numeroLote + " ya existe con vencimiento "
                                + lote.getFechaVencimiento() + ". Revise la fecha o use otro número de lote");
            }
            if (lote.isDadoDeBaja()) {
                throw new ReglaNegocioException(
                        "El lote " + numeroLote + " fue dado de baja y no admite más entradas");
            }
            lote.setCantidadInicial(lote.getCantidadInicial() + cantidad);
            lote.setCantidadActual(lote.getCantidadActual() + cantidad);
            if (request.getPrecioCompra() != null) {
                lote.setPrecioCompra(request.getPrecioCompra());
            }
        }

        Lote guardado = loteRepository.save(lote);

        MovimientoInventario movimiento = nuevoMovimiento(
                guardado, TipoMovimiento.ENTRADA, cantidad, "Entrada de mercadería");
        movimiento.setClaveIdempotencia(clave);
        movimientoRepository.save(movimiento);

        log.info("Entrada registrada | producto={} lote={} cantidad={} stockLote={}",
                producto.getCodigo(), numeroLote, cantidad, guardado.getCantidadActual());

        return InventarioMapper.toResponse(guardado, hoy);
    }

    /* Stock de todos los productos activos, con sus lotes con existencias. */
    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public List<StockProductoDTO> stockGeneral() {

        LocalDate hoy = LocalDate.now();

        Map<Long, List<Lote>> lotesPorProducto = new HashMap<>();
        for (Lote lote : loteRepository.conStock()) {
            lotesPorProducto
                    .computeIfAbsent(lote.getProducto().getId(), id -> new ArrayList<>())
                    .add(lote);
        }

        // stream: recorre la coleccion aplicando map/filter y junta el resultado
        return productoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(p -> InventarioMapper.toStock(
                        p, lotesPorProducto.getOrDefault(p.getId(), List.of()), hoy))
                .toList();
    }

    /* HU-06 Consultar el stock por producto y lote. */
    @Override
    @Transactional(readOnly = true)
    public StockProductoDTO stockDeProducto(Long productoId) {
        Producto producto = obtenerProducto(productoId);
        List<Lote> lotes = loteRepository.findByProductoIdOrderByFechaVencimientoAscIdAsc(productoId);
        return InventarioMapper.toStock(producto, lotes, LocalDate.now());
    }

    /*
     * HU-07 Registrar ajustes de inventario. Se informa la cantidad contada
     * en el estante y el sistema registra la diferencia en el kardex.
     */
    @Override
    @Transactional
    public LoteResponseDTO ajustar(AjusteRequestDTO request) {

        Lote lote = obtenerLoteParaActualizar(request.getLoteId());
        if (lote.isDadoDeBaja()) {
            throw new ReglaNegocioException("El lote fue dado de baja y no se puede ajustar");
        }

        int diferencia = request.getCantidadContada() - lote.getCantidadActual();
        if (diferencia == 0) {
            throw new ReglaNegocioException(
                    "La cantidad contada es igual al stock registrado: no hay nada que ajustar");
        }

        lote.setCantidadActual(request.getCantidadContada());
        Lote guardado = loteRepository.save(lote);

        TipoMovimiento tipo = diferencia > 0
                ? TipoMovimiento.AJUSTE_POSITIVO
                : TipoMovimiento.AJUSTE_NEGATIVO;
        movimientoRepository.save(
                nuevoMovimiento(guardado, tipo, Math.abs(diferencia), request.getMotivo().trim()));

        log.info("Ajuste de inventario | lote={} diferencia={}", lote.getNumeroLote(), diferencia);

        return InventarioMapper.toResponse(guardado, LocalDate.now());
    }

    /*
     * HU-16 Registrar la baja de un lote vencido. Solo aplica a lotes ya
     * vencidos; otras perdidas se registran con un ajuste. El lote queda
     * en cero y marcado como dado de baja.
     */
    @Override
    @Transactional
    public LoteResponseDTO darDeBaja(Long loteId, String motivo) {

        LocalDate hoy = LocalDate.now();
        Lote lote = obtenerLoteParaActualizar(loteId);

        if (lote.isDadoDeBaja()) {
            throw new ReglaNegocioException("El lote ya fue dado de baja");
        }
        if (!lote.estaVencido(hoy)) {
            throw new ReglaNegocioException(
                    "Solo se puede dar de baja un lote vencido. Este lote vence el "
                            + lote.getFechaVencimiento());
        }

        int cantidad = lote.getCantidadActual();
        lote.setCantidadActual(0);
        lote.setDadoDeBaja(true);
        Lote guardado = loteRepository.save(lote);

        movimientoRepository.save(
                nuevoMovimiento(guardado, TipoMovimiento.BAJA_VENCIMIENTO, cantidad, motivo.trim()));

        log.info("Baja por vencimiento | lote={} cantidad={}", lote.getNumeroLote(), cantidad);

        return InventarioMapper.toResponse(guardado, hoy);
    }

    /* HU-37 Kardex: historial de movimientos de un producto, del mas reciente al mas antiguo. */
    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> kardex(Long productoId) {
        obtenerProducto(productoId);
        return movimientoRepository.kardex(productoId).stream()
                .map(InventarioMapper::toResponse)
                .toList();
    }

    /* HU-08 Productos cuyo stock disponible llego al minimo o esta por debajo. */
    @Override
    @Transactional(readOnly = true)
    public List<StockProductoDTO> alertasStockMinimo() {
        return stockGeneral().stream()
                .filter(StockProductoDTO::isBajoMinimo)
                .toList();
    }

    /* HU-13 Lotes con stock que vencen dentro de N dias (incluye los ya vencidos). */
    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> alertasVencimiento(Integer dias) {
        LocalDate hoy = LocalDate.now();
        return loteRepository.porVencer(hoy.plusDays(diasValidos(dias))).stream()
                .map(l -> InventarioMapper.toResponse(l, hoy))
                .toList();
    }

    /* HU-41 Contadores de alertas para el panel de inicio. */
    @Override
    @Transactional(readOnly = true)
    public ResumenAlertasDTO resumenAlertas(Integer dias) {
        int diasValidos = diasValidos(dias);
        List<LoteResponseDTO> lotes = alertasVencimiento(diasValidos);
        int vencidos = (int) lotes.stream().filter(LoteResponseDTO::isVencido).count();
        return new ResumenAlertasDTO(
                alertasStockMinimo().size(),
                lotes.size() - vencidos,
                vencidos,
                diasValidos);
    }

    private int diasValidos(Integer dias) {
        if (dias == null) {
            return diasAlertaPorDefecto;
        }
        if (dias < 0 || dias > 730) {
            throw new ReglaNegocioException("El parámetro 'dias' debe estar entre 0 y 730");
        }
        return dias;
    }

    private MovimientoInventario nuevoMovimiento(Lote lote, TipoMovimiento tipo,
                                                 int cantidad, String motivo) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(lote.getProducto());
        movimiento.setLote(lote);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(lote.getCantidadActual());
        movimiento.setMotivo(motivo);
        movimiento.setUsuario(usuarioActual.obtener());
        movimiento.setFecha(LocalDateTime.now());
        return movimiento;
    }

    private Producto obtenerProducto(Long id) {
        return productoRepository.findById(id)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));
    }

    private Lote obtenerLoteParaActualizar(Long id) {
        return loteRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el lote con id " + id));
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
