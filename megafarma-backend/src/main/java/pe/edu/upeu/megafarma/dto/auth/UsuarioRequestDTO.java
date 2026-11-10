package pe.edu.upeu.megafarma.dto.auth;

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
public class UsuarioRequestDTO {

    // @NotBlank: obligatorio: no puede ser null, vacio ni solo espacios
    @NotBlank(message = "El usuario es obligatorio")
    // @Pattern: el texto debe cumplir la expresion regular
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
            message = "El usuario debe tener de 3 a 50 letras, números, punto, guion o guion bajo")
    private String username;

    /* Obligatoria al crear; al actualizar se deja vacia para no cambiarla. */
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre completo admite hasta 120 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El rol es obligatorio")
    private String rol;
}
