package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Limita los inicios de sesion fallidos por IP (proteccion contra fuerza bruta).
 * Ventana deslizante de 60 segundos: si una IP acumula demasiados fallos, se
 * rechazan sus intentos con HTTP 429 hasta que salga de la ventana.
 * Complementa al bloqueo de la cuenta tras 5 fallos (HU-46): este frena a quien
 * prueba muchas cuentas distintas desde el mismo equipo.
 * Se guarda en memoria; con varios servidores se usaria Redis.
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorIntentosLogin {

    private static final long VENTANA_MS = 60_000;

    private final int maxFallos;
    private final Map<String, Deque<Long>> fallosPorIp = new ConcurrentHashMap<>();

    public LimitadorIntentosLogin(@Value("${app.seguridad.max-fallos-por-ip:10}") int maxFallos) {
        this.maxFallos = maxFallos;
    }

    public boolean estaBloqueada(String ip) {
        Deque<Long> fallos = fallosPorIp.get(clave(ip));
        if (fallos == null) {
            return false;
        }
        synchronized (fallos) {
            depurar(fallos);
            return fallos.size() >= maxFallos;
        }
    }

    public void registrarFallo(String ip) {
        Deque<Long> fallos = fallosPorIp.computeIfAbsent(clave(ip), k -> new ArrayDeque<>());
        synchronized (fallos) {
            depurar(fallos);
            fallos.addLast(System.currentTimeMillis());
        }
    }

    /* Segundos hasta que el fallo mas antiguo salga de la ventana. */
    public long segundosParaReintentar(String ip) {
        Deque<Long> fallos = fallosPorIp.get(clave(ip));
        if (fallos == null) {
            return 0;
        }
        synchronized (fallos) {
            Long primero = fallos.peekFirst();
            return primero == null ? 0 : Math.max(1, (primero + VENTANA_MS - System.currentTimeMillis()) / 1000);
        }
    }

    private static void depurar(Deque<Long> fallos) {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!fallos.isEmpty() && fallos.peekFirst() < limite) {
            fallos.pollFirst();
        }
    }

    private static String clave(String ip) {
        return ip == null ? "desconocida" : ip;
    }
}
