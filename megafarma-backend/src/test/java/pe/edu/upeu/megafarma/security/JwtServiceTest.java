package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · PRUEBA UNITARIA
 * Prueba una sola clase aislada. Lo que esa clase necesita (repositorios, etc.)
 * se reemplaza por objetos simulados (mocks), asi no hace falta base de datos.
 * Cada @Test sigue el patron: preparar datos -> ejecutar -> comprobar el resultado.
 */

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // constante de la clase: un solo valor compartido que no cambia
    private static final String CLAVE =
            "bWVnYWZhcm1hLWNsYXZlLWRlLXBydWViYXMtdW5pdGFyaWFzLWRlLTMyLWJ5dGVzLW8tbWFz";
    private static final String OTRA_CLAVE =
            "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLWZpcm1hci10b2tlbnMtZmFsc29zLTMyLWJ5dGVz";

    private final UserDetails vendedor = User.withUsername("vendedor")
            .password("x")
            .authorities(PermisosPorRol.authorities(PermisosPorRol.VENDEDOR))
            .build();

    // @Test: marca el metodo como un caso de prueba
    @Test
    // @DisplayName: nombre legible que aparece en el reporte de pruebas
    @DisplayName("el token lleva el usuario y sus permisos")
    void generaYLeeElToken() {
        JwtService jwt = new JwtService(CLAVE, 60);

        String token = jwt.generarToken(vendedor);

        // assertThat(valor): compara el resultado real con el esperado; si difiere la prueba falla
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwt.extraerUsername(token)).isEqualTo("vendedor");

        List<?> authorities = jwt.leerClaims(token).get("authorities", List.class);
        assertThat(authorities).map(Object::toString)
                .contains("ROLE_VENDEDOR", "VENTAS_REGISTRAR")
                .doesNotContain("VENTAS_ANULAR", "USUARIOS_ADMINISTRAR");
        assertThat(jwt.getExpiracionSegundos()).isEqualTo(3600);
    }

    @Test
    @DisplayName("rechaza un token firmado con otra clave")
    void rechazaFirmaAjena() {
        String falso = new JwtService(OTRA_CLAVE, 60).generarToken(vendedor);

        // comprueba que el codigo lanza la excepcion esperada
        assertThatThrownBy(() -> new JwtService(CLAVE, 60).extraerUsername(falso))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rechaza un token alterado")
    void rechazaTokenAlterado() {
        JwtService jwt = new JwtService(CLAVE, 60);
        String token = jwt.generarToken(vendedor);
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwt.extraerUsername(alterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rechaza un token expirado")
    void rechazaTokenExpirado() {
        JwtService jwt = new JwtService(CLAVE, -1);
        String token = jwt.generarToken(vendedor);

        assertThatThrownBy(() -> jwt.extraerUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
