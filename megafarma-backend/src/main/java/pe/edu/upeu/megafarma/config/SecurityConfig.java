package pe.edu.upeu.megafarma.config;

/*
 * PARA ESTUDIAR · CONFIGURACION (config)
 * Clases que Spring lee al arrancar para armar la aplicacion: seguridad, CORS,
 * Swagger y datos de demostracion.
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.web.cors.CorsConfigurationSource;
import pe.edu.upeu.megafarma.security.JwtAuthenticationFilter;
import pe.edu.upeu.megafarma.security.JwtService;
import pe.edu.upeu.megafarma.security.RestAccessDeniedHandler;
import pe.edu.upeu.megafarma.security.RestAuthenticationEntryPoint;

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

// @Configuration: clase de configuracion: sus metodos @Bean crean objetos que Spring administra
@Configuration
// @EnableWebSecurity: activa Spring Security con la configuracion de esta clase
@EnableWebSecurity
public class SecurityConfig {

    // constante de la clase: un solo valor compartido que no cambia
    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/api/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    // @Bean: el objeto que devuelve este metodo queda disponible para inyectarse en otras clases
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                // API sin estado con token en cabecera: no aplica CSRF ni sesion.
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Catalogo: todos leen; ADMIN y ALMACENERO escriben
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAuthority(CATALOGO_LEER.name())
                        .requestMatchers(
                                "/api/v1/categorias/**", "/api/v1/productos/**")
                        .hasAuthority(CATALOGO_ESCRIBIR.name())

                        // Inventario y alertas: todos consultan; ADMIN y ALMACENERO mueven stock
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/inventario/**", "/api/v1/alertas/**")
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

                        // Reportes: solo ADMIN
                        .requestMatchers("/api/v1/reportes/**")
                        .hasAuthority(REPORTES_VER.name())

                        // Administracion de usuarios y roles: solo ADMIN
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/roles/**")
                        .hasAuthority(USUARIOS_ADMINISTRAR.name())

                        // Todo lo demas (auth/me...) exige un usuario autenticado
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * BCrypt: hash lento y con "sal" aleatoria. La misma contrasena
     * produce un hash distinto cada vez y no se puede revertir.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * Spring arma el AuthenticationManager con el UserDetailsService
     * (UsuarioDetailsService) y el PasswordEncoder declarados.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
