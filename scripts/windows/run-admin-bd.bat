@echo off
cd /d "%~dp0..\.."
set "APP_BASE=%CD%"
echo Compilando administrador de base de datos...
call gradlew.bat :servidor:installDist --no-daemon -q
if errorlevel 1 (
    echo.
    echo ERROR: No se pudo compilar. Revise que Java este instalado.
    pause
    exit /b 1
)
java -cp "servidor\build\install\servidor\lib\*" servidor.vista.AplicacionAdminBaseDatos
if errorlevel 1 (
    echo.
    echo ERROR al iniciar el administrador.
    pause
    exit /b 1
)
