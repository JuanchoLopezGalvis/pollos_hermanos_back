# Modelo relacional — Los Pollos Hermanos

Propuesta de modelo relacional en 3FN para PostgreSQL. Se construyó a partir del Acta de
Constitución (22/10/2026), el Documento de Supuestos, la Propuesta, el enunciado y el
`CLAUDE.md`, junto con las decisiones ya tomadas por el equipo.

> **Estado: aprobado por el equipo el 24/10/2026**, incluidas las decisiones D4 a D9, y ajustado ese
> mismo día con el documento de ajustes del negocio (sección 1.4). Implementado en `sql/modulo_01` a
> `sql/modulo_07`; donde este documento y los scripts difieran, mandan los ajustes de la sección 1.4.

---

## 1. Decisiones de diseño

### 1.1 Conflictos entre documentos (se sigue el más reciente: el acta)

| # | Documento de Supuestos dice | Este modelo hace | Motivo |
|---|---|---|---|
| D1 | S4: no hay integración con pasarelas de pago. | Hay pago en línea con una pasarela en *sandbox* (método `EN_LINEA`). | El acta lo incluye en el alcance. |
| D2 | S14: la mortalidad diaria va en MongoDB. | La mortalidad va en la tabla `mortalidad` de PostgreSQL. MongoDB guarda solo peso, ambiente, observaciones, muestreos y novedades. | Decisión del equipo y del acta: ningún dato se guarda en las dos bases. |
| D3 | S24: cuatro cargos. | Se agrega un quinto cargo: `BENEFICIADORA`. | Decisión del equipo sobre el beneficio. |
| D4 | S23: las categorías de gasto incluyen alimentación y sanidad. | `categoria_gasto` incluye solo mano de obra, servicios públicos, transporte y otros. La alimentación sale de `consumo_alimento`, la sanidad de `aplicacion_sanitaria` y el beneficio de `detalle_beneficio`. Los pagos a las beneficiadoras se registran solo en `detalle_beneficio`, nunca como gasto de mano de obra; esa categoría es para otros trabajos de la finca. | Si también se registraran como gasto, se contarían dos veces en el costo del lote. |

### 1.2 Otras decisiones que requieren aprobación

| # | Decisión | Alternativa descartada |
|---|---|---|
| D5 | **Pedido–Venta es 1:1.** Una venta vende exactamente las aves del pedido, tomadas de `detalle_pedido`. | Ventas parciales de un mismo pedido: requieren una tabla `detalle_venta` y complican el modelo. |
| D6 | **Ingreso por lote en la rentabilidad.** La venta guarda el peso total del pedido. Cuando un pedido mezcla lotes, el ingreso se reparte entre ellos en proporción a las aves de cada uno. | Pesar por separado las aves de cada lote en cada venta. |
| D7 | **Los gastos generales de la finca no se reparten entre los lotes.** Aparecen en los reportes, pero no suman al costo de ningún lote. | Prorratearlos por aves o por días de ocupación del galpón. |
| D8 | **Valoración congelada.** Al registrar un consumo de alimento o una aplicación sanitaria, un trigger guarda el costo promedio ponderado vigente en esa fecha (`costo_unitario`). Así, el costo de un lote no cambia con compras posteriores, igual que el precio por kilo de la venta (S34). | Recalcular el costo cada vez: el costo de un lote ya cerrado cambiaría al comprar más alimento. |
| D9 | **Nueva tabla `etapa_alimentacion`.** Guarda los rangos de días de cada etapa. Si esos rangos estuvieran en `tipo_alimento` habría una dependencia transitiva (tipo → etapa → días), lo que rompe la 3FN. | Guardar el rango de días dentro de `tipo_alimento`. |
| D10 | La cantidad solicitada de un pedido (S31) **no se guarda**: es la suma de `detalle_pedido.cantidad_aves`. | Guardarla en `pedido` (dato derivado y redundante). |
| D11 | `venta.valor_total` es una columna `GENERATED ALWAYS AS (peso_total_kg * precio_kilo) STORED`. Cumple S33 sin que el valor pueda quedar inconsistente. | Una columna normal actualizada a mano. |
| D12 | Un lote está **activo** cuando su estado es `EN_ENGORDE` o `LISTO_PARA_VENTA`. Esta definición la usan el índice de "un lote activo por galpón" y la verificación de MongoDB. | — |
| D13 | Supuesto 3 (fecha de creación y estado). Todas las tablas tienen `fecha_creacion`. Las tablas maestras (catálogos, empleado, proveedor, cliente, producto) tienen `activo`. Las transaccionales tienen su propio estado de negocio cuando aplica. | Poner un campo `activo` en cada consumo o pago, lo que no tiene sentido en el negocio. |

### 1.4 Ajustes del documento del negocio (24/10/2026)

Estos ajustes reemplazan lo que digan las secciones siguientes y los documentos de Fase 1
(acta, supuestos, propuesta), que el equipo debe actualizar para que coincidan.

