@echo off
REM Alternativa sin ventana cmd extra: ejecuta directamente con Gradle
cd /d "%~dp0..\.."
set "APP_BASE=%CD%"
call gradlew.bat :servidor:ejecutarAdminBd --no-daemon
if errorlevel 1 pause
