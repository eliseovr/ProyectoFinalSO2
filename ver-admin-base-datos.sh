#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
# shellcheck source=scripts/ubuntu/_comun.sh
source "$ROOT/scripts/ubuntu/_comun.sh"

export APP_BASE="$ROOT"
abrir_terminal "Admin BD - Mensajeria SO2" "cd '$ROOT' && ./scripts/ubuntu/run-admin-bd.sh" \
    || ./scripts/ubuntu/run-admin-bd.sh
