package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.CategoriaRequestDTO;
import pe.edu.upeu.megafarma.dto.CategoriaResponseDTO;

import java.util.List;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface CategoriaService {

    List<CategoriaResponseDTO> listar();

    CategoriaResponseDTO buscar(Long id);

    CategoriaResponseDTO crear(CategoriaRequestDTO request);

    CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO request);

    void eliminar(Long id);
}
