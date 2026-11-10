package pe.edu.upeu.megafarma.dto.inventario;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* Cuerpo de las operaciones que solo piden un motivo (baja de lote, anulacion). */
// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class MotivoRequestDTO {

    // @NotBlank: obligatorio: no puede ser null, vacio ni solo espacios
    @NotBlank(message = "El motivo es obligatorio")
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(max = 250, message = "El motivo admite hasta 250 caracteres")
    private String motivo;
}
