-- =====================================================================
-- CORRECCIÓN DEL MÓDULO 1: privilegios de la aplicación.
--
-- La base tiene ALTER DEFAULT PRIVILEGES que da SELECT, INSERT, UPDATE y
-- DELETE a pollos_app sobre toda tabla nueva de pollos_owner. Por eso, al
-- ejecutar modulo_01_galpones_lotes.sql antes de esta corrección, la
-- aplicación quedó con permiso para borrar lotes y mortalidad.
-- Solo hace falta en bases donde el módulo 1 ya se instaló sin el REVOKE.
--   ./instalar_bd.sh correccion_01_privilegios.sql
-- =====================================================================

BEGIN;

SET ROLE pollos_owner;

REVOKE ALL ON galpon, lote, mortalidad, v_lote_resumen FROM pollos_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON galpon TO pollos_app;
GRANT SELECT, INSERT, UPDATE ON lote, mortalidad TO pollos_app;
GRANT SELECT ON v_lote_resumen TO pollos_app;

RESET ROLE;

COMMIT;
