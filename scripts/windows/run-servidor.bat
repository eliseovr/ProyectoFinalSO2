@echo off
cd /d "%~dp0..\.."
set "APP_BASE=%CD%"
set "KEYSTORE_PATH=%CD%\certificados\server.p12"
echo.
echo === SERVIDOR DE MENSAJERIA ===
echo Puerto: 9443 (TLS)
echo Eventos en vivo abajo. Cierre con Ctrl+C o cerrando esta ventana.
echo.
call servidor\build\install\servidor\bin\servidor.bat
