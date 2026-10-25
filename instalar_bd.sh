#!/bin/bash
# Ejecuta uno o varios scripts SQL de la carpeta sql/ como superusuario de PostgreSQL, en orden.
# Uso: ./instalar_bd.sh modulo_01_galpones_lotes.sql
#      ./instalar_bd.sh modulo_0[2-7]*      (instala los módulos 2 a 7)
# Cada archivo lo lee este usuario y se entrega por la entrada estándar, porque
# el usuario postgres no tiene permiso para leer la carpeta personal.
set -e
cd "$(dirname "$0")"
[ $# -gt 0 ] || { echo "Indica el script, por ejemplo modulo_01_galpones_lotes.sql"; exit 1; }
archivos=()
for patron in "$@"; do
	for f in sql/$patron; do
		[ -f "$f" ] || { echo "No existe sql/$patron"; exit 1; }
		archivos+=("$f")
	done
done
for f in "${archivos[@]}"; do
	echo "== Instalando $f"
	sudo -u postgres psql -d pollos_hermanos -v ON_ERROR_STOP=1 -q < "$f"
	echo "   listo"
done
