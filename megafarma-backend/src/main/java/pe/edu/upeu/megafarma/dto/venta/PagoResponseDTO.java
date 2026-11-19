package pe.edu.upeu.megafarma.dto.venta;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Forma de pago registrada en la venta.
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponseDTO {
    private String medio;
    private BigDecimal monto;
    private String referencia;
}
