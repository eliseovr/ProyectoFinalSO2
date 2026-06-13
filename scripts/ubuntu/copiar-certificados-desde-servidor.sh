#!/usr/bin/env bash
# Copia certificados/ desde la VM servidor (ejecutar en la VM cliente).
set -euo pipefail

if [ -z "${1:-}" ]; then
    echo "Uso: $0 IP_SERVIDOR [usuario_ssh]"
    echo "Ejemplo: $0 192.168.56.10 ubuntu"
    exit 1
fi

IP="$1"
USER="${2:-$USER}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
REMOTE="${USER}@${IP}:ProyectoFinalSO2/certificados/"

mkdir -p "$ROOT/certificados"
echo "Copiando certificados desde ${USER}@${IP} ..."
scp -r "${USER}@${IP}:ProyectoFinalSO2/certificados/." "$ROOT/certificados/"
echo "Listo. Ahora ejecute: cd '$ROOT' && ./iniciar-cliente.sh"
