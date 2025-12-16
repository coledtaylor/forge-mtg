@echo off
echo.
echo ========================================
echo   Starting Forge Web Starter
echo ========================================
echo.

REM Navigate to the script directory
cd /d "%~dp0"

echo [1/3] Starting Docker containers...
cd build
docker compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start Docker containers
    echo Make sure Docker Desktop is running
    pause
    exit /b 1
)

echo.
echo [2/3] Waiting for PostgreSQL to be ready...
timeout /t 5 /nobreak > nul

echo.
echo [3/3] Starting Spring Boot application...
cd ..
mvn spring-boot:run

pause
