package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Clases que Spring lee al arrancar para armar la aplicacion: seguridad, CORS,
 * Swagger y datos de demostracion.
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// @Configuration: clase de configuracion: sus metodos @Bean crean objetos que Spring administra
@Configuration
public class CorsConfig {

    /*
     * Solo los origenes web declarados pueden llamar a /api/** desde un
     * navegador; nunca el comodin "*". La app movil nativa (Ktor Client)
     * no envia Origin, asi que CORS no la afecta.
     */
    // @Bean: el objeto que devuelve este metodo queda disponible para inyectarse en otras clases
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
            @Value("${app.cors.allowed-origins}") List<String> origenes) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
