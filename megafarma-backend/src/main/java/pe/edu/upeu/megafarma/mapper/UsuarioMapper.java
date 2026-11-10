package pe.edu.upeu.megafarma.mapper;

/*
 * PARA ESTUDIAR · MAPPER
 * Convierte entre entidades (base de datos) y DTO (JSON de la API).
 */

import pe.edu.upeu.megafarma.dto.auth.RolResponseDTO;
import pe.edu.upeu.megafarma.dto.auth.UsuarioResponseDTO;
import pe.edu.upeu.megafarma.entity.Rol;
import pe.edu.upeu.megafarma.entity.Usuario;
import pe.edu.upeu.megafarma.security.PermisosPorRol;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    /* Nunca se devuelve el hash de la contrasena. */
    public static UsuarioResponseDTO toResponse(Usuario usuario, int maxIntentosFallidos) {
        String rol = usuario.getRol().getNombre();
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                rol,
                PermisosPorRol.permisos(rol),
                usuario.isActivo(),
                usuario.getIntentosFallidos() >= maxIntentosFallidos,
                usuario.getCreadoEn());
    }

    public static RolResponseDTO toResponse(Rol rol) {
        return new RolResponseDTO(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                PermisosPorRol.permisos(rol.getNombre()));
    }
}
