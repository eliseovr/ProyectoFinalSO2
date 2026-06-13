@echo off
cd /d "%~dp0..\.."
set "APP_BASE=%CD%"
call gradlew.bat :servidor:installDist :servidor:ejecutarMonitor --no-daemon
