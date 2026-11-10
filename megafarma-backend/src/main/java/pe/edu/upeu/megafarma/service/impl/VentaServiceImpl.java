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
import pe.edu.upeu.megafarma.dto.venta.DetalleVentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaResponseDTO;
import pe.edu.upeu.megafarma.entity.Cliente;
import pe.edu.upeu.megafarma.entity.DetalleVenta;
import pe.edu.upeu.megafarma.entity.Lote;
import pe.edu.upeu.megafarma.entity.MovimientoInventario;
import pe.edu.upeu.megafarma.entity.Producto;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.EstadoVenta;
import pe.edu.upeu.megafarma.enums.TipoMovimiento;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.VentaMapper;
import pe.edu.upeu.megafarma.repository.ClienteRepository;
import pe.edu.upeu.megafarma.repository.LoteRepository;
import pe.edu.upeu.megafarma.repository.MovimientoInventarioRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.repository.VentaRepository;
import pe.edu.upeu.megafarma.security.UsuarioActual;
import pe.edu.upeu.megafarma.service.VentaService;
import pe.edu.upeu.megafarma.util.PaginacionUtil;
import pe.edu.upeu.megafarma.util.RangoFechas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class VentaServiceImpl implements VentaService {

    // logger: escribe mensajes en la consola/archivo de log de la aplicacion
    private static final Logger log = LoggerFactory.getLogger(VentaServiceImpl.class);

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioActual usuarioActual;

    public VentaServiceImpl(VentaRepository ventaRepository,
                            ClienteRepository clienteRepository,
                            ProductoRepository productoRepository,
                            LoteRepository loteRepository,
                            MovimientoInventarioRepository movimientoRepository,
                            UsuarioActual usuarioActual) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioActual = usuarioActual;
    }

    /*
     * HU-09 Registrar una venta.
     *
     *  - HU-29 FEFO: cada producto sale del lote que vence primero; si ese
     *    lote no alcanza, se completa con el siguiente.
     *  - HU-15: los lotes vencidos o dados de baja nunca se venden.
     *  - El precio lo pone el servidor (precio de venta del producto), no
     *    el cliente: la app no puede alterar el importe.
     *  - Todo ocurre en una transaccion: si un producto no tiene stock
     *    suficiente no se descuenta nada de ninguno.
     *  - Idempotencia: reenviar la misma venta devuelve la ya registrada.
     */
    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {

        // PASO 1 · Idempotencia: si esta misma venta ya llego antes, se devuelve la que ya existe.
        String clave = limpiar(request.getClaveIdempotencia());
        if (clave != null) {
            // Optional: caja que puede traer un valor o venir vacia; evita trabajar con null
            Optional<Venta> existente = ventaRepository.findByClaveIdempotencia(clave);
            if (existente.isPresent()) {
                // deja registro de la operacion en el log
                log.info("Venta repetida ignorada | clave={} venta={}",
                        clave, existente.get().getNumero());
                return VentaMapper.toResponse(existente.get());
            }
        }

        // PASO 2 · Quien vende: el usuario dueño del token JWT de la peticion.
        Usuario vendedor = usuarioActual.obtener();

        // PASO 3 · Cliente (opcional): si se envio un id debe existir; si no, venta de mostrador.
        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el cliente con id " + request.getClienteId()));
        }

        // PASO 4 · Juntar cantidades: si el mismo producto llega en dos lineas se suma en una sola.
        // Mapa productoId -> cantidad total. LinkedHashMap conserva el orden en que llegaron.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleVentaRequestDTO linea : request.getDetalles()) {
            // merge: si el producto no estaba lo agrega; si ya estaba, suma la cantidad nueva.
            cantidades.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }

        // fecha de hoy segun el reloj del servidor
        LocalDate hoy = LocalDate.now();

        // PASO 5 · Cabecera de la venta (todavia en memoria, aun no se guarda).
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setMedioPago(request.getMedioPago());
        venta.setCliente(cliente);
        venta.setUsuario(vendedor);
        venta.setClaveIdempotencia(clave);

        // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
        BigDecimal total = BigDecimal.ZERO;
        // Movimientos de kardex que se guardaran al final, uno por cada lote usado.
        List<MovimientoInventario> movimientos = new ArrayList<>();

        // PASO 6 · Por cada producto pedido: validar, buscar lotes y descontar con FEFO.
        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {

            Long productoId = linea.getKey();      // que producto
            int solicitado = linea.getValue();     // cuantas unidades pide el cliente

            // 6.1 El producto debe existir (si no, 404).
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + productoId));

            // 6.2 Un producto desactivado no se vende.
            if (!producto.isActivo()) {
                // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
                throw new ReglaNegocioException(
                        "El producto '" + producto.getNombre() + "' está desactivado y no se puede vender");
            }

            // 6.3 FEFO: lotes vendibles (con stock, no vencidos, no dados de baja) ordenados
            //     del que vence primero al que vence ultimo. La consulta ademas BLOQUEA esas
            //     filas para que otra venta simultanea no descuente el mismo stock.
            List<Lote> lotes = loteRepository.lotesVendiblesFefo(productoId, hoy);
            // stream: recorre la coleccion aplicando map/filter y junta el resultado
            // Aqui: suma la cantidad de todos los lotes = stock disponible del producto.
            int disponible = lotes.stream().mapToInt(Lote::getCantidadActual).sum();

            // 6.4 Si no alcanza se cancela TODA la venta (la excepcion provoca el rollback).
            if (disponible < solicitado) {
                throw new ReglaNegocioException(
                        "Stock insuficiente para '" + producto.getNombre()
                                + "': disponible " + disponible + ", solicitado " + solicitado);
            }

            // 6.5 Repartir lo pedido entre los lotes, empezando por el que vence primero.
            //     Ejemplo: piden 15, lote A tiene 10 y lote B tiene 50 -> 10 de A y 5 de B.
            int pendiente = solicitado;                 // unidades que aun falta despachar
            for (Lote lote : lotes) {
                if (pendiente == 0) {                   // ya se completo el pedido: no tocar mas lotes
                    break;
                }
                // Se toma lo que falta o lo que tenga el lote, lo que sea menor.
                int tomar = Math.min(pendiente, lote.getCantidadActual());
                // Descuenta el stock del lote. No hace falta llamar a save(): la entidad esta
                // "administrada" por JPA y el UPDATE se envia solo al cerrar la transaccion.
                lote.setCantidadActual(lote.getCantidadActual() - tomar);
                pendiente -= tomar;                     // falta menos por despachar

                // Importe de esta linea = precio del producto x unidades, redondeado a 2 decimales.
                BigDecimal subtotal = producto.getPrecioVenta()
                        .multiply(BigDecimal.valueOf(tomar))
                        .setScale(2, RoundingMode.HALF_UP);

                // Una linea de detalle por cada lote usado: asi se sabe de que lote salio cada unidad.
                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setLote(lote);
                detalle.setCantidad(tomar);
                detalle.setPrecioUnitario(producto.getPrecioVenta());
                detalle.setSubtotal(subtotal);
                venta.agregarDetalle(detalle);

                total = total.add(subtotal);            // acumula el total de la venta

                // Deja la salida en el kardex (quien, cuando, cuanto y stock que quedo en el lote).
                movimientos.add(movimiento(lote, TipoMovimiento.SALIDA_VENTA, tomar,
                        "Venta", vendedor));
            }
        }

        // PASO 7 · Guardar. save() inserta la venta y, por el cascade, tambien sus detalles.
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        // Con el id que asigno la base se arma el numero legible: id 1 -> "V-00000001".
        guardada.setNumero(String.format("V-%08d", guardada.getId()));

        // PASO 8 · Guardar el kardex, enlazando cada movimiento con el numero de la venta.
        movimientos.forEach(m -> m.setReferencia(guardada.getNumero()));
        movimientoRepository.saveAll(movimientos);

        log.info("Venta registrada | numero={} total={} vendedor={}",
                guardada.getNumero(), total, vendedor.getUsername());

        // PASO 9 · Responder: la entidad se convierte en DTO y el controlador la devuelve como JSON.
        return VentaMapper.toResponse(guardada);
    }

    /* HU-10 Consultar el detalle de una venta. */
    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public VentaResponseDTO buscar(Long id) {
        return VentaMapper.toResponse(obtener(id));
    }

    /* HU-11 Buscar ventas por fecha (las mas recientes primero). */
    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<VentaResponseDTO> listar(LocalDate desde, LocalDate hasta,
                                                      int pagina, int tamanio) {

        RangoFechas rango = RangoFechas.de(desde, hasta);
        Pageable pageable = PaginacionUtil.crear(
                pagina, tamanio, "fecha", "desc", Set.of("fecha"), "fecha");

        Page<Venta> resultado = ventaRepository.porRango(rango.inicio(), rango.fin(), pageable);

        return PaginaResponseDTO.de(resultado,
                resultado.getContent().stream().map(VentaMapper::toResponse).toList());
    }

    /*
     * HU-12 Anular una venta registrada por error. La venta no se borra:
     * queda como ANULADA con su motivo y cada unidad vuelve al mismo lote
     * del que salio.
     */
    @Override
    @Transactional
    public VentaResponseDTO anular(Long id, String motivo) {

        Venta venta = obtener(id);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("La venta " + venta.getNumero() + " ya está anulada");
        }

        Usuario usuario = usuarioActual.obtener();
        List<MovimientoInventario> movimientos = new ArrayList<>();

        for (DetalleVenta detalle : venta.getDetalles()) {
            Lote lote = loteRepository.buscarParaActualizar(detalle.getLote().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el lote con id " + detalle.getLote().getId()));

            lote.setCantidadActual(lote.getCantidadActual() + detalle.getCantidad());

            MovimientoInventario m = movimiento(lote, TipoMovimiento.ANULACION_VENTA,
                    detalle.getCantidad(), "Anulación: " + motivo.trim(), usuario);
            m.setReferencia(venta.getNumero());
            movimientos.add(m);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(motivo.trim());
        venta.setAnuladaEn(LocalDateTime.now());

        Venta guardada = ventaRepository.save(venta);
        movimientoRepository.saveAll(movimientos);

        log.info("Venta anulada | numero={} por={}", venta.getNumero(), usuario.getUsername());

        return VentaMapper.toResponse(guardada);
    }

    private MovimientoInventario movimiento(Lote lote, TipoMovimiento tipo, int cantidad,
                                            String motivo, Usuario usuario) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(lote.getProducto());
        m.setLote(lote);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockResultante(lote.getCantidadActual());
        m.setMotivo(motivo.length() > 250 ? motivo.substring(0, 250) : motivo);
        m.setUsuario(usuario);
        m.setFecha(LocalDateTime.now());
        return m;
    }

    private Venta obtener(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la venta con id " + id));
    }

    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
