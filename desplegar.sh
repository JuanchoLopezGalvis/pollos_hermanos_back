#!/bin/bash
# Recompila el backend y reinicia el servicio con la versión nueva.
set -e
cd "$(dirname "$0")"
git pull || echo "Sin git pull (el proyecto no está en un repositorio)"
./mvnw clean package -DskipTests
sudo cp target/pollos_hermanos_back-0.0.1-SNAPSHOT.jar /opt/pollos-hermanos/app.jar
sudo chown pollos:pollos /opt/pollos-hermanos/app.jar
sudo systemctl restart pollos-hermanos
echo "Backend actualizado. Ver logs: journalctl -u pollos-hermanos -f"
