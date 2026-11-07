package co.edu.unbosque.pollos_hermanos_back.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas, como el marcado diario de la cartera
 * vencida.
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
