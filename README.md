# Los Pollos Hermanos: backend

Backend del Proyecto Final de Bases de Datos 2 (Universidad El Bosque): gestión de una granja de
pollos de engorde (galpones, lotes, alimentación, sanidad, personal, gastos, ventas, cartera,
portal de clientes con pago en línea, seguimiento del crecimiento y auditoría).

Equipo: Juan López (Juancho), Dylan y Gabriela.

- Spring Boot 4, Java 21 y Maven.
- PostgreSQL: modelo relacional, reglas del negocio en procedimientos, funciones y triggers.
- MongoDB: seguimiento diario del crecimiento de los lotes.
- JWT para la sesión, Swagger para la documentación de la API.
- Wompi (ambiente de pruebas) para el pago en línea y correo con Thymeleaf para el comprobante.

El front está en el repositorio `pollos_hermanos_front_dos`.

## Cómo queda montado

| Pieza       | Dónde                        | Puerto |
|-------------|------------------------------|--------|
| PostgreSQL  | máquina virtual `nodoA`      | 5432   |
| MongoDB     | segunda máquina virtual      | 27017  |
| Backend     | computador (este proyecto)   | 8081   |
| Frontend    | computador (`ng serve`)      | 4200   |

El backend se conecta a PostgreSQL con el rol `pollos_app`, que solo tiene DML: el esquema se crea
con los scripts de `sql/` y Hibernate solo lo valida (`ddl-auto=validate`).

## Instalación de la base (PostgreSQL)

Se corre en `nodoA`, desde la carpeta del proyecto. `instalar_bd.sh` ejecuta los scripts como
superusuario sobre la base `pollos_hermanos`.

1. Base y roles (una sola vez; pide la contraseña de `pollos_app`):

   ```bash
   sudo -u postgres psql -d postgres -v ON_ERROR_STOP=1 < sql/modulo_00_base_y_roles.sql
   ```

2. Tablas de seguridad, módulos y correcciones, en este orden:

   ```bash
   ./instalar_bd.sh propuesta_seguridad.sql
   ./instalar_bd.sh 'modulo_0[1-9]*' 'modulo_1[01]*'
   ./instalar_bd.sh correccion_02_zona_horaria.sql
   ```

   `correccion_01_privilegios.sql` solo hace falta en una base donde el módulo 1 se instaló con la
   versión anterior (sin el `REVOKE`); en una instalación nueva no es necesaria.

Los objetos quedan a nombre de `pollos_owner` (sin login) y cada módulo deja a `pollos_app` solo
los privilegios que necesita.

### Pruebas SQL

Cada archivo de `sql/pruebas/` prueba las reglas de su módulo dentro de una transacción que se
deshace al final, así que no deja datos:

```bash
./instalar_bd.sh 'pruebas/*'
```

## Variables de entorno

Se copian de `.env.example` a `.env` (que no se sube a git) y se llenan. `DB_URL` y `MONGODB_URI`
no están en el ejemplo porque por defecto apuntan a `localhost`; se agregan cuando las bases están
en las máquinas virtuales.

| Variable                     | Para qué                                                         |
|------------------------------|------------------------------------------------------------------|
| `DB_URL`                     | `jdbc:postgresql://<ip de nodoA>:5432/pollos_hermanos`           |
| `DB_PASSWORD`                | contraseña de `pollos_app` (la del paso 1)                       |
| `MONGODB_URI`                | `mongodb://<ip de la VM de Mongo>:27017/pollos_hermanos`          |
| `JWT_SECRET`                 | secreto del token, mínimo 32 caracteres                          |
| `ADMIN_USUARIO`, `ADMIN_CORREO`, `ADMIN_CONTRASENA` | superusuario que se crea al primer arranque |
| `CORS_ALLOWED_ORIGINS`       | origen del front (por defecto `http://localhost:4200`)           |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NOMBRE` | cuenta de correo del asadero (contraseña de aplicación de Gmail) |
| `WOMPI_LLAVE_PUBLICA`, `WOMPI_SECRETO_INTEGRIDAD`, `WOMPI_SECRETO_EVENTOS` | llaves de pruebas de Wompi (`pub_test_`, `test_integrity_`, `test_events_`) |

Sin las llaves de Wompi la aplicación arranca, pero el pago en línea responde que la pasarela no
está configurada.

## Arrancar y probar

```bash
./mvnw test                   # pruebas unitarias (no necesitan base de datos)
set -a; source .env; set +a   # carga las variables en la terminal
./mvnw spring-boot:run        # arranca en http://localhost:8081
```

- Documentación de la API: http://localhost:8081/swagger-ui.html
- Para entrar, se usa el superusuario de `ADMIN_USUARIO` desde el front o con `POST /auth/login`.

`desplegar.sh` recompila y reinicia el backend cuando corre como servicio; `configurar_wompi.sh`
guarda las llaves de Wompi en la configuración de ese servicio.
