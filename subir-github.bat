@echo off
cd /d "%~dp0"
echo Subiendo a https://github.com/eliseovr/ProyectoFinalSO2 ...
git remote remove origin 2>nul
git remote add origin https://github.com/eliseovr/ProyectoFinalSO2.git
git push -u origin main
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Listo: https://github.com/eliseovr/ProyectoFinalSO2
) else (
    echo.
    echo Error. Inicia sesion en GitHub e intenta de nuevo.
    pause
)
