@echo off
echo.
echo ========================================
echo   Resetting Forge Database
echo ========================================
echo.
echo This will DELETE all existing card data
echo and Docker volumes, then recreate the
echo database with the correct schema.
echo.
pause

echo.
echo [1/4] Stopping Spring Boot application...
echo Please press Ctrl+C in the Spring Boot window if it's running
timeout /t 3 /nobreak > nul

echo.
echo [2/4] Stopping Docker containers and removing volumes...
cd build
docker-compose down -v
echo.

echo [3/4] Starting Docker containers...
docker-compose up -d
echo.

cd ..
echo.
echo ========================================
echo   Database reset complete!
echo ========================================
echo.

