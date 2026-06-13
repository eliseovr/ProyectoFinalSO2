@echo off
cd /d "%~dp0"
if not exist "certificados\truststore.p12" (
    echo Generando certificados...
    powershell -ExecutionPolicy Bypass -File certificados\generar-certificados.ps1
) else if not exist "certificados\server.p12" (
    echo Regenerando certificados ^(falta server.p12^)...
    powershell -ExecutionPolicy Bypass -File certificados\generar-certificados.ps1
)
echo Iniciando cliente grafico...
set APP_BASE=%CD%
set KEYSTORE_PATH=%CD%\certificados\server.p12
call gradlew.bat :comun:compileJava :cliente:compileJava :cliente:run --no-daemon
