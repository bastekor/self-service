package mx.bastekor.selfservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Self Service Spring Boot application.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
		title = "Self Service API",
		version = "v1",
		description = "API para operaciones basicas de archivos y directorios"
))
public class SelfServiceApplication {

	/**
	 * Bootstraps the Spring application.
	 *
	 * @param args runtime arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SelfServiceApplication.class, args);
	}

}
