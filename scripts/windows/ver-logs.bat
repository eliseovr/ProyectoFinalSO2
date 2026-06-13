@echo off
cd /d "%~dp0..\.."
if not exist "registros\sistema.log" (
    echo Aun no hay logs. Inicie primero el servidor con iniciar-servidor.bat
    pause
    exit /b 1
)

start "Mensajeria SO2 - Logs del servidor" powershell -NoExit -Command "Write-Host '=== REGISTRO DE ACTIVIDAD (registros\sistema.log) ===' -ForegroundColor Cyan; Write-Host 'Conexiones, logins, mensajes enviados/recibidos...' -ForegroundColor Gray; Write-Host ''; Get-Content -Path 'registros\sistema.log' -Wait -Tail 30"
