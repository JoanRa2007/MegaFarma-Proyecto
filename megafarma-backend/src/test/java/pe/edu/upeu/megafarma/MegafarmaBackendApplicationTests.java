package pe.edu.upeu.megafarma;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest: levanta el contexto completo de Spring para la prueba
@SpringBootTest
// @ActiveProfiles: usa application-test.yaml (base H2 en memoria)
@ActiveProfiles("test")
class MegafarmaBackendApplicationTests {

    /* Arranca el contexto completo: Flyway crea el esquema y Hibernate lo valida. */
    // @Test: marca el metodo como un caso de prueba
    @Test
    void contextLoads() {
    }
}
