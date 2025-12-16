#!/bin/bash

echo ""
echo "========================================"
echo "   Resetting Forge Database"
echo "========================================"
echo ""
echo "This will DELETE all existing card data"
echo "and Docker volumes, then recreate the"
echo "database with the correct schema."
echo ""
read -p "Press Enter to continue or Ctrl+C to cancel..."

echo ""
echo "[1/4] Stopping Spring Boot application..."
echo "Please press Ctrl+C in the Spring Boot terminal if it's running"
sleep 3

echo ""
echo "[2/4] Stopping Docker containers and removing volumes..."
cd build
docker-compose down -v
echo ""

echo "[3/4] Starting Docker containers..."
docker-compose up -d
echo ""

echo "[4/4] Waiting for PostgreSQL to be ready..."
sleep 5

cd ..
echo ""
echo "========================================"
echo "   Database reset complete!"
echo "========================================"
echo ""

