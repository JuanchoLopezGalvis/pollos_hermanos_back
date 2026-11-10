package co.edu.unbosque.pollos_hermanos_back.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import co.edu.unbosque.pollos_hermanos_back.exception.CorreoException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Pruebas de {@link CorreoService} con la plantilla real.
 */
class CorreoServiceTest {

	/** Cliente de correo simulado. */
	private final JavaMailSender mailSender = mock(JavaMailSender.class);

	/** Servicio bajo prueba. */
	private final CorreoService correoService = new CorreoService(mailSender, motorPlantillas(), "finca@pollos.co",
			"Los Pollos Hermanos");

	/**
	 * Crea el motor de Thymeleaf con SpEL, igual al de la aplicación, que lee las plantillas de {@code templates}.
	 *
	 * @return motor de plantillas
	 */
	private static SpringTemplateEngine motorPlantillas() {
		ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
		resolvedor.setPrefix("templates/");
		resolvedor.setSuffix(".html");
		resolvedor.setTemplateMode(TemplateMode.HTML);
		resolvedor.setCharacterEncoding("UTF-8");
		SpringTemplateEngine motor = new SpringTemplateEngine();
		motor.setTemplateResolver(resolvedor);
		return motor;
	}

	/** Envía el correo con el asunto y el contenido de la plantilla. */
	@Test
	void enviaCorreoConLaPlantillaBase() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

		correoService.enviarMensaje("cliente@correo.co", "Pedido confirmado", "Su pedido fue confirmado",
				"Pedido 15: 200 aves.");

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());
		MimeMessage enviado = captor.getValue();
		enviado.saveChanges();
		assertEquals("Pedido confirmado", enviado.getSubject());
		assertEquals("Los Pollos Hermanos <finca@pollos.co>", enviado.getFrom()[0].toString());
		assertEquals("cliente@correo.co", enviado.getAllRecipients()[0].toString());
		assertTrue(enviado.getContent().toString().contains("Pedido 15: 200 aves."));
	}

	/** Si el servidor de correo falla, se lanza CorreoException. */
	@Test
	void fallaDeEnvioLanzaCorreoException() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
		doThrow(new MailSendException("sin conexión")).when(mailSender).send(any(MimeMessage.class));

		assertThrows(CorreoException.class,
				() -> correoService.enviarMensaje("cliente@correo.co", "Asunto", "Título", "Mensaje"));
	}
}
