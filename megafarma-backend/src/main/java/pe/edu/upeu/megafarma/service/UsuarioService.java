package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Interfaz: lista QUE operaciones ofrece el modulo, sin decir COMO se hacen.
 * El controlador depende de esta interfaz y no de la implementacion (service.impl).
 */

import pe.edu.upeu.megafarma.dto.auth.RolResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;

import java.util.List;

// interface: solo declara metodos; otra clase (o Spring) pone la implementacion
public interface UsuarioService {

    List<UsuarioResponseDTO> listar();

    UsuarioResponseDTO buscar(Long id);

    UsuarioResponseDTO crear(UsuarioRequestDTO request);

    UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO request);

    UsuarioResponseDTO cambiarEstado(Long id, boolean activo);

    List<RolResponseDTO> roles();
}
