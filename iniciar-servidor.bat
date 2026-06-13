@echo off
cd /d "%~dp0"

echo Liberando puerto 9443 si estaba en uso...
:kill_port
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":9443" ^| findstr "LISTENING"') do (
    echo Cerrando proceso PID %%p en puerto 9443...
    taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":9443" ^| findstr "LISTENING"') do (
    goto kill_port
)

if not exist "certificados\server.p12" (
    echo Generando certificados...
    powershell -ExecutionPolicy Bypass -File certificados\generar-certificados.ps1
) else if not exist "certificados\truststore.p12" (
    echo Regenerando certificados ^(falta truststore^)...
    powershell -ExecutionPolicy Bypass -File certificados\generar-certificados.ps1
)

echo Compilando servidor...
set APP_BASE=%CD%
set KEYSTORE_PATH=%CD%\certificados\server.p12
call gradlew.bat :servidor:installDist --no-daemon -q
if errorlevel 1 (
    echo.
    echo ERROR: No se pudo compilar el servidor.
    pause
    exit /b 1
)

echo.
echo Abriendo ventana del servidor...
echo.
echo  - Ventana "Monitor": panel grafico con eventos en vivo
echo  - Ventana "Servidor": consola del proceso
echo  - Administrador BD: ver-admin-base-datos.bat (o boton en el Monitor)
echo  - Cliente grafico: iniciar-cliente.bat
echo.

start "Mensajeria SO2 - Monitor" cmd /k call "%~dp0scripts\windows\run-monitor.bat"
timeout /t 1 /nobreak >nul
start "Mensajeria SO2 - Servidor" cmd /k call "%~dp0scripts\windows\run-servidor.bat"

echo Servidor y monitor abiertos en ventanas separadas.
timeout /t 3 /nobreak >nul
