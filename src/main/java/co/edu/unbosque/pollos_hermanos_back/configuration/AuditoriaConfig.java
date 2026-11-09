package co.edu.unbosque.pollos_hermanos_back.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conecta la auditoría de PostgreSQL ({@code sql/modulo_10_auditoria.sql}) con
 * el usuario autenticado del backend.
 */
@Configuration
public class AuditoriaConfig {

	/**
	 * Envuelve el {@link DataSource} que crea Spring Boot en un
	 * {@link UsuarioAuditoriaDataSource}, para que JPA, {@code JdbcTemplate} y
	 * los procedimientos usen todos conexiones con el usuario ya fijado.
	 * Es {@code static} para que se registre antes de crear los demás beans.
	 *
	 * @return procesador que envuelve el DataSource
	 */
	@Bean
	public static BeanPostProcessor envolverDataSourceAuditoria() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof DataSource dataSource && !(bean instanceof UsuarioAuditoriaDataSource)) {
					return new UsuarioAuditoriaDataSource(dataSource);
				}
				return bean;
			}
		};
	}
}
