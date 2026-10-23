package co.edu.unbosque.pollos_hermanos_back.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuración de OpenAPI (Swagger) para documentar la API REST, disponible en
 * {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

	/** Nombre del esquema de seguridad JWT en la documentación. */
	private static final String ESQUEMA_JWT = "bearerAuth";

	/**
	 * Define la información general de la API y el esquema de autenticación JWT.
	 *
	 * @return configuración de OpenAPI
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		String descripcion = """
				<p>API del sistema de gestión de producción y venta de pollos de engorde de
				<strong>Los Pollos Hermanos</strong>: galpones y lotes, alimentación, sanidad,
				gastos, personal, comercial, cartera y entregas, y seguimiento diario del
				crecimiento (MongoDB).</p>
				<h3>Autenticación</h3>
				<ol>
				  <li>Obtén un token JWT.</li>
				  <li>Haz clic en <strong>Authorize</strong> y pega el token.</li>
				  <li>Desde ese momento puedes usar los endpoints protegidos.</li>
				</ol>
				<h3>Códigos de estado</h3>
				<ul>
				  <li><strong>200/201</strong>: operación exitosa.</li>
				  <li><strong>400</strong>: datos con formato inválido o rechazados por una restricción.</li>
				  <li><strong>401</strong>: token ausente, inválido o expirado.</li>
				  <li><strong>403</strong>: el usuario no tiene permisos para la operación.</li>
				  <li><strong>404</strong>: recurso no encontrado.</li>
				  <li><strong>409</strong>: la operación incumple una regla del negocio.</li>
				</ul>
				""";

		Info info = new Info().title("API Los Pollos Hermanos").version("1.0").description(descripcion)
				.contact(new Contact().name("Equipo Los Pollos Hermanos"));

		SecurityScheme esquemaJwt = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
				.bearerFormat("JWT").description("Token JWT, sin el prefijo 'Bearer '.");

		return new OpenAPI().info(info).addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
				.components(new Components().addSecuritySchemes(ESQUEMA_JWT, esquemaJwt));
	}
}
