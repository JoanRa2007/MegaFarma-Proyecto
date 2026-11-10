package pe.edu.upeu.megafarma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication: clase principal: activa la autoconfiguracion y el escaneo de componentes
@SpringBootApplication
public class MegafarmaBackendApplication {

	// punto de entrada: arranca Spring Boot y el servidor web embebido (Tomcat)
	public static void main(String[] args) {
		// crea el contexto de Spring, conecta la base, ejecuta Flyway y abre el puerto 8080
		SpringApplication.run(MegafarmaBackendApplication.class, args);
	}

}
