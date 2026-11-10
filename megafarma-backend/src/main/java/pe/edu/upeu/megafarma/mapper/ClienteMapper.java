package pe.edu.upeu.megafarma.mapper;

/*
 * PARA ESTUDIAR · MAPPER
 * Convierte entre entidades (base de datos) y DTO (JSON de la API).
 */

import pe.edu.upeu.megafarma.dto.ClienteResponseDTO;
import pe.edu.upeu.megafarma.entity.Cliente;

public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.isActivo());
    }
}
