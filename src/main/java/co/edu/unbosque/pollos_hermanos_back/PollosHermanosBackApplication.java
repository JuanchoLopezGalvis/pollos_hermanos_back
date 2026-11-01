package co.edu.unbosque.pollos_hermanos_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Clase principal del backend de Los Pollos Hermanos: sistema de gestión de
 * producción y venta de pollos de engorde.
 * <p>
 * Se excluye el usuario en memoria que Spring Security crea por defecto, porque
 * la autenticación se hace con JWT y los usuarios vivirán en PostgreSQL.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PollosHermanosBackApplication {

	/**
	 * Punto de entrada de la aplicación.
	 *
	 * @param args argumentos de la línea de comandos
	 */
	public static void main(String[] args) {
		SpringApplication.run(PollosHermanosBackApplication.class, args);
	}

}
