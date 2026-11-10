package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.dto.ProductoRequestDTO;
import pe.edu.upeu.megafarma.dto.ProductoResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface ProductoService {

    PaginaResponseDTO<ProductoResponseDTO> listar(String q, boolean incluirInactivos,
                                                  int pagina, int tamanio,
                                                  String ordenarPor, String direccion);

    ProductoResponseDTO buscar(Long id);

    List<ProductoResponseDTO> cambiosDesde(LocalDateTime desde);

    ProductoResponseDTO crear(ProductoRequestDTO request);

    ProductoResponseDTO actualizar(Long id, ProductoRequestDTO request);

    void desactivar(Long id);

    ProductoResponseDTO activar(Long id);
}
