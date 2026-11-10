package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaRequestDTO;
import pe.edu.upeu.megafarma.dto.venta.VentaResponseDTO;

import java.time.LocalDate;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface VentaService {

    VentaResponseDTO registrar(VentaRequestDTO request);

    VentaResponseDTO buscar(Long id);

    PaginaResponseDTO<VentaResponseDTO> listar(LocalDate desde, LocalDate hasta,
                                               int pagina, int tamanio);

    VentaResponseDTO anular(Long id, String motivo);
}
