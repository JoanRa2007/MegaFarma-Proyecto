package pe.edu.upeu.megafarma.dto.venta;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
// @AllArgsConstructor: Lombok genera un constructor con todos los atributos, en el orden declarado
@AllArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private String numero;
    private LocalDateTime fecha;
    private String estado;
    private String medioPago;
    private Long clienteId;
    private String clienteNombre;
    private String vendedor;
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal total;
    private String motivoAnulacion;
    private LocalDateTime anuladaEn;
    private List<DetalleVentaResponseDTO> detalles;
}
