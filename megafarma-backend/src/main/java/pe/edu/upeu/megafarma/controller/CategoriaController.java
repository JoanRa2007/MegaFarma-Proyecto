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
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.CategoriaRequestDTO;
import pe.edu.upeu.megafarma.dto.CategoriaResponseDTO;
import pe.edu.upeu.megafarma.service.CategoriaService;

import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/categorias")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "2. Categorías", description = "CRUD de categorías de productos")
public class CategoriaController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.categoriaService = categoriaService;
    }

    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Listar categorías")
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(categoriaService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar una categoría por id")
    public ResponseEntity<CategoriaResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscar(id));
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping
    @Operation(summary = "Crear una categoría")
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CategoriaRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    // @PutMapping: atiende peticiones HTTP PUT (actualizar todo el recurso)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una categoría")
    public ResponseEntity<CategoriaResponseDTO> actualizar(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long id,
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    // @DeleteMapping: atiende peticiones HTTP DELETE (eliminar o desactivar)
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una categoría sin productos")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        // responde HTTP 204 No Content: salio bien y no hay cuerpo
        return ResponseEntity.noContent().build();
    }
}
