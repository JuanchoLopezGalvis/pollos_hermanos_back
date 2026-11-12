-- =====================================================================
-- Documento SQL de reportes
-- Proyecto: Sistema de Gestión de Producción y Venta de Pollos de Engorde
-- Docente: Ing., Esp., M.Ed. Christian Felipe Duarte
-- Equipo: Gabriela Poveda, Dylan Mancipe y Juan Diego López
-- Versión 1.0 
-- ---------------------------------------------------------------------
-- RDBMS: PostgreSQL 18. Base: pollos_hermanos.
-- Objetos usados: las vistas (v_), funciones (fn_) y tablas creadas por los
-- scripts sql/modulo_00 a modulo_11 del repositorio pollos_hermanos_back.
--
-- Ejecución (solo lectura; no modifica datos):
--   psql -h <servidor> -U pollos_app -d pollos_hermanos -f 07_SQL_Reportes.sql
-- El rol pollos_app tiene SELECT sobre todos los objetos usados aquí.
--
-- Índice de reportes
--   R01  Costo y rentabilidad por lote (RF-20)
--   R02  Participación de cada componente en el costo del lote (RF-20)
--   R03  Ventas por cliente con ranking y participación (RF-28)
--   R04  Cartera vencida por rangos de antigüedad (RF-13)
--   R05  Estado de cuenta por venta con detalle de pagos (RF-12, RF-13)
--   R06  Mortalidad por lote y alerta del 5 % (RF-16)
--   R07  Mortalidad diaria acumulada de los lotes activos (RF-16)
--   R08  Inventario y costo promedio de alimento por etapa (RF-05, RF-06, RF-17)
--   R09  Consumo de alimento por lote y etapa (RF-06, RF-17)
--   R10  Ocupación de galpones (RF-03, RF-22)
--   R11  Aplicaciones sanitarias por lote y responsable (RF-07)
--   R12  Gastos por categoría y mes (RF-08)
--   R13  Costo del beneficio por jornada y beneficiadora (RF-19)
--   R14  Pedidos por estado y aves comprometidas (RF-10)
--   R15  Entregas pendientes por transportista (RF-14)
--   R16  Recaudo por método de pago y mes (RF-12, RF-24)
--   R17  Auditoría: cambios por usuario y tabla en los últimos 30 días (RF-26)
-- =====================================================================

\echo '== R01 Costo y rentabilidad por lote =='
-- Costo total con sus cinco componentes frente a los ingresos del lote.
-- Las ventas se reparten entre los lotes de un pedido en proporción a sus aves
-- (fn_ingreso_lote). Ordena de mayor a menor ganancia.
SELECT codigo                AS lote,
       estado,
       costo_total,
       aves_vendidas,
       libras_vendidas,
       ingreso_total,
       ganancia,
       costo_por_ave_vendida,
       costo_por_libra,
       precio_promedio_libra,
       margen_porcentaje
FROM v_rentabilidad_lote
ORDER BY ganancia DESC NULLS LAST;

\echo '== R02 Participación de cada componente en el costo del lote =='
-- Muestra qué porcentaje del costo corresponde a pollitos, alimento, sanidad,
-- beneficio y otros gastos. NULLIF evita la división por cero en lotes sin costo.
SELECT codigo AS lote,
       costo_total,
       ROUND(costo_pollitos  * 100 / NULLIF(costo_total, 0), 1) AS pct_pollitos,
       ROUND(costo_alimento  * 100 / NULLIF(costo_total, 0), 1) AS pct_alimento,
       ROUND(costo_sanidad   * 100 / NULLIF(costo_total, 0), 1) AS pct_sanidad,
       ROUND(costo_beneficio * 100 / NULLIF(costo_total, 0), 1) AS pct_beneficio,
       ROUND(otros_gastos    * 100 / NULLIF(costo_total, 0), 1) AS pct_otros
FROM v_costo_lote
ORDER BY costo_total DESC;

\echo '== R03 Ventas por cliente con ranking y participación =='
-- Usa funciones de ventana: RANK para el puesto del cliente y SUM() OVER ()
-- para su participación en el total vendido.
SELECT RANK() OVER (ORDER BY valor_vendido DESC)                         AS puesto,
       cliente,
       tipo_cliente,
       ventas,
       valor_vendido,
       ROUND(valor_vendido * 100 / NULLIF(SUM(valor_vendido) OVER (), 0), 2) AS pct_del_total,
       saldo_pendiente
FROM v_ventas_por_cliente
WHERE ventas > 0
ORDER BY puesto, cliente;

\echo '== R04 Cartera vencida por rangos de antigüedad =='
-- fn_cartera_vencida devuelve las ventas con saldo cuya fecha límite ya pasó.
-- Se agrupa por cliente en rangos de días de mora.
SELECT cliente,
       COUNT(*)                                                        AS ventas_vencidas,
       SUM(saldo) FILTER (WHERE dias_vencida BETWEEN 1 AND 30)          AS saldo_1_a_30_dias,
       SUM(saldo) FILTER (WHERE dias_vencida BETWEEN 31 AND 60)         AS saldo_31_a_60_dias,
       SUM(saldo) FILTER (WHERE dias_vencida > 60)                      AS saldo_mas_de_60_dias,
       SUM(saldo)                                                      AS saldo_total,
       MAX(dias_vencida)                                               AS max_dias_mora
