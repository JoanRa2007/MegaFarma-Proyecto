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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.dto.ProductoRequestDTO;
import pe.edu.upeu.megafarma.dto.ProductoResponseDTO;
import pe.edu.upeu.megafarma.service.ProductoService;

import java.time.LocalDateTime;
import java.util.List;

// @RestController: controlador REST: lo que devuelven sus metodos se convierte a JSON
@RestController
// @RequestMapping: ruta base comun a todos los metodos de esta clase
@RequestMapping("/api/v1/productos")
// @Tag: nombre del grupo con que aparece este controlador en Swagger
@Tag(name = "3. Productos", description = "CRUD y búsqueda de productos y medicamentos")
@org.springframework.validation.annotation.Validated
public class ProductoController {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ProductoService productoService;
    private final pe.edu.upeu.megafarma.service.AuditoriaService auditoria;

    public ProductoController(ProductoService productoService,
            pe.edu.upeu.megafarma.service.AuditoriaService auditoria) {
        this.auditoria = auditoria;
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.productoService = productoService;
    }

    /*
     * Listado paginado con busqueda.
     * Campos de ordenamiento permitidos: id, codigo, nombre, precioVenta, actualizadoEn.
     */
    // @GetMapping: atiende peticiones HTTP GET (leer)
    @GetMapping
    // @Operation: titulo y descripcion de la operacion en Swagger
    @Operation(summary = "Buscar productos por código, nombre o principio activo (HU-03)",
            description = "Paginado. Sin 'q' devuelve todos los productos activos.")
    public ResponseEntity<PaginaResponseDTO<ProductoResponseDTO>> listar(
            // @RequestParam: toma el valor de la query string: ?nombre=valor
            @RequestParam(required = false) @jakarta.validation.constraints.Pattern(regexp = "^(?!.*--)[\\p{L}\\p{N}\\s.,'()/%+-]{0,100}$", message = "La búsqueda contiene caracteres no permitidos (máximo 100 letras, números y . , ' ( ) / % + -, sin --)") String q,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @RequestParam(defaultValue = "false") boolean soloInactivos,
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int pagina,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanio,
            @RequestParam(defaultValue = "nombre") @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z]{1,20}$", message = "Campo de orden inválido") String ordenarPor,
            @RequestParam(defaultValue = "asc") @jakarta.validation.constraints.Pattern(regexp = "^(?i)(asc|desc)$", message = "La dirección debe ser asc o desc") String direccion) {

        // responde HTTP 200 OK; el objeto se convierte a JSON
        return ResponseEntity.ok(productoService.listar(
                q, incluirInactivos, soloInactivos, pagina, tamanio, ordenarPor, direccion));
    }

    @GetMapping("/cambios")
    @Operation(summary = "Productos modificados desde una fecha (sincronización offline-first)",
            description = "Ejemplo: desde=2026-11-01T00:00:00")
    public ResponseEntity<List<ProductoResponseDTO>> cambios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde) {
        return ResponseEntity.ok(productoService.cambiosDesde(desde));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un producto por id")
    public ResponseEntity<ProductoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscar(id));
    }

    // @PostMapping: atiende peticiones HTTP POST (crear o ejecutar una accion)
    @PostMapping
    @Operation(summary = "Registrar un producto (HU-01)")
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO request) {
        // responde HTTP 201 Created con el recurso recien creado
        ProductoResponseDTO creado = productoService.crear(request);
        auditoria.registrar("PRODUCTO_CREADO", creado.getCodigo() + " " + creado.getNombre(), true);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // @PutMapping: atiende peticiones HTTP PUT (actualizar todo el recurso)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar los datos de un producto (HU-02)")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            // @PathVariable: toma el valor de la ruta, por ejemplo el {id}
            @PathVariable Long id,
            // @Valid: ejecuta las validaciones del DTO; si alguna falla responde 400 sin entrar al metodo
            @Valid @RequestBody ProductoRequestDTO request) {
        ProductoResponseDTO actualizado = productoService.actualizar(id, request);
        auditoria.registrar("PRODUCTO_ACTUALIZADO", actualizado.getCodigo() + " precio " + actualizado.getPrecioVenta(), true);
        return ResponseEntity.ok(actualizado);
    }

    // @DeleteMapping: atiende peticiones HTTP DELETE (eliminar o desactivar)
    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un producto (HU-04)",
            description = "Borrado lógico: conserva lotes, ventas y kardex.")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        auditoria.registrar("PRODUCTO_DESACTIVADO", "Producto id " + id, true);
        // responde HTTP 204 No Content: salio bien y no hay cuerpo
        return ResponseEntity.noContent().build();
    }

    // @PatchMapping: atiende peticiones HTTP PATCH (cambiar una parte del recurso)
    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar un producto desactivado")
    public ResponseEntity<ProductoResponseDTO> activar(@PathVariable Long id) {
        ProductoResponseDTO activado = productoService.activar(id);
        auditoria.registrar("PRODUCTO_ACTIVADO", activado.getCodigo(), true);
        return ResponseEntity.ok(activado);
    }
}
