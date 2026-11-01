package co.edu.unbosque.pollos_hermanos_back.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Genera y valida los tokens JWT firmados con HMAC-SHA256.
 * <p>
 * El token solo guarda el nombre de usuario. Los permisos se leen de la base de
 * datos en cada solicitud, así un cambio de rol o la desactivación de un usuario
 * aplican de inmediato.
 * <p>
 * El secreto se lee de la variable de entorno {@code JWT_SECRET} y debe tener al
 * menos 32 caracteres; nunca se escribe en un archivo versionado.
 */
@Component
public class JwtUtil {

	/** Llave de firma derivada del secreto. */
	private final SecretKey llave;

	/** Vigencia del token en milisegundos. */
	private final long expiracionMs;

	/**
	 * Crea la utilidad con el secreto y la vigencia configurados.
	 *
	 * @param secreto      secreto de firma, de al menos 32 caracteres
	 * @param expiracionMs vigencia del token en milisegundos
	 * @throws IllegalStateException si el secreto falta o es demasiado corto
	 */
	public JwtUtil(@Value("${jwt.secret}") String secreto, @Value("${jwt.expiration-ms}") long expiracionMs) {
		if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("La variable de entorno JWT_SECRET debe tener al menos 32 caracteres.");
		}
		this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
		this.expiracionMs = expiracionMs;
	}

	/**
	 * Genera un token para el usuario.
	 *
	 * @param nombreUsuario nombre de usuario
	 * @return token JWT firmado
	 */
	public String generarToken(String nombreUsuario) {
		Date ahora = new Date();
		return Jwts.builder().subject(nombreUsuario).issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + expiracionMs)).signWith(llave).compact();
	}

	/**
	 * Indica si el token tiene una firma válida y no ha expirado.
	 *
	 * @param token token JWT
	 * @return {@code true} si el token es válido
	 */
	public boolean esValido(String token) {
		try {
			leerClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Obtiene el usuario dueño del token.
	 *
	 * @param token token JWT válido
	 * @return identificador del usuario
	 */
	public String extraerUsuario(String token) {
		return leerClaims(token).getSubject();
	}

	/**
	 * Verifica la firma del token y devuelve su contenido.
	 *
	 * @param token token JWT
	 * @return claims del token
	 */
	private Claims leerClaims(String token) {
		return Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();
	}
}
