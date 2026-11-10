package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Aqui viven las reglas de negocio. El controlador llama al servicio, el servicio
 * valida las reglas, usa los repositorios y devuelve un DTO de respuesta.
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.megafarma.dto.ClienteRequestDTO;
import pe.edu.upeu.megafarma.dto.ClienteResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.entity.Cliente;
import pe.edu.upeu.megafarma.exception.RecursoNoEncontradoException;
import pe.edu.upeu.megafarma.exception.ReglaNegocioException;
import pe.edu.upeu.megafarma.mapper.ClienteMapper;
import pe.edu.upeu.megafarma.repository.ClienteRepository;
import pe.edu.upeu.megafarma.service.ClienteService;
import pe.edu.upeu.megafarma.util.PaginacionUtil;

import java.util.Set;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
// implements: esta clase cumple el contrato (los metodos) de esa interfaz
public class ClienteServiceImpl implements ClienteService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.clienteRepository = clienteRepository;
    }

    // @Override: este metodo implementa o redefine uno declarado en la interfaz o clase padre
    @Override
    // @Transactional: todo el metodo es una transaccion: si lanza excepcion se deshacen los cambios (rollback)
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ClienteResponseDTO> listar(String q, int pagina, int tamanio) {

        Pageable pageable = PaginacionUtil.crear(
                pagina, tamanio, "nombre", "asc", Set.of("nombre"), "nombre");

        Page<Cliente> resultado = clienteRepository.buscar(PaginacionUtil.patron(q), pageable);

        return PaginaResponseDTO.de(resultado,
                // stream: recorre la coleccion aplicando map/filter y junta el resultado
                resultado.getContent().stream().map(ClienteMapper::toResponse).toList());
    }

    @Override
    // readOnly: transaccion solo de lectura (mas liviana, no guarda cambios)
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscar(Long id) {
        return ClienteMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public ClienteResponseDTO crear(ClienteRequestDTO request) {

        validarDocumento(request);

        String documento = request.getNumeroDocumento().trim().toUpperCase();
        if (clienteRepository.findByNumeroDocumento(documento).isPresent()) {
            // regla de negocio incumplida -> el cliente recibe HTTP 409 con este mensaje
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el documento " + documento);
        }

        Cliente cliente = new Cliente();
        copiar(request, cliente);

        return ClienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request) {

        validarDocumento(request);

        Cliente cliente = obtener(id);
        String documento = request.getNumeroDocumento().trim().toUpperCase();

        clienteRepository.findByNumeroDocumento(documento)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException(
                            "Ya existe un cliente con el documento " + documento);
                });

        copiar(request, cliente);

        return ClienteMapper.toResponse(clienteRepository.save(cliente));
    }

    /* No se borra: puede tener ventas. Deja de aparecer en las busquedas. */
    @Override
    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = obtener(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    /* DNI = 8 digitos, RUC = 11 digitos. */
    private void validarDocumento(ClienteRequestDTO request) {
        String numero = request.getNumeroDocumento().trim();
        if ("DNI".equals(request.getTipoDocumento()) && !numero.matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI debe tener exactamente 8 dígitos");
        }
        if ("RUC".equals(request.getTipoDocumento()) && !numero.matches("\\d{11}")) {
            throw new ReglaNegocioException("El RUC debe tener exactamente 11 dígitos");
        }
    }

    private void copiar(ClienteRequestDTO request, Cliente cliente) {
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNumeroDocumento(request.getNumeroDocumento().trim().toUpperCase());
        cliente.setNombre(request.getNombre().trim());
        cliente.setTelefono(request.getTelefono());
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                // findBy... devuelve un Optional: si viene vacio se lanza la excepcion indicada
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el cliente con id " + id));
    }
}
