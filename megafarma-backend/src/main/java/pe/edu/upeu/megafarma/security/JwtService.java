package pe.edu.upeu.megafarma.security;

/*
 * PARA ESTUDIAR · SEGURIDAD (security)
 * Genera y valida los tokens JWT firmados con HMAC-SHA256.
 * El token lleva: sub (usuario), authorities (rol y permisos), iss (emisor),
 * jti (identificador unico, permite revocarlo al cerrar sesion), iat y exp.
 * Si alguien modifica el token, la firma deja de coincidir y se rechaza.
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
import java.util.UUID;

@Service
public class JwtService {

    /* Solo se aceptan tokens emitidos por esta API. */
    public static final String EMISOR = "megafarma-api";

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(
            @Value("${app.jwt.secret}") String secretoBase64,
            @Value("${app.jwt.expiracion-minutos:60}") long expiracionMinutos) {

        // La clave debe tener al menos 256 bits (32 bytes); si es mas
        // corta, Keys.hmacShaKeyFor lanza WeakKeyException al arrancar.
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(UserDetails usuario) {

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);

        List<String> authorities = usuario.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(EMISOR)
                .subject(usuario.getUsername())
                .claim("authorities", authorities)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
    }

    /*
     * Valida firma, emisor y expiracion y devuelve el usuario (claim "sub").
     * Lanza JwtException si el token no es valido.
     */
    public String extraerUsername(String token) {
        return leerClaims(token).getSubject();
    }

    public Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionSegundos() {
        return expiracionMinutos * 60;
    }
}
