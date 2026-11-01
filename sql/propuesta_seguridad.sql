-- =====================================================================
-- PROPUESTA: tablas de usuarios, roles y permisos (estilo Django).
-- Estado: borrador para revisar e integrar al modelo relacional de la Fase 1.
--
-- Coincide con las entidades JPA Usuario/UsuarioBase, Rol y Permiso.
-- La aplicación arranca con spring.jpa.hibernate.ddl-auto=validate, así que
-- si cambian nombres o tipos aquí, hay que cambiarlos también en las entidades.
--
-- Equivalencias con Django:
--   usuario          -> auth_user (AbstractUser)
--   rol              -> auth_group
--   permiso          -> auth_permission (codigo = 'modulo.accion')
--   rol_permiso      -> auth_group_permissions
--   usuario_rol      -> auth_user_groups
--   usuario_permiso  -> auth_user_user_permissions
-- =====================================================================

CREATE TABLE permiso (
    id_permiso   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo       VARCHAR(80)  NOT NULL UNIQUE,
    descripcion  VARCHAR(200),
    CONSTRAINT ck_permiso_codigo CHECK (codigo ~ '^[a-z_]+\.[a-z_]+$')
);

CREATE TABLE rol (
    id_rol       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(60)  NOT NULL UNIQUE,
    descripcion  VARCHAR(200)
);

CREATE TABLE usuario (
    id_usuario      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_usuario  VARCHAR(50)  NOT NULL UNIQUE,
    correo          VARCHAR(120) NOT NULL UNIQUE,
    contrasena      VARCHAR(100) NOT NULL,          -- hash BCrypt, nunca texto plano
    nombres         VARCHAR(80),
    apellidos       VARCHAR(80),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    superusuario    BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_ingreso  TIMESTAMP
);

CREATE TABLE rol_permiso (
    id_rol      BIGINT NOT NULL REFERENCES rol (id_rol) ON DELETE CASCADE,
    id_permiso  BIGINT NOT NULL REFERENCES permiso (id_permiso),
    PRIMARY KEY (id_rol, id_permiso)
);

CREATE TABLE usuario_rol (
    id_usuario  BIGINT NOT NULL REFERENCES usuario (id_usuario),
    id_rol      BIGINT NOT NULL REFERENCES rol (id_rol),
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE usuario_permiso (
    id_usuario  BIGINT NOT NULL REFERENCES usuario (id_usuario),
    id_permiso  BIGINT NOT NULL REFERENCES permiso (id_permiso),
    PRIMARY KEY (id_usuario, id_permiso)
);

-- Permisos que ya usa el backend. Los demás módulos agregan los suyos
-- (por ejemplo lote.crear, pago.registrar) cuando se implementen.
INSERT INTO permiso (codigo, descripcion) VALUES
    ('usuario.gestionar',     'Crear, modificar y desactivar usuarios'),
    ('rol.gestionar',         'Crear y modificar roles y sus permisos'),
    ('seguimiento.ver',       'Consultar el seguimiento diario del crecimiento'),
    ('seguimiento.registrar', 'Registrar y modificar el seguimiento diario del crecimiento');

-- Privilegios mínimos del rol de la aplicación: solo DML, sin DDL.
-- GRANT SELECT, INSERT, UPDATE, DELETE ON permiso, rol, usuario, rol_permiso, usuario_rol, usuario_permiso TO pollos_app;
