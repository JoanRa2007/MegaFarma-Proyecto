package pe.edu.upeu.megafarma.util;

/*
 * PARA ESTUDIAR · UTILIDADES (util)
 * Funciones de apoyo reutilizadas por varios servicios.
 */

import pe.edu.upeu.megafarma.exception.SolicitudInvalidaException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * Rango de fechas de una consulta. Si no llega "desde" se usa el primer dia
 * del mes actual y si no llega "hasta" se usa hoy. En las consultas se usa
 * [inicio, fin): desde las 00:00 de "desde" hasta las 00:00 del dia
 * siguiente a "hasta", para incluir todo el ultimo dia.
 */
// record: clase inmutable; Java genera constructor y metodos de acceso
public record RangoFechas(LocalDate desde, LocalDate hasta) {

    public static RangoFechas de(LocalDate desde, LocalDate hasta) {
        // fecha de hoy segun el reloj del servidor
        LocalDate hoy = LocalDate.now(); // zona horaria de Lima (ver ZonaHorariaConfig)
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.withDayOfMonth(1) : desde;

        if (inicio.isAfter(fin)) {
            // Sprint 2 · CP-S2-48: un rango invertido es una solicitud invalida -> HTTP 400
            throw new SolicitudInvalidaException(
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        return new RangoFechas(inicio, fin);
    }

    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    public LocalDateTime fin() {
        return hasta.plusDays(1).atStartOfDay();
    }
}
