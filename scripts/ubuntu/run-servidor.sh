#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
export APP_BASE="$ROOT"
export KEYSTORE_PATH="$ROOT/certificados/server.p12"
echo ""
echo "=== SERVIDOR DE MENSAJERIA ==="
echo "Puerto: 9443 (TLS)"
echo "Cierre con Ctrl+C o cerrando esta ventana."
echo ""
./gradlew :servidor:run --no-daemon
