package co.edu.unbosque.pollos_hermanos_back;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto completo arranque. Necesita PostgreSQL, MongoDB y las
 * variables de entorno configuradas, así que solo corre cuando existe
 * {@code DB_PASSWORD}.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class PollosHermanosBackApplicationTests {

	/** El contexto de Spring arranca sin errores. */
	@Test
	void contextLoads() {
	}

}
