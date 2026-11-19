package pe.edu.upeu.megafarma.service.impl;

/*
 * PARA ESTUDIAR · CAPA SERVICIO - IMPLEMENTACION (service.impl)
 * Bitacora de auditoria (HU-45).
 *  - REQUIRES_NEW: el registro se guarda en su propia transaccion, asi queda
 *    aunque la operacion principal falle (por ejemplo, un login rechazado).
 *  - Nunca rompe el flujo principal: si falla al guardar, solo se registra en el log.
 *  - La IP es la de la conexion (getRemoteAddr). No se confia en X-Forwarded-For,
 *    porque cualquier cliente podria falsificarla.
 */

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.upeu.megafarma.dto.AuditoriaResponseDTO;
import pe.edu.upeu.megafarma.dto.PaginaResponseDTO;
import pe.edu.upeu.megafarma.entity.Auditoria;
import pe.edu.upeu.megafarma.repository.AuditoriaRepository;
import pe.edu.upeu.megafarma.service.AuditoriaService;

import java.time.LocalDateTime;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);

    private final AuditoriaRepository repositorio;

    public AuditoriaServiceImpl(AuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String detalle, boolean exito) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth == null || !auth.isAuthenticated() ? "anonimo" : auth.getName();
        registrarDe(usuario, accion, detalle, exito);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarDe(String usuario, String accion, String detalle, boolean exito) {
        try {
            Auditoria a = new Auditoria();
            a.setFecha(LocalDateTime.now());
            a.setUsuario(recortar(usuario == null || usuario.isBlank() ? "anonimo" : usuario, 50));
            a.setAccion(recortar(accion, 40));
            a.setDetalle(recortar(detalle, 250));
            a.setIp(ipActual());
            a.setExito(exito);
            repositorio.save(a);
            log.info("AUDITORIA | usuario={} accion={} exito={} ip={} detalle={}",
                    a.getUsuario(), a.getAccion(), exito, a.getIp(), a.getDetalle());
        } catch (Exception e) {
            log.error("No se pudo registrar la auditoria de {}", accion, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<AuditoriaResponseDTO> buscar(String usuario, String accion, int pagina, int tamanio) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), 100),
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        Page<Auditoria> resultado = repositorio.buscar(
                usuario == null ? "" : usuario.trim().toLowerCase(),
                accion == null ? "" : accion.trim().toUpperCase(),
                pageable);
        return PaginaResponseDTO.de(resultado, resultado.getContent().stream()
                .map(a -> new AuditoriaResponseDTO(a.getId(), a.getFecha(), a.getUsuario(), a.getAccion(),
                        a.getDetalle(), a.getIp(), a.isExito()))
                .toList());
    }

    private static String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest request = atributos.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        // Se quitan saltos de linea para que nadie pueda falsificar lineas en el log.
        String limpio = texto.replaceAll("[\\r\\n\\t]", " ");
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
