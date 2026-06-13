#!/bin/sh
# Genera certificados TLS (Linux)
set -e
cd "$(dirname "$0")"
PASSWORD=changeit
VALIDITY=3650

keytool -genkeypair -alias mensajeria -keyalg RSA -keysize 2048 \
  -validity "$VALIDITY" -storetype PKCS12 -keystore server.p12 \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=MensajeriaSO2, OU=SO2, O=UAM, L=Ciudad, ST=Estado, C=MX"

keytool -exportcert -alias mensajeria -keystore server.p12 -storepass "$PASSWORD" \
  -file server.cer

keytool -importcert -alias mensajeria -file server.cer -keystore truststore.p12 \
  -storetype PKCS12 -storepass "$PASSWORD" -noprompt

rm -f server.cer
echo "Certificados generados en $(pwd)"
