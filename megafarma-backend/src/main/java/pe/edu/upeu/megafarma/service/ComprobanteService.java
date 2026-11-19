package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Comprobantes electronicos de SUNAT (boleta y factura) en modo demostracion.
 */

import pe.edu.upeu.megafarma.dto.venta.ComprobanteResponseDTO;
import pe.edu.upeu.megafarma.entity.Venta;
import pe.edu.upeu.megafarma.enums.TipoComprobante;

public interface ComprobanteService {

    /* Emite el comprobante de una venta recien registrada (misma transaccion). */
    void emitir(Venta venta, TipoComprobante tipo);

    ComprobanteResponseDTO obtener(Long ventaId);

    String xml(Long ventaId);

    /* Marca como ANULADO el comprobante de una venta anulada. */
    void anular(Long ventaId);
}
