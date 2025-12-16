@echo off
docker compose up -d
mvn spring-boot:run
cd /d "%~dp0"
echo.
echo Starting Forge Web Starter...