FROM fn_cartera_vencida(CURRENT_DATE)
GROUP BY cliente
ORDER BY saldo_total DESC;

\echo '== R05 Estado de cuenta por venta con detalle de pagos =='
-- Una fila por venta con lo pagado, el saldo y la lista de pagos (fecha, método y monto).
SELECT v.id_venta,
       v.cliente,
       v.fecha_venta,
       v.valor_total,
       v.pagado,
       v.saldo,
       v.fecha_limite_pago,
       v.estado,
       v.vencida,
       COALESCE(v.estado_entrega, 'SIN ENTREGA')                          AS estado_entrega,
       (SELECT string_agg(to_char(pg.fecha, 'DD/MM/YYYY') || ' ' || pg.metodo || ' $' || pg.monto, '; '
                          ORDER BY pg.fecha)
          FROM v_pago pg
         WHERE pg.id_venta = v.id_venta)                                  AS pagos
FROM v_venta v
ORDER BY v.saldo DESC, v.fecha_venta;

\echo '== R06 Mortalidad por lote y alerta del 5 % =='
-- La alerta se activa cuando la mortalidad acumulada supera la referencia
-- (fn_mortalidad_referencia_pct = 5 %).
SELECT codigo                 AS lote,
       galpon,
       estado,
       edad_dias,
       cantidad_inicial,
       mortalidad_acumulada,
       porcentaje_mortalidad,
       fn_mortalidad_referencia_pct()  AS referencia_pct,
       CASE WHEN alerta_mortalidad THEN 'ALERTA' ELSE 'Normal' END AS alerta,
       aves_vendidas,
       aves_disponibles
FROM v_lote_resumen
ORDER BY porcentaje_mortalidad DESC;

\echo '== R07 Mortalidad diaria acumulada de los lotes activos =='
-- Suma acumulada por fecha con una función de ventana, para ver en qué día
-- cada lote cruzó el 5 %.
SELECT l.codigo                                                           AS lote,
       m.fecha,
       m.fecha - l.fecha_ingreso + 1                                      AS edad_dias,
       m.cantidad                                                         AS muertes_dia,
       m.causa,
       SUM(m.cantidad) OVER w                                             AS muertes_acumuladas,
       ROUND(SUM(m.cantidad) OVER w * 100.0 / l.cantidad_inicial, 2)      AS pct_acumulado
FROM mortalidad m
JOIN lote l ON l.id_lote = m.id_lote
WHERE l.estado IN ('EN_ENGORDE', 'LISTO_PARA_VENTA')
WINDOW w AS (PARTITION BY m.id_lote ORDER BY m.fecha)
ORDER BY l.codigo, m.fecha;

\echo '== R08 Inventario y costo promedio de alimento por etapa =='
-- Bultos comprados, consumidos y disponibles, y su valor al costo promedio ponderado.
SELECT etapa,
       dia_inicio,
       COALESCE(dia_fin::text, 'venta')                     AS dia_fin,
       nombre                                               AS tipo_alimento,
       bultos_comprados,
       bultos_consumidos,
       bultos_disponibles,
       costo_promedio,
       ROUND(bultos_disponibles * costo_promedio, 2)        AS valor_inventario
FROM v_inventario_alimento
WHERE activo
ORDER BY dia_inicio, nombre;

\echo '== R09 Consumo de alimento por lote y etapa =='
-- Bultos, kilogramos y costo consumidos por cada lote en cada etapa, más el
-- consumo por ave inicial.
SELECT l.codigo                                                        AS lote,
       e.orden,
       e.nombre                                                        AS etapa,
       SUM(c.cantidad_bultos)                                          AS bultos,
       SUM(c.cantidad_bultos * t.peso_bulto_kg)                        AS kilogramos,
       SUM(ROUND(c.cantidad_bultos * c.costo_unitario, 2))             AS costo,
       ROUND(SUM(c.cantidad_bultos * t.peso_bulto_kg) / l.cantidad_inicial, 3) AS kg_por_ave_inicial
FROM consumo_alimento c
JOIN lote l               ON l.id_lote = c.id_lote
JOIN tipo_alimento t      ON t.id_tipo_alimento = c.id_tipo_alimento
JOIN etapa_alimentacion e ON e.id_etapa = t.id_etapa
GROUP BY l.codigo, l.cantidad_inicial, e.orden, e.nombre
ORDER BY l.codigo, e.orden;

\echo '== R10 Ocupación de galpones =='
-- Producción escalonada: varios lotes activos por galpón sin superar su capacidad.
SELECT nombre                                                       AS galpon,
       estado,
       capacidad_maxima,
       lotes_activos,
       aves_alojadas,
       capacidad_libre,
       ROUND(aves_alojadas * 100.0 / capacidad_maxima, 1)           AS pct_ocupacion,
       proxima_salida
