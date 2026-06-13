@echo off
cd /d "%~dp0"

echo Limpiando historial de Git...
echo Un solo commit, autor: Eliseo Velasquez (sin Cursor)
echo.

set GIT_AUTHOR_NAME=Eliseo Velasquez
set GIT_COMMITTER_NAME=Eliseo Velasquez
set GIT_AUTHOR_EMAIL=eliseo_vr@hotmail.com
set GIT_COMMITTER_EMAIL=eliseo_vr@hotmail.com

git checkout main 2>nul
git branch -D main-limpio 2>nul
git checkout --orphan main-limpio

git add -A
git reset HEAD limpiar-historial-git.bat subir-github.bat 2>nul

for /f %%i in ('git write-tree') do set TREE=%%i
for /f %%i in ('git commit-tree -m "Proyecto Final SO2: mensajeria segura distribuida." %TREE%') do set COMMIT=%%i
git reset --hard %COMMIT%

git branch -D main 2>nul
git branch -m main

git remote set-url origin https://github.com/eliseovr/ProyectoFinalSO2.git
echo.
echo Subiendo a GitHub (force push)...
git push -u origin main --force

echo.
echo === Verificacion ===
git log -1 --format="Autor: %%an ^<%%ae^>"
git log -1 --format="Mensaje: %%s"
echo.
echo Repo: https://github.com/eliseovr/ProyectoFinalSO2
pause
