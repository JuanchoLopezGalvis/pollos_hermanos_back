#!/bin/bash
# Guarda las llaves del ambiente de PRUEBAS de Wompi en la configuración del servicio.
# Las pide por teclado para que no queden en el código ni en el historial de git.
set -e
ARCHIVO=/etc/pollos-hermanos/app.env
read -rp "Llave pública (pub_test_...): " LLAVE
read -rp "Secreto de integridad (test_integrity_...): " SECRETO
[[ $LLAVE == pub_test_* ]] || { echo "La llave pública debe empezar por pub_test_ (solo ambiente de pruebas)."; exit 1; }
[[ $SECRETO == test_integrity_* ]] || { echo "El secreto debe empezar por test_integrity_."; exit 1; }
sudo sed -i '/^WOMPI_LLAVE_PUBLICA=/d;/^WOMPI_SECRETO_INTEGRIDAD=/d' "$ARCHIVO"
printf 'WOMPI_LLAVE_PUBLICA=%s\nWOMPI_SECRETO_INTEGRIDAD=%s\n' "$LLAVE" "$SECRETO" | sudo tee -a "$ARCHIVO" >/dev/null
echo "Llaves guardadas en $ARCHIVO. Se aplican al reiniciar el backend (./desplegar.sh lo reinicia)."
