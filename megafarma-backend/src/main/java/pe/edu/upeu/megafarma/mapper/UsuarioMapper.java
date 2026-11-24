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
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setNombreCompleto(usuario.getNombreCompleto());
        dto.setRol(rol);
        dto.setPermisos(PermisosPorRol.permisos(rol));
        dto.setActivo(usuario.isActivo());
        dto.setBloqueado(usuario.getIntentosFallidos() >= maxIntentosFallidos);
        dto.setCreadoEn(usuario.getCreadoEn());
        dto.setNombres(usuario.getNombres());
        dto.setApellidos(usuario.getApellidos());
        dto.setDni(usuario.getDni());
        dto.setTelefono(usuario.getTelefono());
        dto.setCorreo(usuario.getCorreo());
        dto.setDebeCambiarPassword(usuario.isDebeCambiarPassword());
        return dto;
    }

    public static RolResponseDTO toResponse(Rol rol) {
        return new RolResponseDTO(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                PermisosPorRol.permisos(rol.getNombre()));
    }
}
