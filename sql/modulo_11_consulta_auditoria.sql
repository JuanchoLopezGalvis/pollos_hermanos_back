BEGIN;

SET ROLE pollos_owner;

INSERT INTO permiso (codigo, descripcion) VALUES
    ('auditoria.ver', 'Consultar la auditoría: quién creó, modificó o borró cada registro')
ON CONFLICT (codigo) DO NOTHING;

RESET ROLE;

COMMIT;
