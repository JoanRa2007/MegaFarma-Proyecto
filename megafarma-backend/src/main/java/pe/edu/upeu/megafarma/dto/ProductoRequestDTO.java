package pe.edu.upeu.megafarma.dto;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class ProductoRequestDTO {

    // @NotBlank: obligatorio: no puede ser null, vacio ni solo espacios
    @NotBlank(message = "El código es obligatorio")
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(max = 30, message = "El código admite hasta 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
    private String nombre;

    @Size(max = 150, message = "El principio activo admite hasta 150 caracteres")
    private String principioActivo;

    @Size(max = 100, message = "La presentación admite hasta 100 caracteres")
    private String presentacion;

    @Size(max = 100, message = "El laboratorio admite hasta 100 caracteres")
    private String laboratorio;

    // @NotNull: obligatorio: no puede ser null
    @NotNull(message = "El precio de venta es obligatorio")
    // @DecimalMin: valor decimal minimo permitido
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
    // @Digits: cantidad maxima de digitos enteros y decimales
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 2 decimales")
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal precioVenta;

    @NotNull(message = "El stock mínimo es obligatorio")
    // @Min: valor numerico minimo permitido
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    private boolean requiereReceta;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;
}
