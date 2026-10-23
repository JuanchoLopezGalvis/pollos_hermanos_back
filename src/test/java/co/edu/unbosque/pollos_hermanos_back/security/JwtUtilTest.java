package co.edu.unbosque.pollos_hermanos_back.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link JwtUtil}.
 */
class JwtUtilTest {

	/** Secreto de prueba de 32 caracteres. */
	private static final String SECRETO = "secreto-de-prueba-de-32-caracteres";

	/** Un token generado es válido y conserva el usuario. */
	@Test
	void generaTokenValidoConElUsuario() {
		JwtUtil jwtUtil = new JwtUtil(SECRETO, 60_000);
		String token = jwtUtil.generarToken("operario");

		assertTrue(jwtUtil.esValido(token));
		assertEquals("operario", jwtUtil.extraerUsuario(token));
	}

	/** Un token firmado con otro secreto no es válido. */
	@Test
	void rechazaTokenConOtraFirma() {
		String token = new JwtUtil("otro-secreto-distinto-de-32-caracteres", 60_000).generarToken("operario");

		assertFalse(new JwtUtil(SECRETO, 60_000).esValido(token));
	}

	/** Un token expirado no es válido. */
	@Test
	void rechazaTokenExpirado() {
		JwtUtil jwtUtil = new JwtUtil(SECRETO, -1_000);

		assertFalse(jwtUtil.esValido(jwtUtil.generarToken("operario")));
	}

	/** Un texto cualquiera no es un token válido. */
	@Test
	void rechazaTextoQueNoEsToken() {
		assertFalse(new JwtUtil(SECRETO, 60_000).esValido("no-es-un-token"));
	}

	/** Un secreto corto impide arrancar. */
	@Test
	void exigeSecretoDeAlMenos32Caracteres() {
		assertThrows(IllegalStateException.class, () -> new JwtUtil("corto", 60_000));
	}
}
