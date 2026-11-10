package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.CategoriaRequestDTO;
import pe.edu.upeu.megafarma.dto.CategoriaResponseDTO;
import pe.edu.upeu.megafarma.entity.Categoria;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.CategoriaMapper;
import pe.edu.upeu.megafarma.repository.CategoriaRepository;
import pe.edu.upeu.megafarma.repository.ProductoRepository;
import pe.edu.upeu.megafarma.service.CategoriaService;

import java.util.List;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class CategoriaServiceImpl implements CategoriaService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
                                ProductoRepository productoRepository) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar() {
        // stream: recorre la coleccion aplicando map/filter y junta el resultado
        return categoriaRepository.findAll(Sort.by("nombre")).stream()
                .map(CategoriaMapper::toResponse)
                .toList();
    }

    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscar(Long id) {
        return CategoriaMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public CategoriaResponseDTO crear(CategoriaRequestDTO request) {

        String nombre = request.getNombre().trim();
        if (categoriaRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException("Ya existe una categoría con el nombre '" + nombre + "'");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());

        return CategoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponseDTO actualizar(Long id, CategoriaRequestDTO request) {

        Categoria categoria = obtener(id);
        String nombre = request.getNombre().trim();

        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException(
                            "Ya existe una categoría con el nombre '" + nombre + "'");
                });

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());

        return CategoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    /* Solo se elimina una categoria que no tiene productos. */
    @Override
    @Transactional
    public void eliminar(Long id) {

        Categoria categoria = obtener(id);

        if (productoRepository.existsByCategoriaId(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar la categoría '" + categoria.getNombre()
                            + "' porque tiene productos asociados");
        }

        categoriaRepository.delete(categoria);
    }

    private Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría con id " + id));
    }
}
