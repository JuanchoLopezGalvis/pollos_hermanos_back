# Los Pollos Hermanos: backend

Backend del Proyecto Final de Bases de Datos 2 (Universidad El Bosque, Ingeniería de Sistemas).
Sistema web de gestión de producción y venta de pollos de engorde para una finca avícola
con una sola sede y varios galpones. Equipo: Dylan Mancipe (gerente y experto del negocio),
Juan Diego López (base de datos e infraestructura) y Gabriela Poveda (frontend, calidad y documentación).

Responde y documenta siempre en español.

## Stack

- Java 21, Spring Boot 4.0.x, Maven, empaquetado jar.
- PostgreSQL con PL/pgSQL: datos transaccionales y lógica de negocio.
- MongoDB: un solo módulo, el seguimiento diario del crecimiento de los lotes.
- Frontend en Angular, en un proyecto aparte (`final_project_front` como referencia de estructura).
- Despliegue en una máquina virtual con Ubuntu Server.
- Dependencias de Initializr: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Data MongoDB,
  Validation, Spring Security, Spring Boot DevTools, Java Mail Sender, Thymeleaf.
- Dependencias agregadas a mano: springdoc-openapi 3.x (Swagger), jjwt (JWT), ModelMapper.
- No usar Lombok. Para DTO se permiten `record` de Java.

## Comandos

- Ejecutar: `./mvnw spring-boot:run`
- Pruebas: `./mvnw test`
- Swagger: `/swagger-ui.html`

## Estructura de paquetes

Paquete raíz: `co.edu.unbosque.pollos_hermanos_back`. Sigue la misma organización del proyecto
anterior del equipo (Donatello IA):

- `configuration`: beans y configuración (OpenAPI, correo, Mongo).
- `controller`: controladores REST, documentados con anotaciones de springdoc.
- `dto`: objetos de entrada y salida de la API.
- `entity`: entidades JPA de PostgreSQL.
- `document`: documentos de MongoDB.
- `repository`: repositorios JPA y de MongoDB.
- `service`: lógica de aplicación. Los servicios CRUD implementan `CRUDOperation<D>`.
- `exception`: excepciones propias y su manejo global con `@RestControllerAdvice`.
- `security`: JWT, filtro de autenticación y configuración de Spring Security.

Convenciones:
- Clases del dominio con nombres en español: `Lote`, `Galpon`, `Pedido`, `DetallePedido`.
- JavaDoc en español en todas las clases y métodos públicos.
- Una clase de prueba JUnit por cada controller y cada service.
- Conversión entre entidad y DTO con ModelMapper.

## Reglas de arquitectura

- Las reglas de negocio viven en PostgreSQL (procedimientos, funciones, triggers, índices y CHECK).
  El backend las invoca y no las duplica. En Java solo se valida formato con Bean Validation.
- Los procedimientos se llaman con `JdbcTemplate` o `SimpleJdbcCall` (`CALL sp_...`) y las funciones con `SELECT fn_...`.
- Cuando PostgreSQL rechaza una operación (`RAISE EXCEPTION`, SQLSTATE `P0001` o violación de
  restricción), el backend responde 400 o 409 con un mensaje claro en español, nunca 500.
- Ningún dato se guarda en las dos bases. Mortalidad, alimento, sanidad, costos, ventas y pagos
  van solo en PostgreSQL. MongoDB solo guarda el seguimiento del crecimiento.
- Antes de guardar un documento en MongoDB, verificar en PostgreSQL que el lote existe y está activo.
  La edad del lote se calcula con su fecha de ingreso en PostgreSQL.
- El modelo relacional aprobado está en `docs/modelo_relacional.md`. No inventes tablas ni columnas
  fuera de ese documento: si falta una definición, pregunta antes de crearla.
- Cada módulo tiene su script en `sql/modulo_NN_*.sql` (se ejecuta como superusuario y deja los
  objetos a nombre de `pollos_owner`) y sus pruebas en `sql/pruebas/`. Se instala con
  `./instalar_bd.sh modulo_NN_*.sql`.
- La base real tiene `ALTER DEFAULT PRIVILEGES` que da SELECT, INSERT, UPDATE y DELETE a
  `pollos_app` en toda tabla nueva. Cada script de módulo debe hacer `REVOKE ALL ... FROM pollos_app`
  antes de sus `GRANT`, para que la aplicación no pueda borrar lo que no debe.

## Reglas del negocio

Lotes y galpones
- Cada galpón tiene capacidad máxima y estado (disponible, ocupado, en mantenimiento).
- Producción escalonada: un galpón aloja VARIOS lotes a la vez (entra uno nuevo cada semana).
  La suma de aves vivas de sus lotes activos no supera la capacidad. Cada lote se sigue por separado.
