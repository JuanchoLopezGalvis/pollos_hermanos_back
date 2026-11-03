
BEGIN;

SET ROLE pollos_owner;

REVOKE ALL ON galpon, lote, mortalidad, v_lote_resumen FROM pollos_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON galpon TO pollos_app;
GRANT SELECT, INSERT, UPDATE ON lote, mortalidad TO pollos_app;
GRANT SELECT ON v_lote_resumen TO pollos_app;

RESET ROLE;

COMMIT;
