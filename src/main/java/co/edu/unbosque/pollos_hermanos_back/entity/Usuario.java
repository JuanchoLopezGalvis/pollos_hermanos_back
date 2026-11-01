package co.edu.unbosque.pollos_hermanos_back.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Usuario concreto del sistema, equivalente al {@code User} de Django que
 * hereda de {@code AbstractUser}.
 * <p>
 * Hereda todo de {@link UsuarioBase}. Si más adelante el usuario necesita datos
 * propios (por ejemplo, el vínculo con su registro de empleado), se agregan
 * aquí sin tocar la clase base.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends UsuarioBase {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/** Constructor vacío requerido por JPA y ModelMapper. */
	public Usuario() {
	}
}