| # | Antes | Ahora | Dónde |
|---|---|---|---|
| D14 | S7: un galpón aloja un solo lote activo (índice `ux_lote_activo_por_galpon`). | **Producción escalonada:** un galpón aloja varios lotes a la vez (uno nuevo por semana). La suma de aves vivas de sus lotes activos no supera la capacidad (`fn_aves_alojadas`). El galpón está `OCUPADO` mientras tenga al menos un lote activo. | `modulo_07` |
| D15 | S5: ciclo de 40 a 50 días. | Engorde de **35 a 45 días**; venta estimada por defecto: ingreso + 40. | `modulo_07` |
| D16 | Venta en kilos (`peso_total_kg`, `precio_kilo`). | Venta en **libras**: `peso_total_libras`, `precio_libra`; peso promedio de salida = libras / aves vendidas. | `modulo_06` |
| D17 | Ingreso del lote = solo ventas. | Nueva tabla `otro_ingreso` (lote, fecha, monto, descripción) para ingresos como la gallinaza. **Ganancia neta = ingresos totales − costos totales.** | `modulo_06` |
| D18 | MongoDB guardaba temperatura y humedad. | No se registran temperatura ni humedad (condiciones favorables de la zona). Se permiten varios seguimientos del mismo lote el mismo día. | backend y front |
| D19 | El beneficio solo en lotes `LISTO_PARA_VENTA`. | También en lotes `VENDIDO` (el beneficio puede registrarse el día de la última venta), nunca en `CERRADO`. | `modulo_05` |
| D20 | Mensajes de restricciones en un `CASE` dentro de la función. | Tabla `mensaje_restriccion`: cada módulo agrega los suyos; la usan los procedimientos y el backend. | `modulo_02` |

Nuevas vistas para el seguimiento por lote y el panel del galpón: `v_lote_resumen` (ahora con aves
vendidas y consumo de alimento), `v_galpon_ocupacion` y `v_rentabilidad_lote` (costos, ingresos y
ganancia por lote).

### 1.5 Portal de clientes (aprobado el 25/10/2026)

Los clientes crean su cuenta e inician sesión **solo** para hacer pedidos y pagarlos. Script nuevo:
`modulo_08_portal_clientes.sql`.

**Flujo**

1. El cliente se registra: nombre, documento, tipo de cliente, teléfono o correo, dirección,
   nombre de usuario y contraseña. Se crean en una sola transacción su `usuario` (con el rol
   `Cliente`) y su `cliente`.
2. El cliente pide **una cantidad de aves** y la fecha deseada de entrega. No ve ni elige lotes.
3. PostgreSQL reparte el pedido **por antigüedad (FIFO)**: toma las aves libres del lote
   `LISTO_PARA_VENTA` que más tiempo lleva disponible, y si no alcanza sigue con el siguiente.
   Si entre todos no alcanza, el pedido **se rechaza de inmediato** con el mensaje
   "Solo hay N aves disponibles". El pedido queda `PENDIENTE`, con sus lotes ya apartados.
4. El trabajador revisa el pedido, lo **aprueba** (`CONFIRMADO`) o lo cancela. Al pesar las aves,
   registra la venta con las libras y el **precio por libra**. Ahí queda el valor a pagar.
5. El cliente ve en su portal el valor y el saldo de cada venta y **paga con Wompi**, en un solo
   pago o en abonos. El pago entra por el mismo flujo de pago en línea que ya existe.
6. Mientras el pedido siga `PENDIENTE`, el cliente puede cancelarlo; así libera las aves.

| # | Decisión | Dónde |
|---|---|---|
| D21 | `cliente.id_usuario BIGINT NULL UNIQUE`, con FK a `usuario`. Es una relación 1:1 opcional: los clientes que registra el personal no tienen cuenta. | `modulo_08` |
| D22 | "El lote que más tiempo lleva disponible" = el de **`fecha_estimada_venta` más antigua**; si empatan, el de menor `id_lote`. No se guarda la fecha en que el lote pasó a `LISTO_PARA_VENTA`. | `fn_repartir_pedido_fifo` |
| D23 | Los pedidos `PENDIENTE` **también apartan aves** (`fn_aves_comprometidas` hoy solo cuenta los `CONFIRMADO`). Sin este cambio, dos clientes podrían recibir las mismas aves. El procedimiento bloquea los lotes (`FOR UPDATE`) mientras reparte. Esto aplica también a los pedidos que crea el personal. | `modulo_08` |
| D24 | Rol `Cliente` con un único permiso, `portal.cliente`. Las rutas `/portal/**` exigen ese permiso y filtran **siempre** por el cliente del usuario autenticado: un cliente no puede ver pedidos, ventas ni pagos de otro aunque cambie el id en la URL. Las demás rutas siguen igual, así que un cliente no entra a galpones, lotes, costos, etc. | backend |
| D25 | Si ya existe un cliente con ese documento, el registro se rechaza con el mensaje "Ya estás registrado como cliente; pide a la finca que active tu cuenta". El personal lo vincula desde la pantalla de clientes creándole la cuenta. Así nadie puede apropiarse de la ficha de otro conociendo su documento. | backend y front |
| D26 | Se usa el mismo login para todos. Después de iniciar sesión, el cliente va a `/portal` y el personal a su panel. | front |

