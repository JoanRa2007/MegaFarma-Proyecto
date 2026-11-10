package pe.edu.upeu.megafarma.dto;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class ClienteRequestDTO {

    // @NotBlank: obligatorio: no puede ser null, vacio ni solo espacios
    @NotBlank(message = "El tipo de documento es obligatorio")
    // @Pattern: el texto debe cumplir la expresion regular
    @Pattern(regexp = "DNI|RUC|CE", message = "El tipo de documento debe ser DNI, RUC o CE")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "^[0-9A-Za-z]{8,15}$",
            message = "El número de documento debe tener entre 8 y 15 caracteres alfanuméricos")
    private String numeroDocumento;

    @NotBlank(message = "El nombre es obligatorio")
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
    private String nombre;

    @Size(max = 20, message = "El teléfono admite hasta 20 caracteres")
    private String telefono;
}
