package pe.edu.upeu.megafarma.dto.inventario;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class EntradaRequestDTO {

    // @NotNull: obligatorio: no puede ser null
    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    // @NotBlank: obligatorio: no puede ser null, vacio ni solo espacios
    @NotBlank(message = "El número de lote es obligatorio")
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(max = 40, message = "El número de lote admite hasta 40 caracteres")
    private String numeroLote;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    @NotNull(message = "La cantidad es obligatoria")
    // @Min: valor numerico minimo permitido
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    // @DecimalMin: valor decimal minimo permitido
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal precioCompra;

    /*
     * Identificador unico que genera la app por cada envio (UUID).
     * Si la misma entrada llega dos veces (reintento sin conexion),
     * la segunda no duplica el stock.
     */
    @Size(max = 60, message = "La clave de idempotencia admite hasta 60 caracteres")
    private String claveIdempotencia;
}