**Objetos nuevos**

- `fn_repartir_pedido_fifo(p_id_pedido, p_cantidad_aves)`: bloquea los lotes listos para venta,
  rechaza el pedido si no alcanzan y llena `detalle_pedido` por antigüedad. Es una función (sin
  COMMIT) para poder probarla dentro de una transacción.
- `sp_crear_pedido_cliente(p_id_cliente, p_cantidad_aves, p_fecha_estimada_entrega,
  p_observaciones, OUT p_id_pedido)`: crea el pedido y llama al reparto; hace COMMIT o ROLLBACK.
- `fn_aves_libres_lote(p_id_lote)` y `fn_aves_libres_venta()`: aves que se pueden pedir. El portal
  muestra solo el total, nunca los lotes.
- El registro del cliente no usa procedimiento: el backend crea el `usuario` (JPA) y el `cliente`
  (INSERT con `id_usuario`) en una sola transacción de Spring, porque un procedimiento con COMMIT
  no puede participar en ella. Las reglas siguen en la base: `uq_cliente_documento`,
  `uq_cliente_usuario` y `ck_cliente_contacto`.
- `v_cliente` agrega al final `id_usuario` y `usuario`. `v_pedido` y `v_venta` no cambian: el
  portal las consulta filtrando por `id_cliente`.

### 1.6 Pagos solo con Wompi y entrega en la dirección del cliente (25/10/2026)

Script: `modulo_09_pago_en_linea_y_entrega.sql`.

| # | Decisión | Dónde |
|---|---|---|
| D27 | **El único medio de pago es Wompi** (`EN_LINEA`) y solo lo usa el cliente desde su portal. Efectivo, transferencia y crédito quedan inactivos en `metodo_pago`, y `fn_trg_pago_validar` rechaza cualquier pago que no sea en línea o que no traiga la referencia de la transacción. El personal ya no registra pagos: solo los consulta. Los pagos anteriores con otros medios se conservan como historial. | `modulo_09`, backend y front |
| D28 | **La entrega va a la dirección que registró el cliente.** `fn_trg_entrega_direccion` la copia en `entrega.direccion` (queda como constancia) y no deja cambiarla; si el cliente no tiene dirección, la entrega se rechaza. `sp_registrar_entrega` ya no recibe dirección. | `modulo_09` |
| D29 | `v_venta` agrega al final `direccion_cliente`, para mostrarla al programar la entrega. | `modulo_09` |

| D30 | **Wompi avisa al servidor** (URL de eventos `https://nodoa.tail6f73fd.ts.net/api/pagos-en-linea/eventos`, publicada con Tailscale Funnel). Así el pago se registra aunque el cliente cierre la ventana antes de volver. El backend verifica la firma del aviso con `WOMPI_SECRETO_EVENTOS`, consulta la transacción en Wompi y la registra; recibirla varias veces no la duplica (`uq_pago_referencia`). No cambia el modelo. | backend |

Una venta queda `PAGADA` solo cuando los pagos aprobados por Wompi cubren su valor (`trg_pago_venta`).
El backend registra un pago solo después de consultar la transacción directamente en Wompi y
recibir `APPROVED`; un pago rechazado (`DECLINED`) o pendiente no se registra.

### 1.3 Convenciones

- Nombres de tablas y columnas en `snake_case` y singular, igual que las tablas ya existentes `usuario`, `rol` y `permiso`.
- La PK de cada tabla es `id_<tabla> BIGINT GENERATED ALWAYS AS IDENTITY`. Las tablas N:M usan PK compuesta.
- Dinero: `NUMERIC(14,2)` en COP. Cantidades de aves: `INTEGER`. Pesos: `NUMERIC(10,3)` en kg.
- Estados con tipos `ENUM` de PostgreSQL, como dice el acta (criterio C3).
- Catálogos con `codigo` estable (`VARCHAR(30)`, en mayúsculas). Los triggers validan por `codigo`, nunca por el nombre visible.
- `fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` en todas las tablas. En las tablas siguientes se omite para no repetirla.

---

## 2. Tipos enumerados

| Tipo | Valores | Supuesto |
|---|---|---|
| `estado_galpon` | `DISPONIBLE`, `OCUPADO`, `MANTENIMIENTO` | S6 |
| `estado_lote` | `EN_ENGORDE`, `LISTO_PARA_VENTA`, `VENDIDO`, `CERRADO` | S10 |
| `estado_pedido` | `PENDIENTE`, `CONFIRMADO`, `ENTREGADO`, `CANCELADO` | S31 |
| `estado_venta` | `PENDIENTE_PAGO`, `PAGADA`, `VENCIDA` | S37, S38 |
| `estado_entrega` | `PENDIENTE`, `ENTREGADO`, `DEVUELTO` | S40 |
| `tipo_producto_sanitario` | `VITAMINA`, `MEDICAMENTO` | S18 |

