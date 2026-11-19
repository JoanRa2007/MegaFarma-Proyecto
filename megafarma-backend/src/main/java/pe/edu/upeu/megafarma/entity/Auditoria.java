package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Un registro de la bitacora de auditoria (HU-45): quien, que, cuando, desde donde y si tuvo exito.
 */

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    private String usuario;

    /* LOGIN_OK, LOGIN_FALLIDO, ACCESO_DENEGADO, VENTA_REGISTRADA, ... */
    private String accion;

    private String detalle;

    private String ip;

    private boolean exito;
}
