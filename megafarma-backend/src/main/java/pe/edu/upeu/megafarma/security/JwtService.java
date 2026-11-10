package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Clases de autenticacion (quien eres: token JWT) y autorizacion (que puedes
 * hacer: permisos del rol).
 */

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

// @Service: Spring crea un unico objeto de esta clase y lo inyecta donde se necesite
@Service
public class JwtService {

    // dependencia: 'final' obliga a recibirla en el constructor (inyeccion de dependencias)
    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(
            // @Value: inyecta un valor de application.yaml (lo que va tras ':' es el valor por defecto)
            @Value("${app.jwt.secret}") String secretoBase64,
            @Value("${app.jwt.expiracion-minutos:60}") long expiracionMinutos) {

        // La clave debe tener al menos 256 bits (32 bytes); si es mas
        // corta, Keys.hmacShaKeyFor lanza WeakKeyException al arrancar.
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        // el constructor guarda cada dependencia que Spring le entrega al crear el objeto
        this.expiracionMinutos = expiracionMinutos;
    }

    /*
     * Token con: sub = username, authorities = rol y permisos,
     * iat = emitido en, exp = expira en.
     */
    public String generarToken(UserDetails usuario) {

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);

        List<String> authorities = usuario.getAuthorities()
                // stream: recorre la coleccion aplicando map/filter y junta el resultado
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("authorities", authorities)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
    }

    /*
     * Valida firma y expiracion y devuelve el username (claim "sub").
     * Lanza ExpiredJwtException si expiro y JwtException si la firma
     * o el formato no son validos.
     */
    public String extraerUsername(String token) {
        return leerClaims(token).getSubject();
    }

    public Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionSegundos() {
        return expiracionMinutos * 60;
    }
}