---

## 3. Tablas

### Módulo 0 — Seguridad (ya existe, no cambia)

`usuario`, `rol`, `permiso`, `rol_permiso`, `usuario_rol` y `usuario_permiso` quedan como en
`sql/propuesta_seguridad.sql` y en las entidades JPA actuales.

### Módulo 1 — Catálogos

#### `cargo`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_cargo | BIGINT | PK |
| codigo | VARCHAR(30) | NOT NULL, UNIQUE, CHECK `codigo ~ '^[A-Z_]+$'` |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

Datos iniciales: `OPERARIO_GALPON`, `ENCARGADO_SANIDAD`, `TRANSPORTISTA`, `ADMINISTRADOR`, `BENEFICIADORA`.
**Regla:** S24 y D3.

#### `tipo_cliente`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_tipo_cliente | BIGINT | PK |
| codigo | VARCHAR(30) | NOT NULL, UNIQUE |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

Datos iniciales: `TIENDA`, `RESTAURANTE`, `PERSONA_NATURAL`. **Regla:** S28.

#### `metodo_pago`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_metodo_pago | BIGINT | PK |
| codigo | VARCHAR(30) | NOT NULL, UNIQUE |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

Datos iniciales: `EFECTIVO`, `TRANSFERENCIA`, `CREDITO`, `EN_LINEA` (pasarela *sandbox*). **Regla:** S36 y D1.

#### `categoria_gasto`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_categoria_gasto | BIGINT | PK |
| codigo | VARCHAR(30) | NOT NULL, UNIQUE |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

Datos iniciales: `MANO_OBRA`, `SERVICIOS_PUBLICOS`, `TRANSPORTE`, `OTROS`. **Regla:** S23 y D4.

#### `etapa_alimentacion`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_etapa | BIGINT | PK |
| nombre | VARCHAR(40) | NOT NULL, UNIQUE |
| orden | SMALLINT | NOT NULL, UNIQUE, CHECK `> 0` |
| dia_inicio | SMALLINT | NOT NULL, CHECK `>= 1` |
| dia_fin | SMALLINT | NULL (sin tope = hasta la venta), CHECK `dia_fin IS NULL OR dia_fin >= dia_inicio` |

Datos iniciales: Preiniciación 1–7, Iniciación 8–21, Engorde 22–35 y Finalización 36–(sin tope).
**Regla:** etapas del acta y D9.

#### `tipo_alimento`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_tipo_alimento | BIGINT | PK |
| nombre | VARCHAR(80) | NOT NULL, UNIQUE |
| id_etapa | BIGINT | NOT NULL, FK → `etapa_alimentacion` |
| peso_bulto_kg | NUMERIC(6,2) | NOT NULL DEFAULT 40, CHECK `> 0` |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

**Regla:** catálogo `TipoAlimento`. Cada tipo de alimento pertenece a una sola etapa.

### Módulo 2 — Personal

#### `empleado`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_empleado | BIGINT | PK |
| documento | VARCHAR(20) | NOT NULL, UNIQUE |
| nombres | VARCHAR(80) | NOT NULL |
| apellidos | VARCHAR(80) | NOT NULL |
| telefono | VARCHAR(20) | NULL |
| id_cargo | BIGINT | NOT NULL, FK → `cargo` |
| fecha_ingreso | DATE | NOT NULL |
| fecha_retiro | DATE | NULL, CHECK `fecha_retiro >= fecha_ingreso` |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

CHECK `ck_empleado_retiro`: `(activo AND fecha_retiro IS NULL) OR (NOT activo AND fecha_retiro IS NOT NULL)`.
**Reglas:** S24 (un cargo por empleado), S27 (un empleado retirado conserva su historial). No se borra, se retira con `sp_retirar_empleado`.
La función `fn_validar_empleado(id_empleado, codigo_cargo)` lanza una excepción si el empleado
no está activo o no tiene ese cargo. La usan los triggers de sanidad, beneficio y entrega.

### Módulo 3 — Galpones y lotes *(primer módulo a implementar)*

#### `galpon`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_galpon | BIGINT | PK |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| capacidad_maxima | INTEGER | NOT NULL, CHECK `> 0` |
| estado | estado_galpon | NOT NULL DEFAULT `'DISPONIBLE'` |

**Reglas:** S6. El estado `OCUPADO` no se escribe a mano: lo pone y lo quita el trigger de `lote`.
Un galpón `OCUPADO` no puede pasar a `MANTENIMIENTO` (trigger `trg_galpon_estado`).

#### `lote`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_lote | BIGINT | PK |
| codigo | VARCHAR(20) | NOT NULL, UNIQUE |
| id_galpon | BIGINT | NOT NULL, FK → `galpon` |
| fecha_ingreso | DATE | NOT NULL |
| cantidad_inicial | INTEGER | NOT NULL, CHECK `> 0` |
| costo_pollito | NUMERIC(14,2) | NOT NULL, CHECK `>= 0` (costo unitario del pollito de un día) |
| fecha_estimada_venta | DATE | NOT NULL, CHECK `BETWEEN fecha_ingreso + 40 AND fecha_ingreso + 50` |
| estado | estado_lote | NOT NULL DEFAULT `'EN_ENGORDE'` |

