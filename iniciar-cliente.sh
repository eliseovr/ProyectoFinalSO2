#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [ ! -f "certificados/truststore.p12" ] || [ ! -f "certificados/server.p12" ]; then
    echo ""
    echo "AVISO: Faltan certificados en certificados/"
    echo "En la VM cliente copie la carpeta certificados/ desde la VM servidor"
    echo "(despues de que el servidor haya arrancado al menos una vez)."
    echo ""
    echo "  scp -r usuario@IP_SERVIDOR:~/ProyectoFinalSO2/certificados ./certificados"
    echo ""
    read -r -p "Continuar de todos modos? [s/N] " r
    case "$r" in
        s|S|si|Si|SI) ;;
        *) exit 1 ;;
    esac
fi

export APP_BASE="$ROOT"
export KEYSTORE_PATH="$ROOT/certificados/server.p12"

echo "Iniciando cliente grafico..."
echo "En login use la IP de la VM servidor (no localhost salvo que el servidor este en esta misma VM)."
./gradlew :comun:compileJava :cliente:compileJava :cliente:run --no-daemon
