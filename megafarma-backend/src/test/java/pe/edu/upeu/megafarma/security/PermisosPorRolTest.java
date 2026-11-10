package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.megafarma.enums.Permiso;

import static org.assertj.core.api.Assertions.assertThat;

class PermisosPorRolTest {

    // @Test: marca el metodo como un caso de prueba
    @Test
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("ADMIN tiene todos los permisos")
    void admin() {
        // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
        assertThat(PermisosPorRol.de("ADMIN")).containsExactlyInAnyOrder(Permiso.values());
    }

    @Test
    @DisplayName("VENDEDOR vende y consulta, pero no anula, no mueve stock ni ve reportes")
    void vendedor() {
        assertThat(PermisosPorRol.de("VENDEDOR"))
                .contains(Permiso.VENTAS_REGISTRAR, Permiso.VENTAS_LEER,
                        Permiso.CATALOGO_LEER, Permiso.INVENTARIO_LEER)
                .doesNotContain(Permiso.VENTAS_ANULAR, Permiso.INVENTARIO_ESCRIBIR,
                        Permiso.CATALOGO_ESCRIBIR, Permiso.REPORTES_VER,
                        Permiso.USUARIOS_ADMINISTRAR);
    }

    @Test
    @DisplayName("ALMACENERO gestiona catalogo e inventario, pero no vende")
    void almacenero() {
        assertThat(PermisosPorRol.de("ALMACENERO"))
                .contains(Permiso.CATALOGO_ESCRIBIR, Permiso.INVENTARIO_ESCRIBIR)
                .doesNotContain(Permiso.VENTAS_REGISTRAR, Permiso.REPORTES_VER);
    }

    @Test
    @DisplayName("un rol desconocido no tiene permisos")
    void rolDesconocido() {
        assertThat(PermisosPorRol.de("INVITADO")).isEmpty();
        assertThat(PermisosPorRol.authorities("INVITADO"))
                .extracting(Object::toString)
                .containsExactly("ROLE_INVITADO");
    }
}
