package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.upeu.megafarma.enums.Permiso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Matriz rol -> permisos (HU-23). */
public final class PermisosPorRol {

    public static final String ADMIN = "ADMIN";
    public static final String VENDEDOR = "VENDEDOR";
    public static final String ALMACENERO = "ALMACENERO";

    // constante de la clase: un solo valor compartido que no cambia
    private static final Map<String, Set<Permiso>> PERMISOS = Map.of(
            ADMIN, Collections.unmodifiableSet(EnumSet.allOf(Permiso.class)),
            VENDEDOR, Collections.unmodifiableSet(EnumSet.of(
                    Permiso.CATALOGO_LEER,
                    Permiso.INVENTARIO_LEER,
                    Permiso.CLIENTES_LEER,
                    Permiso.CLIENTES_ESCRIBIR,
                    Permiso.VENTAS_LEER,
                    Permiso.VENTAS_REGISTRAR)),
            ALMACENERO, Collections.unmodifiableSet(EnumSet.of(
                    Permiso.CATALOGO_LEER,
                    Permiso.CATALOGO_ESCRIBIR,
                    Permiso.INVENTARIO_LEER,
                    Permiso.INVENTARIO_ESCRIBIR))
    );

    private PermisosPorRol() {
    }

    public static Set<Permiso> de(String nombreRol) {
        return PERMISOS.getOrDefault(nombreRol, Set.of());
    }

    /* Nombres de los permisos del rol, en el orden del enum. */
    public static List<String> permisos(String nombreRol) {
        // stream: recorre la coleccion aplicando map/filter y junta el resultado
        return de(nombreRol).stream().sorted().map(Permiso::name).toList();
    }

    /*
     * Authorities para Spring Security: "ROLE_<rol>" (para hasRole) mas un
     * authority por cada permiso (para hasAuthority).
     */
    public static List<GrantedAuthority> authorities(String nombreRol) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + nombreRol));
        permisos(nombreRol).forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }
}
