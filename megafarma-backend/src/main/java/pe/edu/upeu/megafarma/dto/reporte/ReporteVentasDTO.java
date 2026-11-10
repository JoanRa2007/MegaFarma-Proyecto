package pe.edu.upeu.megafarma.dto.reporte;

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
import java.time.LocalDate;
import java.util.List;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
// @AllArgsConstructor: Lombok genera un constructor con todos los atributos, en el orden declarado
@AllArgsConstructor
public class ReporteVentasDTO {
    private LocalDate desde;
    private LocalDate hasta;
    private int cantidadVentas;
    // BigDecimal: decimales exactos para dinero (double pierde centimos por redondeo)
    private BigDecimal totalVendido;
    private BigDecimal ticketPromedio;
    private int ventasAnuladas;
    private BigDecimal totalAnulado;
    private List<Grupo> porDia;
    private List<Grupo> porMedioPago;

    /* Una fila del reporte: etiqueta (dia o medio de pago), cantidad y total. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Grupo {
        private String etiqueta;
        private int cantidadVentas;
        private BigDecimal total;
    }
}
