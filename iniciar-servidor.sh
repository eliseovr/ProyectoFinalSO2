#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
# shellcheck source=scripts/ubuntu/_comun.sh
source "$ROOT/scripts/ubuntu/_comun.sh"

liberar_puerto_9443

if [ ! -f "certificados/server.p12" ] || [ ! -f "certificados/truststore.p12" ]; then
    echo "Generando certificados TLS..."
    chmod +x certificados/generar-certificados.sh
    (cd certificados && ./generar-certificados.sh)
fi

export APP_BASE="$ROOT"
export KEYSTORE_PATH="$ROOT/certificados/server.p12"

echo "Compilando servidor..."
./gradlew :servidor:installDist --no-daemon -q

echo ""
echo "Abriendo ventanas del servidor..."
echo "  1) Monitor  - Centro de control del servidor"
echo "  2) Servidor - consola del proceso (puerto 9443)"
echo ""
echo "Administrador BD: ./ver-admin-base-datos.sh (o boton en el Monitor)"
echo "Cliente (otra VM): ./iniciar-cliente.sh con IP de esta maquina"
echo ""

MON_CMD="cd '$ROOT' && ./scripts/ubuntu/run-monitor.sh"
SRV_CMD="cd '$ROOT' && ./scripts/ubuntu/run-servidor.sh"

abrir_terminal "Mensajeria SO2 - Monitor" "$MON_CMD" || true
sleep 1
abrir_terminal "Mensajeria SO2 - Servidor" "$SRV_CMD" || true

echo "Si no se abrieron ventanas nuevas, use dos terminales:"
echo "  ./scripts/ubuntu/run-monitor.sh"
echo "  ./scripts/ubuntu/run-servidor.sh"
