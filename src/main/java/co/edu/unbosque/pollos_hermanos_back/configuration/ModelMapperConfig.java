package co.edu.unbosque.pollos_hermanos_back.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.unbosque.pollos_hermanos_back.dto.RolDTO;
import co.edu.unbosque.pollos_hermanos_back.dto.UsuarioDTO;
import co.edu.unbosque.pollos_hermanos_back.entity.Rol;
import co.edu.unbosque.pollos_hermanos_back.entity.Usuario;

/**
 * Configuración de ModelMapper para convertir entre entidades, documentos y DTO.
 */
@Configuration
public class ModelMapperConfig {

	/**
	 * Crea el ModelMapper compartido por los servicios.
	 * <p>
	 * Usa coincidencia estricta y omite los valores nulos, de modo que al
	 * actualizar solo cambian los campos enviados. Las relaciones (roles y
	 * permisos), la contraseña y los campos que controla el sistema se omiten:
	 * los servicios los asignan a mano.
	 *
	 * @return instancia configurada de ModelMapper
	 */
	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT).setSkipNullEnabled(true);

		modelMapper.typeMap(Usuario.class, UsuarioDTO.class).addMappings(m -> {
			m.skip(UsuarioDTO::setContrasena);
			m.skip(UsuarioDTO::setRoles);
			m.skip(UsuarioDTO::setPermisos);
			m.skip(UsuarioDTO::setPermisosEfectivos);
		});
		modelMapper.typeMap(UsuarioDTO.class, Usuario.class).addMappings(m -> {
			m.skip(Usuario::setId);
			m.skip(Usuario::setContrasena);
			m.skip(Usuario::setRoles);
			m.skip(Usuario::setPermisos);
			m.skip(Usuario::setFechaRegistro);
			m.skip(Usuario::setUltimoIngreso);
		});

		modelMapper.typeMap(Rol.class, RolDTO.class).addMappings(m -> m.skip(RolDTO::setPermisos));
		modelMapper.typeMap(RolDTO.class, Rol.class).addMappings(m -> {
			m.skip(Rol::setId);
			m.skip(Rol::setPermisos);
		});
		return modelMapper;
	}
}
