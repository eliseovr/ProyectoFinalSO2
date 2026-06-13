@echo off
cd /d "%~dp0..\.."
echo === Reinicio completo de Mensajeria SO2 ===

echo Cerrando procesos en puerto 9443...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":9443" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%p >nul 2>&1
)

echo Regenerando certificados TLS...
powershell -ExecutionPolicy Bypass -File certificados\generar-certificados.ps1

echo.
echo Listo. Ahora:
echo   1. Terminal A: iniciar-servidor.bat
echo   2. Cuando vea SERVIDOR iniciado, Terminal B: iniciar-cliente.bat
echo.
