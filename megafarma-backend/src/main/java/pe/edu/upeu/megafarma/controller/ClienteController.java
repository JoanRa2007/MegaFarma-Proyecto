package pe.edu.upeu.megafarma.controller;

/*
 * PARA ESTUDIAR · CAPA CONTROLADOR (controller)
 * Puerta de entrada HTTP. Recibe la peticion, valida el JSON de entrada (@Valid),
 * llama al servicio y devuelve la respuesta con su codigo HTTP. No tiene reglas
 * de negocio: solo traduce entre HTTP y el servicio.
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.ClienteRequestDTO;
import pe.edu.upeu.megafarma.dto.ClienteResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.service.ClienteService;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/clientes")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "4. Clientes", description = "CRUD de clientes")
public class ClienteController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.clienteService = clienteService;
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Buscar clientes por nombre o documento")
    public ResponseEntity<PaginaResponseDTO<ClienteResponseDTO>> listar(
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(clienteService.listar(q, pagina, tamanio));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un cliente por id")
    public ResponseEntity<ClienteResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscar(id));
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping
    @Operation(summary = "Registrar un cliente")
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    // @PutMapping: atiende peticiones HTTP PUT (actualizar todo el recurso)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un cliente")
    public ResponseEntity<ClienteResponseDTO> actualizar(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long id,
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    // @DeleteMapping: atiende peticiones HTTP DELETE (eliminar o desactivar)
    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un cliente (borrado lógico)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
        // responde HTTP 204 No Content: salio bien y no hay cuerpo
        return ResponseEntity.noContent().build();
    }
}
