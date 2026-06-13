#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
export APP_BASE="$ROOT"
./gradlew :servidor:installDist --no-daemon -q
java -cp "servidor/build/install/servidor/lib/*" servidor.vista.AplicacionMonitorServidor
