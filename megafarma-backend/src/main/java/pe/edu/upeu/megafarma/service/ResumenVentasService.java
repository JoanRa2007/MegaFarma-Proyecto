package pe.edu.upeu.megafarma.service;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - CONTRATO (service)
 * Dashboard de ventas y reportes por dia, mes y año.
 */

import pe.edu.upeu.megafarma.dto.reporte.ResumenVentasDTO;
import pe.edu.upeu.megafarma.enums.Periodo;

import java.time.LocalDate;

public interface ResumenVentasService {

    /* HOY, SEMANA (ultimos 7 dias), MES o ANIO, contados hasta hoy. */
    ResumenVentasDTO dashboard(Periodo periodo);

    /* DIA, MES o ANIO completo que contiene la fecha indicada. */
    ResumenVentasDTO reporte(Periodo tipo, LocalDate fecha, int paginaSerie, int paginaTop, int tamanio);
}
