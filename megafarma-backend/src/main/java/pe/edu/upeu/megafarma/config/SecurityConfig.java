package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Configuracion de Spring Security:
 *  - API sin estado con JWT (sin sesiones ni cookies, por eso no aplica CSRF).
 *  - Cada ruta exige un permiso (control de acceso en el servidor, no solo en la app).
 *  - Cabeceras de seguridad en todas las respuestas (nosniff, anti-clickjacking,
 *    CSP, HSTS, sin cache de datos sensibles, Referrer-Policy y Permissions-Policy).
 *  - Swagger solo es publico si app.swagger.habilitado=true (perfiles dev y local).
 *    En produccion la documentacion de la API no se expone.
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfigurationSource;
import pe.edu.upeu.megafarma.security.JwtAuthenticationFilter;
import pe.edu.upeu.megafarma.security.JwtService;
import pe.edu.upeu.megafarma.security.RestAccessDeniedHandler;
import pe.edu.upeu.megafarma.security.RestAuthenticationEntryPoint;
import pe.edu.upeu.megafarma.security.TokensRevocados;

import static pe.edu.upeu.megafarma.enums.Permiso.CATALOGO_ESCRIBIR;
import static pe.edu.upeu.megafarma.enums.Permiso.CATALOGO_LEER;
import static pe.edu.upeu.megafarma.enums.Permiso.CLIENTES_ESCRIBIR;
import static pe.edu.upeu.megafarma.enums.Permiso.CLIENTES_LEER;
import static pe.edu.upeu.megafarma.enums.Permiso.INVENTARIO_ESCRIBIR;
import static pe.edu.upeu.megafarma.enums.Permiso.INVENTARIO_LEER;
import static pe.edu.upeu.megafarma.enums.Permiso.REPORTES_VER;
import static pe.edu.upeu.megafarma.enums.Permiso.USUARIOS_ADMINISTRAR;
import static pe.edu.upeu.megafarma.enums.Permiso.VENTAS_ANULAR;
import static pe.edu.upeu.megafarma.enums.Permiso.VENTAS_LEER;
import static pe.edu.upeu.megafarma.enums.Permiso.VENTAS_REGISTRAR;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/api/health",
            "/error"
    };

    private static final String[] RUTAS_SWAGGER = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            TokensRevocados tokensRevocados,
            CorsConfigurationSource corsConfigurationSource,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            @Value("${app.swagger.habilitado:false}") boolean swaggerHabilitado) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService, tokensRevocados);

        http
                // API sin estado con token en cabecera: no hay cookies que robar, CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ---------- Cabeceras de seguridad ----------
                .headers(h -> h
                        .contentTypeOptions(Customizer.withDefaults())                 // X-Content-Type-Options: nosniff
                        .frameOptions(f -> f.deny())                                   // X-Frame-Options: DENY (clickjacking)
                        .cacheControl(Customizer.withDefaults())                       // no-store: no cachear datos sensibles
                        .httpStrictTransportSecurity(hsts -> hsts                       // HSTS: solo HTTPS (se aplica sobre https)
                                .includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                        // CSP estricta para la API: sus respuestas son JSON, nunca deben ejecutar scripts.
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                request -> request.getRequestURI().startsWith("/api/"),
                                new StaticHeadersWriter("Content-Security-Policy",
                                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))))

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(RUTAS_PUBLICAS).permitAll();
                    if (swaggerHabilitado) {
                        auth.requestMatchers(RUTAS_SWAGGER).permitAll();
                    }
                    auth
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                            // Catalogo: todos leen; ADMIN y ALMACENERO escriben
                            .requestMatchers(HttpMethod.GET, "/api/v1/categorias/**", "/api/v1/productos/**")
                            .hasAuthority(CATALOGO_LEER.name())
                            .requestMatchers("/api/v1/categorias/**", "/api/v1/productos/**")
                            .hasAuthority(CATALOGO_ESCRIBIR.name())

                            // Inventario y alertas: todos consultan; ADMIN y ALMACENERO mueven stock
                            .requestMatchers(HttpMethod.GET, "/api/v1/inventario/**", "/api/v1/alertas/**")
                            .hasAuthority(INVENTARIO_LEER.name())
                            .requestMatchers("/api/v1/inventario/**")
                            .hasAuthority(INVENTARIO_ESCRIBIR.name())

                            // Clientes
                            .requestMatchers(HttpMethod.GET, "/api/v1/clientes/**")
                            .hasAuthority(CLIENTES_LEER.name())
                            .requestMatchers("/api/v1/clientes/**")
                            .hasAuthority(CLIENTES_ESCRIBIR.name())

                            // Ventas: anular es solo de ADMIN
                            .requestMatchers(HttpMethod.POST, "/api/v1/ventas/*/anular")
                            .hasAuthority(VENTAS_ANULAR.name())
                            .requestMatchers(HttpMethod.GET, "/api/v1/ventas/**")
                            .hasAuthority(VENTAS_LEER.name())
                            .requestMatchers("/api/v1/ventas/**")
                            .hasAuthority(VENTAS_REGISTRAR.name())

                            // Dashboard y reportes: solo ADMIN
                            .requestMatchers("/api/v1/dashboard/**", "/api/v1/reportes/**")
                            .hasAuthority(REPORTES_VER.name())

                            // Usuarios, roles y bitacora: solo ADMIN
                            .requestMatchers("/api/v1/usuarios/**", "/api/v1/roles/**", "/api/v1/auditoria/**")
                            .hasAuthority(USUARIOS_ADMINISTRAR.name())

                            // Todo lo demas (auth/me, auth/logout...) exige un usuario autenticado.
                            // Cualquier ruta no declarada queda cerrada por defecto.
                            .anyRequest().authenticated();
                })
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * BCrypt (factor 12): hash lento y con "sal" aleatoria. La misma contrasena
     * produce un hash distinto cada vez y no se puede revertir.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
