#!/usr/bin/env bash
# Funciones compartidas por los scripts de Ubuntu.

proyecto_raiz() {
    cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd
}

exportar_entorno() {
    export APP_BASE="$1"
    export KEYSTORE_PATH="$APP_BASE/certificados/server.p12"
}

liberar_puerto_9443() {
    if command -v fuser >/dev/null 2>&1; then
        fuser -k 9443/tcp 2>/dev/null || true
        sleep 1
    elif command -v lsof >/dev/null 2>&1; then
        local pid
        pid=$(lsof -t -i:9443 2>/dev/null || true)
        if [ -n "$pid" ]; then
            kill -9 $pid 2>/dev/null || true
            sleep 1
        fi
    fi
}

abrir_terminal() {
    local titulo="$1"
    local comando="$2"
    if command -v gnome-terminal >/dev/null 2>&1; then
        gnome-terminal --title="$titulo" -- bash -lc "$comando; echo; echo Pulsa Enter para cerrar.; read -r"
    elif command -v xfce4-terminal >/dev/null 2>&1; then
        xfce4-terminal --title="$titulo" -e "bash -lc '$comando; echo; echo Pulsa Enter para cerrar.; read -r'"
    elif command -v konsole >/dev/null 2>&1; then
        konsole --new-tab -p tabtitle="$titulo" -e bash -lc "$comando; echo; echo Pulsa Enter para cerrar.; read -r"
    elif command -v xterm >/dev/null 2>&1; then
        xterm -title "$titulo" -e bash -lc "$comando; echo; echo Pulsa Enter para cerrar.; read -r" &
    else
        echo "No se encontro emulador de terminal grafico."
        echo "Abra otra ventana de terminal y ejecute:"
        echo "  $comando"
        return 1
    fi
}
