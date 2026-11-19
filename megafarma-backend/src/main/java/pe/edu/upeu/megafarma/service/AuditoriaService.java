package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Bitacora de auditoria (HU-45).
 */

import pe.edu.upeu.megafarma.dto.AuditoriaResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;

public interface AuditoriaService {

    /* Registra una accion del usuario autenticado. */
    void registrar(String accion, String detalle, boolean exito);

    /* Registra una accion de un usuario indicado (por ejemplo, un login fallido). */
    void registrarDe(String usuario, String accion, String detalle, boolean exito);

    PaginaResponseDTO<AuditoriaResponseDTO> buscar(String usuario, String accion, int pagina, int tamanio);
}
