# Genera certificados TLS autofirmados para servidor y clientes.
# Requiere Java keytool (incluido en el JDK).

$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$password = "changeit"
$validity = 3650

Set-Location $dir

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    Write-Error "keytool no encontrado. Instale JDK 17+ y agregue keytool al PATH."
}

Remove-Item server.p12, truststore.p12, active-cert.sha256 -ErrorAction SilentlyContinue

Write-Host "Generando server.p12 ..."
keytool -genkeypair -alias mensajeria -keyalg RSA -keysize 2048 `
    -validity $validity -storetype PKCS12 -keystore server.p12 `
    -storepass $password -keypass $password `
    -dname "CN=MensajeriaSO2, OU=SO2, O=UAM, L=Ciudad, ST=Estado, C=MX"

Write-Host "Exportando certificado público ..."
keytool -exportcert -alias mensajeria -keystore server.p12 -storepass $password `
    -file server.cer

Write-Host "Creando truststore.p12 para clientes ..."
keytool -importcert -alias mensajeria -file server.cer -keystore truststore.p12 `
    -storetype PKCS12 -storepass $password -trustcacerts -noprompt

Remove-Item server.cer -ErrorAction SilentlyContinue
Write-Host "Listo: server.p12 y truststore.p12 en $dir"
