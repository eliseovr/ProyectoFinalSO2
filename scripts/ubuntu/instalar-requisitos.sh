#!/usr/bin/env bash
# Instala Java y dependencias minimas para Swing en Ubuntu (VM).
set -euo pipefail

echo "=== Requisitos Mensajeria SO2 (Ubuntu) ==="
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk-headless openjdk-17-jdk \
    git curl ca-certificates \
    libxext6 libxrender1 libxtst6 libxi6

if ! command -v keytool >/dev/null 2>&1; then
    echo "ERROR: keytool no disponible. Revise la instalacion de JDK."
    exit 1
fi

java -version
echo ""
echo "Listo. Copie el proyecto a esta VM y ejecute:"
echo "  chmod +x gradlew scripts/ubuntu/*.sh *.sh"
echo "  ./scripts/ubuntu/instalar-requisitos.sh   (ya ejecutado)"
echo "  ./iniciar-servidor.sh   (en VM servidor)"
echo "  ./iniciar-cliente.sh    (en VM cliente)"
