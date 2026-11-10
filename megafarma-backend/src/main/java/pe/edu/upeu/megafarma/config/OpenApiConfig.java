package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Clases que Spring lee al arrancar para armar la aplicacion: seguridad, CORS,
 * Swagger y datos de demostracion.
 */

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration: clase de configuracion: sus metodos @Bean crean objetos que Spring administra
@Configuration
public class OpenApiConfig {

    // constante de la clase: un solo valor compartido que no cambia
    private static final String ESQUEMA = "bearerAuth";

    /*
     * Documentacion en /swagger-ui.html. El boton "Authorize" permite pegar
     * el token que devuelve /api/v1/auth/login y probar las rutas protegidas.
     */
    // @Bean: el objeto que devuelve este metodo queda disponible para inyectarse en otras clases
    @Bean
    public OpenAPI megafarmaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Megafarma API")
                        .version("1.0.0")
                        .description("API REST de ventas e inventario de la botica Megafarma: "
                                + "catálogo, inventario por lotes, ventas con FEFO, alertas, "
                                + "reportes, usuarios y roles."))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
