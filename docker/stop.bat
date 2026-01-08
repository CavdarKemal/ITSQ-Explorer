@echo off
REM Stop StandardMDIGUI Docker containers
cd /d "%~dp0.."
docker-compose down
echo.
echo Containers stopped.
pause
