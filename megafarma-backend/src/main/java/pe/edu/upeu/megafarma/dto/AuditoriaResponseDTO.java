package pe.edu.upeu.megafarma.dto;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Registro de la bitacora tal como lo ve el administrador.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponseDTO {
    private Long id;
    private LocalDateTime fecha;
    private String usuario;
    private String accion;
    private String detalle;
    private String ip;
    private boolean exito;
}