Índice único parcial `ux_lote_activo_por_galpon` sobre `(id_galpon)` con
`WHERE estado IN ('EN_ENGORDE','LISTO_PARA_VENTA')`.

**Reglas:**
- S7 (un solo lote activo por galpón): la cubre el índice parcial, sin código (acta, C3).
- S8 (capacidad): trigger `trg_lote_validar` BEFORE INSERT/UPDATE. Comprueba que `cantidad_inicial <= galpon.capacidad_maxima` y que el galpón no esté en `MANTENIMIENTO`.
- S5 (ciclo de 40 a 50 días): CHECK. Si no se indica fecha, `sp_registrar_lote` usa `fecha_ingreso + 45`.
- Estado del galpón: trigger `trg_lote_galpon` AFTER INSERT/UPDATE. Pone el galpón en `OCUPADO` al crear el lote y en `DISPONIBLE` cuando el lote pasa a `VENDIDO` o `CERRADO`.
- RF-02 y S12 (transiciones válidas), que valida `trg_lote_estado`:
  - `EN_ENGORDE → LISTO_PARA_VENTA`
  - `LISTO_PARA_VENTA → VENDIDO`: automática cuando se vende la última ave.
  - `EN_ENGORDE | LISTO_PARA_VENTA → CERRADO`: solo si las aves disponibles son 0 (por ejemplo, mortalidad total).
  - `VENDIDO → CERRADO`
  - Cualquier otro cambio lanza una excepción.
- Costo de pollitos = `cantidad_inicial * costo_pollito`.

#### `mortalidad`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_mortalidad | BIGINT | PK |
| id_lote | BIGINT | NOT NULL, FK → `lote` |
| fecha | DATE | NOT NULL |
| cantidad | INTEGER | NOT NULL, CHECK `> 0` |
| causa | VARCHAR(200) | NULL |

UNIQUE `(id_lote, fecha)`: un registro por lote y día.
**Reglas:**
- Mortalidad diaria registrada en PostgreSQL (D2).
- Trigger `trg_mortalidad_validar`: el lote debe estar activo, la fecha debe estar entre `fecha_ingreso` y hoy, y `cantidad <= fn_aves_disponibles(lote)`.
- Alerta del 5%: `sp_registrar_mortalidad` devuelve `OUT alerta BOOLEAN` cuando `fn_mortalidad_pct(lote) > 5`. Con eso el backend avisa en la interfaz y envía el correo.

### Módulo 4 — Alimentación

#### `proveedor`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_proveedor | BIGINT | PK |
| nit | VARCHAR(20) | NOT NULL, UNIQUE |
| nombre | VARCHAR(120) | NOT NULL |
| telefono | VARCHAR(20) | NULL |
| correo | VARCHAR(120) | NULL |
| direccion | VARCHAR(200) | NULL |
| suministra_alimento | BOOLEAN | NOT NULL DEFAULT FALSE |
| suministra_sanidad | BOOLEAN | NOT NULL DEFAULT FALSE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

CHECK `suministra_alimento OR suministra_sanidad`. **Regla:** S17, S21 y RF-04.

#### `compra_alimento`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_compra_alimento | BIGINT | PK |
| id_proveedor | BIGINT | NOT NULL, FK → `proveedor` |
| id_tipo_alimento | BIGINT | NOT NULL, FK → `tipo_alimento` |
| fecha | DATE | NOT NULL |
| cantidad_bultos | INTEGER | NOT NULL, CHECK `> 0` |
| costo_unitario | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |

**Reglas:** S15. El trigger `trg_compra_alimento_validar` exige que el proveedor esté activo y tenga `suministra_alimento`.

#### `consumo_alimento`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_consumo_alimento | BIGINT | PK |
| id_lote | BIGINT | NOT NULL, FK → `lote` |
| id_tipo_alimento | BIGINT | NOT NULL, FK → `tipo_alimento` |
| fecha | DATE | NOT NULL |
| cantidad_bultos | NUMERIC(8,2) | NOT NULL, CHECK `> 0` |
| costo_unitario | NUMERIC(14,2) | NOT NULL (lo asigna el trigger, D8) |

UNIQUE `(id_lote, id_tipo_alimento, fecha)`.
**Reglas:** trigger `trg_consumo_alimento_validar` BEFORE INSERT:
- El lote está activo y `fecha >= fecha_ingreso`.
- La etapa del tipo de alimento contiene `fn_edad_lote(lote, fecha)` (alimento según la edad del lote).
- S16: `cantidad_bultos <= fn_inventario_alimento(tipo)`. El inventario es la suma de los bultos comprados menos la suma de los consumidos.
- Asigna `costo_unitario = fn_costo_promedio_alimento(tipo, fecha)`, el promedio ponderado de las compras hasta esa fecha.

### Módulo 5 — Sanidad

