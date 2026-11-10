\prompt 'Contraseña para pollos_app: ' clave_app

CREATE ROLE pollos_owner NOLOGIN;
CREATE ROLE pollos_app LOGIN PASSWORD :'clave_app';

CREATE DATABASE pollos_hermanos
    OWNER pollos_owner
    ENCODING 'UTF8'
    TEMPLATE template0;

\connect pollos_hermanos

-- Solo pollos_app (además del dueño) puede conectarse.
REVOKE ALL ON DATABASE pollos_hermanos FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE pollos_hermanos TO pollos_app;

-- Nadie más que el dueño crea objetos en el esquema public.
ALTER SCHEMA public OWNER TO pollos_owner;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO pollos_app;

-- Toda tabla y secuencia nueva de pollos_owner queda disponible para la
-- aplicación. Cada módulo luego hace REVOKE ALL y deja solo lo que necesita.
ALTER DEFAULT PRIVILEGES FOR ROLE pollos_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pollos_app;
ALTER DEFAULT PRIVILEGES FOR ROLE pollos_owner IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO pollos_app;
