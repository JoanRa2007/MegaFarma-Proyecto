package pe.edu.upeu.megafarma.dto.inventario;

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

import java.util.List;

// @Getter: Lombok genera los metodos getX() de todos los atributos al compilar
@Getter
// @Setter: Lombok genera los metodos setX(valor) de todos los atributos
@Setter
// @NoArgsConstructor: Lombok genera el constructor vacio (lo necesitan JPA y el conversor JSON)
@NoArgsConstructor
// @AllArgsConstructor: Lombok genera un constructor con todos los atributos, en el orden declarado
@AllArgsConstructor
public class StockProductoDTO {
    private Long productoId;
    private String codigo;
    private String nombre;
    /* Suma de todos los lotes no dados de baja (incluye vencidos sin retirar). */
    private int stockRegistrado;
    /* Lo que se puede vender hoy. */
    private int stockDisponible;
    private int stockMinimo;
    private boolean bajoMinimo;
    private List<LoteResponseDTO> lotes;
}
