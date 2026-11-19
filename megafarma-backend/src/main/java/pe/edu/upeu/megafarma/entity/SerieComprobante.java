package pe.edu.upeu.megafarma.entity;

/*
 * PARA ESTUDIAR · CAPA ENTIDAD (entity)
 * Serie de comprobantes (B001 boletas, F001 facturas) con su ultimo numero
 * emitido. El correlativo se incrementa con la fila bloqueada para que dos
 * ventas simultaneas nunca reciban el mismo numero.
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.megafarma.enums.TipoComprobante;

@Entity
@Table(name = "series_comprobante")
@Getter
@Setter
@NoArgsConstructor
public class SerieComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoComprobante tipo;

    private String serie;

    private int ultimoNumero;
}