- Estados del lote: en engorde, listo para venta, vendido, cerrado. Engorde de 35 a 45 días
  (venta estimada por defecto: ingreso + 40).
- Aves disponibles = cantidad inicial - mortalidad registrada - aves vendidas.
- Mortalidad de referencia: 5% por lote. El usuario registra la mortalidad real cada día;
  el sistema alerta cuando la acumulada supera el 5%.

Alimentación (por edad del lote)
- Preiniciación días 1 a 7 (con vitaminas en agua), iniciación 8 a 21, engorde 22 a 35,
  finalización desde el día 36 hasta la venta.
- Compras por bultos con costo unitario. El consumo se valida contra el inventario y contra la
  etapa que corresponde a la edad del lote, y se valora al costo promedio ponderado del bulto.

Sanidad
- Compras de vitaminas y medicamentos con costo unitario; aplicaciones por lote con dosis.
- Solo un empleado activo con cargo de encargado de sanidad puede registrar una aplicación.
- El costo de cada aplicación se carga al lote. No se lleva inventario de medicamentos.

Costos
- Costo del lote = pollitos de un día + alimentación + vitaminas y medicamentos
  + beneficio + otros gastos (mano de obra, servicios, transporte).
- Beneficio (sacrificio y pelado de las aves): se registra lote, fecha, aves beneficiadas
  y costo por ave; su total se suma al costo del lote.
- Gastos por lote o generales de la finca, clasificados por categoría.

Comercial
- Un pedido puede tomar aves de varios lotes (`DetallePedido`, relación N:M);
  solo se piden aves de lotes en estado "listo para venta".
- La venta nace de un pedido confirmado y se pesa en LIBRAS: peso total en libras y precio por
  libra, que queda fijo al registrarla. Peso promedio de salida = libras / aves vendidas.
  La venta tiene fecha límite de pago y estado.
- Venta y entrega son 1:1. La entrega la hace un empleado con cargo de transportista.

Pagos y cartera
- Pagos parciales con método de pago. Saldo = valor total - pagos.
- Pago en línea con una pasarela en ambiente de pruebas (sandbox), sin dinero real.
  El pago solo se registra cuando la pasarela confirma la transacción como aprobada.
  Los datos de tarjeta nunca pasan por el backend.
- La cartera vencida se calcula con una función y la marca un procedimiento,
  no un trigger, porque depende del paso del tiempo.

Personal
- Empleados con cargo. Los retirados conservan su historial, pero no se asignan a nuevos registros.

Catálogos (3FN): CategoriaGasto, Cargo, TipoCliente, MetodoPago, TipoAlimento.

Seguimiento y balance
- No se registran temperatura ni humedad (las condiciones de la zona son favorables).
- Un lote puede tener varios seguimientos el mismo día en MongoDB.
- Otros ingresos del lote (por ejemplo, gallinaza) suman en la ganancia.
  Ganancia neta = ingresos totales - costos totales, por lote.

Fuera del alcance: facturación electrónica DIAN, integración contable, varias sedes o monedas,
nómina, inventario de medicamentos, básculas o sensores, app móvil, despliegue en producción.

## Correo

Java Mail Sender con plantillas Thymeleaf en `src/main/resources/templates`. Usos previstos:
confirmación de pedido, venta y pago; aviso de cartera vencida; recuperación de contraseña;
alerta de mortalidad mayor al 5%.

## Seguridad y secretos

- Nunca escribir contraseñas, claves de API ni el secreto de JWT en `application.properties`
  ni en ningún archivo versionado. Usar variables de entorno, por ejemplo
  `spring.datasource.password=${DB_PASSWORD}`.
- Contraseñas de usuarios con BCrypt. Autenticación con JWT. La aplicación se conecta a PostgreSQL
  con un rol de privilegios mínimos, sin permisos de DDL.

## Git

- El profesor evalúa la participación equitativa de los tres integrantes en el historial.
- Ramas por funcionalidad (`feature/lotes`, `feature/pagos`) y commits descriptivos en español.
- No hacer commit ni push sin que el usuario lo pida.

## Documentos de referencia

En `docs/` (léelos cuando necesites detalle; no los cargues siempre):
- `docs/Proyecto_Final_v20261.pdf`: enunciado y rúbrica del profesor.
- `docs/Documento_supuestos_BD2.pdf`: supuestos del negocio.
- `docs/Acta_Constitucion_Pollos_Engorde.docx`: acta de constitución con alcance y decisiones.
