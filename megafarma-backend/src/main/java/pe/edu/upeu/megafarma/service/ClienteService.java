package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.ClienteRequestDTO;
import pe.edu.upeu.megafarma.dto.ClienteResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface ClienteService {

    PaginaResponseDTO<ClienteResponseDTO> listar(String q, int pagina, int tamanio);

    ClienteResponseDTO buscar(Long id);

    ClienteResponseDTO crear(ClienteRequestDTO request);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request);

    void desactivar(Long id);
}