#### `producto_sanitario`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_producto | BIGINT | PK |
| nombre | VARCHAR(100) | NOT NULL |
| tipo | tipo_producto_sanitario | NOT NULL |
| presentacion | VARCHAR(80) | NOT NULL (por ejemplo, "frasco 500 ml") |
| unidad_medida | VARCHAR(20) | NOT NULL (ml, g, dosis) |
| id_proveedor | BIGINT | NOT NULL, FK → `proveedor` |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

UNIQUE `(nombre, presentacion)`. **Regla:** S18. El trigger exige que el proveedor tenga `suministra_sanidad`.

#### `compra_sanitaria`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_compra_sanitaria | BIGINT | PK |
| id_producto | BIGINT | NOT NULL, FK → `producto_sanitario` |
| fecha | DATE | NOT NULL |
| cantidad | NUMERIC(10,2) | NOT NULL, CHECK `> 0` (en la unidad del producto) |
| costo_unitario | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |

El proveedor se obtiene a través del producto, así que no se repite aquí (3FN).
**Regla:** compras de vitaminas y medicamentos con costo unitario. No se lleva inventario (fuera del alcance).

#### `aplicacion_sanitaria`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_aplicacion | BIGINT | PK |
| id_lote | BIGINT | NOT NULL, FK → `lote` |
| id_producto | BIGINT | NOT NULL, FK → `producto_sanitario` |
| id_empleado | BIGINT | NOT NULL, FK → `empleado` |
| fecha | DATE | NOT NULL |
| dosis | NUMERIC(10,2) | NOT NULL, CHECK `> 0` (cantidad aplicada) |
| costo_unitario | NUMERIC(14,2) | NOT NULL (lo asigna el trigger, D8) |
| observaciones | VARCHAR(300) | NULL |

**Reglas:** S19, S20 y S25. El trigger `trg_aplicacion_validar` comprueba:
- `fn_validar_empleado(id_empleado, 'ENCARGADO_SANIDAD')`.
- El lote está activo y `fecha >= fecha_ingreso`.
- El producto tiene al menos una compra registrada (si no, no hay costo con qué valorar la aplicación).
- Asigna `costo_unitario = fn_costo_promedio_producto(producto, fecha)`.

El costo cargado al lote es `dosis * costo_unitario`.

### Módulo 6 — Gastos y beneficio

#### `gasto`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_gasto | BIGINT | PK |
| id_categoria_gasto | BIGINT | NOT NULL, FK → `categoria_gasto` |
| id_lote | BIGINT | NULL, FK → `lote` (NULL = gasto general de la finca) |
| fecha | DATE | NOT NULL |
| monto | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |
| descripcion | VARCHAR(200) | NOT NULL |

**Reglas:** S22 y S23 (con D4 y D7). Si el gasto tiene lote, el trigger exige que `fecha >= fecha_ingreso` y que el lote no esté `CERRADO`.

#### `jornada_beneficio`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_jornada | BIGINT | PK |
| id_lote | BIGINT | NOT NULL, FK → `lote` |
| fecha | DATE | NOT NULL |
| observaciones | VARCHAR(300) | NULL |

UNIQUE `(id_lote, fecha)`: una jornada por lote y día. **Regla:** decisión del equipo (cada jornada de beneficio se registra por lote y fecha).

#### `detalle_beneficio` (N:M jornada–empleado)
| Columna | Tipo | Restricciones |
|---|---|---|
| id_jornada | BIGINT | PK, FK → `jornada_beneficio` ON DELETE CASCADE |
| id_empleado | BIGINT | PK, FK → `empleado` |
| aves_beneficiadas | INTEGER | NOT NULL, CHECK `> 0` |
| tarifa_por_ave | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |

**Reglas:** trigger `trg_detalle_beneficio_validar`:
- `fn_validar_empleado(id_empleado, 'BENEFICIADORA')`: solo beneficiadoras activas.
- El lote de la jornada está en `LISTO_PARA_VENTA`.
- El total de aves beneficiadas del lote (todas las jornadas) no supera `cantidad_inicial - mortalidad`.

Costo del beneficio del lote = `SUM(aves_beneficiadas * tarifa_por_ave)` (`fn_costo_beneficio_lote`).
`sp_registrar_jornada_beneficio(lote, fecha, detalle JSONB)` crea la jornada y su detalle en una sola transacción.

### Módulo 7 — Comercial

#### `cliente`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_cliente | BIGINT | PK |
| id_tipo_cliente | BIGINT | NOT NULL, FK → `tipo_cliente` |
| documento | VARCHAR(20) | NOT NULL, UNIQUE (documento o NIT) |
| nombre | VARCHAR(120) | NOT NULL |
| direccion | VARCHAR(200) | NULL |
| telefono | VARCHAR(20) | NULL, UNIQUE |
| correo | VARCHAR(120) | NULL, UNIQUE |
| activo | BOOLEAN | NOT NULL DEFAULT TRUE |

CHECK `telefono IS NOT NULL OR correo IS NOT NULL`. **Reglas:** S28, S29 y S30.

