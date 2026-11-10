package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.inventario.MovimientoResponseDTO;
import pe.edu.upeu.megafarma.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteInventarioDTO;
import pe.edu.upeu.megafarma.dto.reporte.ReporteVentasDTO;

import java.time.LocalDate;
import java.util.List;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface ReporteService {

    ReporteVentasDTO ventas(LocalDate desde, LocalDate hasta);

    List<ProductoMasVendidoDTO> masVendidos(LocalDate desde, LocalDate hasta, int limite);

    ReporteInventarioDTO inventario();

    List<MovimientoResponseDTO> bajasPorVencimiento(LocalDate desde, LocalDate hasta);
}
