package pe.edu.upeu.megafarma.dto.venta;

/*
 * PARA ESTUDIAR · DTO (Data Transfer Object)
 * Objeto que viaja en el JSON de la API. Los *RequestDTO son lo que envia la app
 * (con sus validaciones) y los *ResponseDTO lo que el servidor responde.
 * Se usan en lugar de las entidades para no exponer la base de datos.
 */

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.MedioPago;

import java.util.List;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
public class VentaRequestDTO {

    /* Opcional: una venta de mostrador puede no tener cliente identificado. */
    private Long clienteId;

    /*
     * Medio de pago cuando se paga con uno solo. Si se envia la lista "pagos"
     * (pago mixto o efectivo con vuelto), este campo es opcional.
     */
    private MedioPago medioPago;

    /* Formas de pago. La suma debe cubrir el total; el exceso en efectivo es el vuelto. */
    @Valid
    private List<PagoRequestDTO> pagos;

    /* BOLETA (por defecto) o FACTURA (exige un cliente con RUC). */
    private pe.edu.upeu.megafarma.enums.TipoComprobante tipoComprobante;

    // @NotEmpty: la lista no puede ser null ni estar vacia
    @NotEmpty(message = "La venta debe tener al menos un producto")
    // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
    @Valid
    private List<DetalleVentaRequestDTO> detalles;

    /*
     * UUID que genera la app por cada venta. Si la app reintenta el envio
     * (sincronizacion offline-first), el servidor devuelve la venta ya
     * registrada en lugar de crear otra.
     */
    // @Size: limita la cantidad de caracteres (o de elementos)
    @Size(max = 60, message = "La clave de idempotencia admite hasta 60 caracteres")
    private String claveIdempotencia;
}