#### `pedido`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_pedido | BIGINT | PK |
| id_cliente | BIGINT | NOT NULL, FK → `cliente` |
| fecha_pedido | DATE | NOT NULL DEFAULT CURRENT_DATE |
| fecha_estimada_entrega | DATE | NOT NULL, CHECK `>= fecha_pedido` |
| estado | estado_pedido | NOT NULL DEFAULT `'PENDIENTE'` |
| observaciones | VARCHAR(300) | NULL |

**Reglas:** S31 (la cantidad solicitada se deriva, D10). Transiciones, validadas por trigger:
- `PENDIENTE → CONFIRMADO | CANCELADO`
- `CONFIRMADO → CANCELADO`: solo si no tiene venta.
- `CONFIRMADO → ENTREGADO`: lo hace el trigger de `entrega`.

El cliente debe estar activo.

#### `detalle_pedido` (N:M pedido–lote)
| Columna | Tipo | Restricciones |
|---|---|---|
| id_pedido | BIGINT | PK, FK → `pedido` ON DELETE CASCADE |
| id_lote | BIGINT | PK, FK → `lote` |
| cantidad_aves | INTEGER | NOT NULL, CHECK `> 0` |

**Reglas:** S32 y decisión del equipo (N:M). Trigger `trg_detalle_pedido_validar`:
- El pedido está en `PENDIENTE`: solo se edita mientras está pendiente.
- El lote está en `LISTO_PARA_VENTA`.

`sp_confirmar_pedido` comprueba, con `SELECT … FOR UPDATE` sobre los lotes, que por cada lote
`cantidad_aves <= fn_aves_disponibles(lote) - fn_aves_comprometidas(lote)`. Las aves comprometidas
son las de pedidos `CONFIRMADO` que todavía no tienen venta.

#### `venta` (1:1 con pedido)
| Columna | Tipo | Restricciones |
|---|---|---|
| id_venta | BIGINT | PK |
| id_pedido | BIGINT | NOT NULL, UNIQUE, FK → `pedido` |
| fecha_venta | DATE | NOT NULL DEFAULT CURRENT_DATE |
| peso_total_kg | NUMERIC(10,3) | NOT NULL, CHECK `> 0` |
| precio_kilo | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |
| valor_total | NUMERIC(16,2) | GENERATED ALWAYS AS `(peso_total_kg * precio_kilo)` STORED |
| fecha_limite_pago | DATE | NOT NULL, CHECK `>= fecha_venta` |
| estado | estado_venta | NOT NULL DEFAULT `'PENDIENTE_PAGO'` |

**Reglas:**
- S33 y S34: el precio por kilo queda fijo y el valor total es generado (D11).
- D5: una sola venta por pedido.
- Trigger `trg_venta_validar` BEFORE INSERT: el pedido está `CONFIRMADO`, cada lote del detalle está `LISTO_PARA_VENTA` (S13) y tiene aves suficientes.
- Trigger `trg_venta_lotes` AFTER INSERT: cada lote cuyo `fn_aves_disponibles` llega a 0 pasa a `VENDIDO` (S12), y eso libera el galpón.
- Aves vendidas de un lote = `SUM(detalle_pedido.cantidad_aves)` de los pedidos que tienen venta.

#### `pago`
| Columna | Tipo | Restricciones |
|---|---|---|
| id_pago | BIGINT | PK |
| id_venta | BIGINT | NOT NULL, FK → `venta` |
| id_metodo_pago | BIGINT | NOT NULL, FK → `metodo_pago` |
| fecha | DATE | NOT NULL DEFAULT CURRENT_DATE |
| monto | NUMERIC(14,2) | NOT NULL, CHECK `> 0` |
| referencia_pasarela | VARCHAR(100) | NULL, UNIQUE (id de la transacción aprobada) |

**Reglas:**
- S35, S36 y S37. Trigger `trg_pago_validar`: `monto <= fn_saldo_venta(venta)`. Si el método es `EN_LINEA`, la referencia es obligatoria; si no lo es, debe ser NULL.
- Trigger AFTER INSERT: cuando el saldo llega a 0, la venta pasa a `PAGADA`.
- El pago en línea solo se inserta cuando la pasarela confirma la transacción como aprobada. La referencia UNIQUE impide registrar dos veces la misma transacción. Los datos de tarjeta nunca llegan a la base de datos.

#### `entrega` (1:1 con venta)
| Columna | Tipo | Restricciones |
|---|---|---|
| id_entrega | BIGINT | PK |
| id_venta | BIGINT | NOT NULL, UNIQUE, FK → `venta` |
| id_empleado | BIGINT | NOT NULL, FK → `empleado` |
| direccion | VARCHAR(200) | NOT NULL |
| fecha_entrega | DATE | NULL (fecha real) |
| estado | estado_entrega | NOT NULL DEFAULT `'PENDIENTE'` |
| observaciones | VARCHAR(300) | NULL |

