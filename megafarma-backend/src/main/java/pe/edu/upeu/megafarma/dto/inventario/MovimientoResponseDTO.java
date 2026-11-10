package pe.edu.upeu.megafarma.dto.inventario;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
// @AllArgsConstructor: Lombok genera un constructor con todos los atributos, en el orden declarado
@AllArgsConstructor
public class MovimientoResponseDTO {
    private Long id;
    private LocalDateTime fecha;
    private String tipo;
    private Long productoId;
    private String productoNombre;
    private Long loteId;
    private String numeroLote;
    private int cantidad;
    private int stockResultante;
    private String motivo;
    private String referencia;
    private String usuario;
}
