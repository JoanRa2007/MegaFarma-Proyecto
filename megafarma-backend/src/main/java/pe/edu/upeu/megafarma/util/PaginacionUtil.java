package pe.edu.upeu.megafarma.util;

/*
 * PARA ESTUDIAR · UTILIDADES (util)
 * Funciones de apoyo reutilizadas por varios servicios.
 */

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginacionUtil {

    // constante de la clase: un solo valor compartido que no cambia
    private static final int TAMANIO_MAXIMO = 100;

    private PaginacionUtil() {
    }

    /*
     * Arma el Pageable validando lo que llega del cliente: pagina nunca
     * negativa, tamanio entre 1 y 100 y solo campos de orden permitidos
     * (asi no se puede ordenar por una columna arbitraria).
     */
    public static Pageable crear(int pagina, int tamanio, String ordenarPor, String direccion,
                                 Set<String> camposPermitidos, String campoPorDefecto) {

        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);

        String campo = (ordenarPor != null && camposPermitidos.contains(ordenarPor))
                ? ordenarPor
                : campoPorDefecto;

        Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(paginaSegura, tamanioSeguro, Sort.by(sentido, campo));
    }

    /* Convierte el texto de busqueda en un patron LIKE en minusculas. */
    public static String patron(String texto) {
        if (texto == null || texto.isBlank()) {
            return "%";
        }
        String limpio = texto.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }
}
