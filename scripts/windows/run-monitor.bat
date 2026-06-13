@echo off
cd /d "%~dp0..\.."
set "APP_BASE=%CD%"
call gradlew.bat :servidor:installDist --no-daemon -q
if errorlevel 1 (
    echo ERROR: No se pudo compilar el monitor.
    pause
    exit /b 1
)
java -cp "servidor\build\install\servidor\lib\*" servidor.vista.AplicacionMonitorServidor
