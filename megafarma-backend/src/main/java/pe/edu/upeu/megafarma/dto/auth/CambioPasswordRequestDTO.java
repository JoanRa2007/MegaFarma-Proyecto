package pe.edu.upeu.megafarma.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* El usuario cambia su propia contrasena: debe enviar la actual y la nueva. */
@Getter
@Setter
@NoArgsConstructor
public class CambioPasswordRequestDTO {

    @NotBlank(message = "Ingrese su contraseña actual")
    private String actual;

    @NotBlank(message = "Ingrese la nueva contraseña")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "La contraseña debe tener al menos una letra y un número")
    private String nueva;
}
