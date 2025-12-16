#!/bin/bash

echo ""
echo "========================================"
echo "   Starting Forge Web Starter"
echo "========================================"
echo ""

# Navigate to script directory
cd "$(dirname "$0")"

echo "[1/3] Starting Docker containers..."
cd build
docker compose up -d

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to start Docker containers"
    echo "Make sure Docker is running"
    exit 1
fi

echo ""
echo "[2/3] Waiting for PostgreSQL to be ready..."
sleep 5

echo ""
echo "[3/3] Starting Spring Boot application..."
cd ..
mvn spring-boot:run