CHECK `estado <> 'ENTREGADO' OR fecha_entrega IS NOT NULL`.
**Reglas:**
- S39 y S40.
- S26: `fn_validar_empleado(id_empleado, 'TRANSPORTISTA')`.
- S41: solo puede pasar a `ENTREGADO` si la venta existe y su pedido está `CONFIRMADO`. Al pasar a `ENTREGADO`, un trigger pone el pedido en `ENTREGADO`.

---

## 4. Relaciones (cardinalidades exigidas por el enunciado)

| Tipo | Relaciones |
|---|---|
| **1:1** | `pedido`–`venta`, `venta`–`entrega` |
| **1:N** | galpón–lote, lote–mortalidad, lote–consumo, lote–aplicación, lote–gasto, lote–jornada, proveedor–compra de alimento, proveedor–producto, producto–compra sanitaria, cliente–pedido, venta–pago, cargo–empleado, cada catálogo con sus tablas |
| **N:M** | `pedido`–`lote` (`detalle_pedido`), `jornada_beneficio`–`empleado` (`detalle_beneficio`), `rol`–`permiso`, `usuario`–`rol`, `usuario`–`permiso` |

Entidades del dominio: 22 tablas, sin contar las 6 de seguridad. El mínimo exigido es 8.

---

## 5. Funciones, procedimientos y vistas

**Funciones:**
- `fn_edad_lote(id_lote, fecha)`
- `fn_aves_disponibles(id_lote)`
- `fn_aves_comprometidas(id_lote)`
- `fn_mortalidad_pct(id_lote)`
- `fn_inventario_alimento(id_tipo)`
- `fn_costo_promedio_alimento(id_tipo, fecha)`
- `fn_costo_promedio_producto(id_producto, fecha)`
- `fn_costo_beneficio_lote(id_lote)`
- `fn_costo_lote(id_lote)`: devuelve una tabla con el desglose de pollitos, alimento, sanidad, beneficio y otros gastos.
- `fn_rentabilidad_lote(id_lote)`
- `fn_saldo_venta(id_venta)`
- `fn_cartera_vencida()`
- `fn_validar_empleado(id_empleado, codigo_cargo)`

**Procedimientos** (con `COMMIT`/`ROLLBACK` y bloques `EXCEPTION` que traducen los errores a mensajes en español con SQLSTATE `P0001`):
- `sp_registrar_lote`
- `sp_cambiar_estado_lote`
- `sp_registrar_mortalidad`
- `sp_registrar_compra_alimento`
- `sp_registrar_consumo_alimento`
- `sp_registrar_aplicacion_sanitaria`
- `sp_registrar_gasto`
- `sp_registrar_jornada_beneficio`
- `sp_retirar_empleado`
- `sp_crear_pedido`
- `sp_confirmar_pedido`
- `sp_cancelar_pedido`
- `sp_registrar_venta`
- `sp_registrar_pago`
- `sp_registrar_entrega`
- `sp_actualizar_entrega`
- `sp_marcar_cartera_vencida`: se ejecuta a diario desde un `@Scheduled` del backend, porque depende del paso del tiempo (`CLAUDE.md`).

**Vistas de reportes:**
- `v_costo_rentabilidad_lote`
- `v_ventas_por_cliente`
- `v_estado_cartera`
- `v_inventario_alimento`

---

## 6. Seguridad en la base de datos

| Rol | Permisos |
|---|---|
| `pollos_owner` | Dueño del esquema. Ejecuta los scripts DDL. |
| `pollos_app` | Usado por el backend. `SELECT/INSERT/UPDATE` sobre las tablas, `EXECUTE` sobre funciones y procedimientos. Sin `DELETE` en las tablas transaccionales y sin DDL. |
| `pollos_lectura` | Solo `SELECT`. Para consultas del equipo (por ejemplo, el acceso de Dylan). |

---

## 7. MongoDB (sin cambios de fondo)

La colección `seguimientos_crecimiento` se mantiene como está implementada: un documento por
lote y día con `codigoLote`, `fecha`, `edadDias`, peso promedio, temperatura, humedad,
observaciones, `muestreo` opcional y `novedades`. No guarda mortalidad (D2).
El backend reemplaza `VerificadorLotePendiente` por una implementación que consulta `lote` por
`codigo`, comprueba que esté activo (D12) y calcula la edad con `fecha_ingreso`.

---

## 8. Orden de implementación propuesto

Al final de cada módulo se hace despliegue (`desplegar.sh`). Cada módulo incluye su script SQL,
el backend con pruebas y su pantalla en el front.

1. **Galpones y lotes:** catálogos base, `galpon`, `lote`, `mortalidad` y el verificador real de MongoDB.
2. Personal: `cargo` y `empleado`.
3. Alimentación: proveedores, etapas, tipos, compras y consumos.
4. Sanidad.
5. Gastos y beneficio.
6. Clientes y pedidos.
7. Ventas, entregas y cartera.
8. Pagos con la pasarela *sandbox*. Hay que elegir la pasarela; la propuesta es Wompi, porque es colombiana y tiene *sandbox* gratuito.
9. Reportes de costo y rentabilidad.
