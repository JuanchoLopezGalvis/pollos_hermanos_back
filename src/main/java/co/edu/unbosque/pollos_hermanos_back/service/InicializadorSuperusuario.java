package co.edu.unbosque.pollos_hermanos_back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;
import co.edu.unbosque.pollos_hermanos_back.repository.UsuarioRepository;

/**
 * Crea el primer superusuario al arrancar, como {@code createsuperuser} de
 * Django.
 * <p>
 * Solo actúa si están definidas las variables de entorno {@code ADMIN_USUARIO},
 * {@code ADMIN_CORREO} y {@code ADMIN_CONTRASENA} y todavía no existe un usuario
 * con ese nombre. Así nunca hay credenciales escritas en el código.
 */
@Service
public class InicializadorSuperusuario implements ApplicationRunner {

	/** Registro de eventos. */
	private static final Logger log = LoggerFactory.getLogger(InicializadorSuperusuario.class);

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarioRepository;

	/** Codificador de contraseñas. */
	private final PasswordEncoder passwordEncoder;

	/** Nombre de usuario del superusuario. */
	private final String nombreUsuario;

	/** Correo del superusuario. */
	private final String correo;

	/** Contraseña inicial del superusuario. */
	private final String contrasena;

	/**
	 * Crea el inicializador.
	 *
	 * @param usuarioRepository repositorio de usuarios
	 * @param passwordEncoder   codificador de contraseñas
	 * @param nombreUsuario     valor de {@code ADMIN_USUARIO}
	 * @param correo            valor de {@code ADMIN_CORREO}
	 * @param contrasena        valor de {@code ADMIN_CONTRASENA}
	 */
	public InicializadorSuperusuario(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			@Value("${pollos.admin.usuario:}") String nombreUsuario, @Value("${pollos.admin.correo:}") String correo,
			@Value("${pollos.admin.contrasena:}") String contrasena) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.nombreUsuario = nombreUsuario;
		this.correo = correo;
		this.contrasena = contrasena;
	}

	/**
	 * Crea el superusuario si está configurado y no existe.
	 *
	 * @param args argumentos de la aplicación
	 */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (nombreUsuario.isBlank() || correo.isBlank() || contrasena.isBlank()) {
			return;
		}
		if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
			return;
		}
		Usuario superusuario = new Usuario();
		superusuario.setNombreUsuario(nombreUsuario);
		superusuario.setCorreo(correo);
		superusuario.setContrasena(passwordEncoder.encode(contrasena));
		superusuario.setSuperusuario(true);
		superusuario.setActivo(true);
		usuarioRepository.save(superusuario);
		log.info("Se creó el superusuario '{}'.", nombreUsuario);
	}
}
