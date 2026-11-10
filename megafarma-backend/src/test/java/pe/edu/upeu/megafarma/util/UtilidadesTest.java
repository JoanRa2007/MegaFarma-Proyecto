package pe.edu.upeu.megafarma.util;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilidadesTest {

    // constante de la clase: un solo valor compartido que no cambia
    private static final Set<String> CAMPOS = Set.of("id", "nombre");

    // @Test: marca el metodo como un caso de prueba
    @Test
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("la paginacion corrige valores fuera de rango")
    void paginacionSegura() {
        Pageable pageable = PaginacionUtil.crear(-3, 5000, "nombre", "desc", CAMPOS, "id");

        // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("nombre").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("un campo de orden no permitido se reemplaza por el campo por defecto")
    void campoDeOrdenNoPermitido() {
        Pageable pageable = PaginacionUtil.crear(0, 20, "passwordHash", "asc", CAMPOS, "id");

        assertThat(pageable.getSort().getOrderFor("passwordHash")).isNull();
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @DisplayName("el patron de busqueda va en minusculas y escapa comodines")
    void patronDeBusqueda() {
        assertThat(PaginacionUtil.patron(null)).isEqualTo("%");
        assertThat(PaginacionUtil.patron("   ")).isEqualTo("%");
        assertThat(PaginacionUtil.patron(" ParaCetamol ")).isEqualTo("%paracetamol%");
        assertThat(PaginacionUtil.patron("100%")).isEqualTo("%100\\%%");
    }

    @Test
    @DisplayName("el rango de fechas incluye completo el ultimo dia")
    void rangoDeFechas() {
        RangoFechas rango = RangoFechas.de(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));

        assertThat(rango.inicio()).isEqualTo("2026-11-01T00:00:00");
        assertThat(rango.fin()).isEqualTo("2026-11-11T00:00:00");
    }

    @Test
    @DisplayName("sin fechas usa del primer dia del mes hasta hoy")
    void rangoPorDefecto() {
        RangoFechas rango = RangoFechas.de(null, null);

        // fecha de hoy segun el reloj del servidor
        assertThat(rango.hasta()).isEqualTo(LocalDate.now());
        assertThat(rango.desde()).isEqualTo(LocalDate.now().withDayOfMonth(1));
    }

    @Test
    @DisplayName("rechaza un rango invertido")
    void rangoInvertido() {
        // comprueba que el codigo lanza la excepcion esperada
        assertThatThrownBy(() ->
                RangoFechas.de(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
