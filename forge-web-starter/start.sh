#!/bin/bash
echo "Starting Forge Web Starter..."
cd "$(dirname "$0")"
docker-compose up -d
mvn spring-boot:run

