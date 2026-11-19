package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Sprint 2 · R04: la botica opera en Lima. Se fija la zona horaria de la
 * aplicacion para que "hoy", los vencimientos y las ventas cerca de la
 * medianoche pertenezcan al mismo dia en la app, el servidor y la base,
 * aunque el servidor este configurado con otra zona.
 */

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

// @Configuration: clase de configuracion que Spring lee al arrancar
@Configuration
public class ZonaHorariaConfig {

    // @Value: inyecta app.zona-horaria de application.yaml (por defecto America/Lima)
    @Value("${app.zona-horaria:America/Lima}")
    private String zonaHoraria;

    /* Se ejecuta una vez al crear esta configuracion, antes de atender peticiones. */
    @PostConstruct
    void fijarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone(zonaHoraria));
    }
}
