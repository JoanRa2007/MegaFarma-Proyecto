package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/health")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "0. Estado", description = "Comprobación de que la API está en línea")
public class HealthController {

    /* Publico: la app lo usa para saber si hay conexion con el servidor. */
    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Estado del servicio (público)")
    public Map<String, Object> estado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "OK");
        respuesta.put("servicio", "Megafarma API");
        respuesta.put("fecha", LocalDateTime.now());
        return respuesta;
    }
}
