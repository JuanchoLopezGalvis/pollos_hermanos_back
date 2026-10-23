package co.edu.unbosque.pollos_hermanos_back.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import co.edu.unbosque.pollos_hermanos_back.exception.CorreoException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Envía correos en HTML construidos con plantillas Thymeleaf de
 * {@code src/main/resources/templates/correo}.
 * <p>
 * Usos previstos: confirmación de pedido, venta y pago; aviso de cartera
 * vencida; recuperación de contraseña; alerta de mortalidad mayor al 5%.
 */
@Service
public class CorreoService {

	/** Plantilla general con título y mensaje. */
	public static final String PLANTILLA_BASE = "correo/base";

	/** Cliente de correo de Spring. */
	private final JavaMailSender mailSender;

	/** Motor de plantillas de Thymeleaf. */
	private final ITemplateEngine motorPlantillas;

	/** Dirección del remitente. */
	private final String remitente;

	/**
	 * Crea el servicio.
	 *
	 * @param mailSender      cliente de correo
	 * @param motorPlantillas motor de plantillas
	 * @param remitente       dirección del remitente ({@code MAIL_USERNAME})
	 */
	public CorreoService(JavaMailSender mailSender, ITemplateEngine motorPlantillas,
			@Value("${spring.mail.username:}") String remitente) {
		this.mailSender = mailSender;
		this.motorPlantillas = motorPlantillas;
		this.remitente = remitente;
	}

	/**
	 * Envía un correo con la plantilla general.
	 *
	 * @param destinatario correo del destinatario
	 * @param asunto       asunto del correo
	 * @param titulo       título que se muestra en el cuerpo
	 * @param mensaje      texto principal del correo
	 * @throws CorreoException si no se pudo enviar
	 */
	public void enviarMensaje(String destinatario, String asunto, String titulo, String mensaje) {
		enviarPlantilla(destinatario, asunto, PLANTILLA_BASE, Map.of("titulo", titulo, "mensaje", mensaje));
	}

	/**
	 * Envía un correo con una plantilla Thymeleaf.
	 *
	 * @param destinatario correo del destinatario
	 * @param asunto       asunto del correo
	 * @param plantilla    ruta de la plantilla dentro de {@code templates}, sin la extensión
	 * @param variables    variables que usa la plantilla
	 * @throws CorreoException si no se pudo construir o enviar el correo
	 */
	public void enviarPlantilla(String destinatario, String asunto, String plantilla, Map<String, Object> variables) {
		Context contexto = new Context();
		contexto.setVariables(variables);
		String html = motorPlantillas.process(plantilla, contexto);
		try {
			MimeMessage correo = mailSender.createMimeMessage();
			MimeMessageHelper ayudante = new MimeMessageHelper(correo, false, StandardCharsets.UTF_8.name());
			if (!remitente.isBlank()) {
				ayudante.setFrom(remitente);
			}
			ayudante.setTo(destinatario);
			ayudante.setSubject(asunto);
			ayudante.setText(html, true);
			mailSender.send(correo);
		} catch (MessagingException | MailException e) {
			throw new CorreoException("No fue posible enviar el correo a " + destinatario + ".", e);
		}
	}
}
