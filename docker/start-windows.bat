@echo off
REM StandardMDIGUI Docker Starter for Windows
REM ==========================================
REM
REM Usage: start-windows.bat [config-file]
REM
REM Prerequisites:
REM   1. Docker Desktop installed and running
REM   2. VcXsrv (X Server) installed: https://sourceforge.net/projects/vcxsrv/
REM
REM Before running this script:
REM   1. Start XLaunch (VcXsrv) with these settings:
REM      - Multiple windows
REM      - Start no client
REM      - CHECK "Disable access control" (important!)
REM   2. Run this script
REM

echo.
echo ========================================
echo  StandardMDIGUI Docker Launcher
echo ========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Set DISPLAY for Windows
set DISPLAY=host.docker.internal:0

REM Handle config file argument
if not "%~1"=="" (
    echo Using config file: %~1
    if not exist "%~1" (
        echo ERROR: Config file not found: %~1
        pause
        exit /b 1
    )
    REM Create config directory if needed
    if not exist "%~dp0config" mkdir "%~dp0config"
    REM Copy config file to docker/config folder
    copy /Y "%~1" "%~dp0config\config.properties" >nul
    echo Config file copied to docker\config\config.properties
    echo.
)

echo Starting PostgreSQL and Application...
echo.

cd /d "%~dp0.."

REM Build and start containers
docker-compose up --build -d

echo.
echo Waiting for services to start...
timeout /t 5 /nobreak >nul

REM Show logs
echo.
echo ========================================
echo  Container Status:
echo ========================================
docker-compose ps

echo.
echo ========================================
echo  Application Logs (Ctrl+C to exit):
echo ========================================
docker-compose logs -f app

pause
