package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Lista de tokens anulados al cerrar sesion. Cada token JWT lleva un
 * identificador unico (jti); al hacer logout ese jti se guarda aqui hasta
 * que el token expire. El filtro rechaza cualquier token de esta lista, asi
 * un token robado deja de servir en cuanto su dueño cierra sesion.
 * Se guarda en memoria; con varios servidores se usaria Redis o la base de datos.
 */

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokensRevocados {

    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();

    public void revocar(String jti, Instant expira) {
        if (jti != null) {
            revocados.put(jti, expira == null ? Instant.now().plusSeconds(86_400) : expira);
        }
    }

    public boolean estaRevocado(String jti) {
        if (jti == null) {
            return false;
        }
        Instant ahora = Instant.now();
        revocados.entrySet().removeIf(e -> e.getValue().isBefore(ahora));
        return revocados.containsKey(jti);
    }
}
