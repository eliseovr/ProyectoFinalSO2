@echo off
cd /d "%~dp0"
set "APP_BASE=%CD%"
title Mensajeria SO2 - Administrador BD
start "Admin BD - Mensajeria SO2" cmd /k call "%~dp0scripts\windows\run-admin-bd.bat"
echo Ventana de administracion de base de datos iniciada.
exit /b 0
