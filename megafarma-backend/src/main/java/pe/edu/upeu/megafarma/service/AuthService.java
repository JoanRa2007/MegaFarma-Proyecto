package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Inicio y cierre de sesion.
 */

import pe.edu.upeu.megafarma.dto.auth.LoginRequestDTO;
import pe.edu.upeu.megafarma.dto.auth.LoginResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request, String ip);

    /* Revoca el token actual: deja de servir aunque no haya expirado. */
    void logout(String token);

    UsuarioResponseDTO perfil();

    void cambiarPassword(String actual, String nueva);
}