FROM v_galpon_ocupacion
ORDER BY pct_ocupacion DESC;

\echo '== R11 Aplicaciones sanitarias por lote y responsable =='
-- Número de aplicaciones, productos distintos y costo cargado a cada lote por
-- cada encargado de sanidad.
SELECT codigo_lote                       AS lote,
       responsable,
       COUNT(*)                          AS aplicaciones,
       COUNT(DISTINCT id_producto)       AS productos_distintos,
       MIN(fecha)                        AS primera_aplicacion,
       MAX(fecha)                        AS ultima_aplicacion,
       SUM(costo_total)                  AS costo_total
FROM v_aplicacion_sanitaria
GROUP BY codigo_lote, responsable
ORDER BY codigo_lote, costo_total DESC;

\echo '== R12 Gastos por categoría y mes =='
-- Separa los gastos asignados a un lote de los generales de la finca.
-- ROLLUP agrega subtotales por mes y un total general.
SELECT COALESCE(to_char(date_trunc('month', fecha), 'YYYY-MM'), 'TOTAL')   AS mes,
       COALESCE(categoria, 'Subtotal')                                     AS categoria,
       SUM(monto) FILTER (WHERE id_lote IS NOT NULL)                       AS gastos_de_lotes,
       SUM(monto) FILTER (WHERE id_lote IS NULL)                           AS gastos_generales,
       SUM(monto)                                                          AS total
FROM v_gasto
GROUP BY ROLLUP (date_trunc('month', fecha), categoria)
ORDER BY date_trunc('month', fecha) NULLS LAST, categoria NULLS LAST;

\echo '== R13 Costo del beneficio por jornada y beneficiadora =='
-- Detalle de cada jornada: quién benefició, cuántas aves, a qué tarifa y su pago.
SELECT j.codigo_lote             AS lote,
       j.fecha,
       d.beneficiadora,
       d.aves_beneficiadas,
       d.tarifa_por_ave,
       d.pago,
       j.aves_beneficiadas       AS aves_jornada,
       j.costo_total             AS costo_jornada
FROM v_jornada_beneficio j
JOIN v_detalle_beneficio d ON d.id_jornada = j.id_jornada
ORDER BY j.fecha, j.codigo_lote, d.beneficiadora;

\echo '== R14 Pedidos por estado y aves comprometidas =='
-- Cuántos pedidos hay en cada estado, cuántas aves suman y cuántos ya tienen venta.
SELECT estado,
       COUNT(*)                          AS pedidos,
       SUM(total_aves)                   AS aves,
       COUNT(id_venta)                   AS con_venta,
       MIN(fecha_estimada_entrega)       AS entrega_mas_proxima
FROM v_pedido
GROUP BY estado
ORDER BY CASE estado WHEN 'PENDIENTE' THEN 1 WHEN 'CONFIRMADO' THEN 2
                     WHEN 'ENTREGADO' THEN 3 ELSE 4 END;

\echo '== R15 Entregas pendientes por transportista =='
SELECT transportista,
       COUNT(*) FILTER (WHERE estado = 'PENDIENTE')  AS pendientes,
       COUNT(*) FILTER (WHERE estado = 'ENTREGADO')  AS entregadas,
       COUNT(*) FILTER (WHERE estado = 'DEVUELTO')   AS devueltas,
       string_agg(cliente || ' (venta ' || id_venta || ')', '; ' ORDER BY id_venta)
           FILTER (WHERE estado = 'PENDIENTE')       AS detalle_pendientes
FROM v_entrega
GROUP BY transportista
ORDER BY pendientes DESC;

\echo '== R16 Recaudo por método de pago y mes =='
SELECT to_char(date_trunc('month', fecha), 'YYYY-MM')  AS mes,
       metodo,
       COUNT(*)                                        AS pagos,
       SUM(monto)                                      AS recaudo
FROM v_pago
GROUP BY date_trunc('month', fecha), metodo
ORDER BY mes, recaudo DESC;

\echo '== R17 Auditoría: cambios por usuario y tabla en los últimos 30 días =='
-- usuario_app es el usuario autenticado en la aplicación; NULL significa que el
-- cambio lo hizo el sistema (aviso de Wompi, tarea programada, registro público o script).
SELECT COALESCE(usuario_app, '(sistema)')                         AS usuario,
       tabla,
       COUNT(*) FILTER (WHERE operacion = 'INSERT')               AS inserciones,
       COUNT(*) FILTER (WHERE operacion = 'UPDATE')               AS modificaciones,
       COUNT(*) FILTER (WHERE operacion = 'DELETE')               AS borrados,
       MAX(fecha_creacion)                                        AS ultimo_cambio
FROM auditoria
WHERE fecha_creacion >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY COALESCE(usuario_app, '(sistema)'), tabla
ORDER BY usuario, tabla;

-- Fin del documento SQL de reportes.
