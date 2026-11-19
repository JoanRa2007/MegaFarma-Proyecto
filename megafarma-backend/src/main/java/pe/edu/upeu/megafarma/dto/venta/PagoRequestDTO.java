package pe.edu.upeu.megafarma.dto.venta;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Una forma de pago enviada por la app. Una venta puede traer varias.
 */

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.MedioPago;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequestDTO {

    @NotNull(message = "El medio de pago es obligatorio")
    private MedioPago medio;

    /* En efectivo puede ser mayor que lo que falta: la diferencia es el vuelto. */
    @NotNull(message = "El monto del pago es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
    private BigDecimal monto;

    /* Yape, Plin y transferencia: numero de operacion. Tarjeta: ultimos 4 digitos. */
    @Size(max = 40, message = "La referencia admite hasta 40 caracteres")
    private String referencia;
}
