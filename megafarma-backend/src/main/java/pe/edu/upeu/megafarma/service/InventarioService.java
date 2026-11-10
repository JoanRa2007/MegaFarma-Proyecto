package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.inventario.AjusteRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.EntradaRequestDTO;
import pe.edu.upeu.megafarma.dto.inventario.LoteResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.inventario.ResumenAlertasDTO;
import pe.edu.upeu.megafarma.dto.inventario.StockProductoDTO;

import java.util.List;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface InventarioService {

    LoteResponseDTO registrarEntrada(EntradaRequestDTO request);

    List<StockProductoDTO> stockGeneral();

    StockProductoDTO stockDeProducto(Long productoId);

    LoteResponseDTO ajustar(AjusteRequestDTO request);

    LoteResponseDTO darDeBaja(Long loteId, String motivo);

    List<MovimientoResponseDTO> kardex(Long productoId);

    List<StockProductoDTO> alertasStockMinimo();

    List<LoteResponseDTO> alertasVencimiento(Integer dias);

    ResumenAlertasDTO resumenAlertas(Integer dias);
}
