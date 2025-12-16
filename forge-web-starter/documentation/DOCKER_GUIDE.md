# Docker Compose Quick Start Guide

This guide helps you set up PostgreSQL and Redis using Docker for local development.

## Prerequisites

- Docker Desktop installed ([Download](https://www.docker.com/products/docker-desktop/))
- Docker Compose (included with Docker Desktop)

## Quick Start

### 1. Start the databases

```powershell
# Start PostgreSQL and Redis
docker-compose up -d

# Check that services are running
docker-compose ps
```

You should see:
- `forge-postgres` running on port 5432
- `forge-redis` running on port 6379

### 2. Verify connections

```powershell
# Test PostgreSQL connection
docker exec -it forge-postgres psql -U forge -d forge -c "SELECT version();"

# Test Redis connection
docker exec -it forge-redis redis-cli ping
# Should respond: PONG
```

### 3. Run your Spring Boot app

```powershell
cd forge-web-starter
mvn spring-boot:run
```

Your app will automatically connect to the Docker databases using the settings in `application.yml`.

---

## Available Services

### PostgreSQL (Port 5432)
- **Database**: `forge`
- **Username**: `forge`
- **Password**: `forge`
- **Connection**: `jdbc:postgresql://localhost:5432/forge`

### Redis (Port 6379)
- **Host**: `localhost`
- **Port**: `6379`

### pgAdmin (Port 5050) - Optional
Web-based database management tool.

```powershell
# Start with GUI tools
docker-compose --profile tools up -d
```

Then open: http://localhost:5050
- **Email**: admin@forge.local
- **Password**: admin

To connect to PostgreSQL in pgAdmin:
- Host: `postgres` (or `host.docker.internal` from Windows)
- Port: `5432`
- Database: `forge`
- Username: `forge`
- Password: `forge`

### Redis Commander (Port 8081) - Optional
Web-based Redis management tool.

Open: http://localhost:8081

---

## Common Commands

### Start services
```powershell
# Start in background
docker-compose up -d

# Start and view logs
docker-compose up

# Start with GUI tools
docker-compose --profile tools up -d
```

### Stop services
```powershell
# Stop all services
docker-compose down

# Stop and remove volumes (DELETE ALL DATA!)
docker-compose down -v
```

### View logs
```powershell
# All services
docker-compose logs

# Specific service
docker-compose logs postgres
docker-compose logs redis

# Follow logs (live)
docker-compose logs -f
```

### Access database directly
```powershell
# PostgreSQL CLI
docker exec -it forge-postgres psql -U forge -d forge

# Common PostgreSQL commands:
# \dt          - List tables
# \d cards     - Describe cards table
# \q           - Quit

# Redis CLI
docker exec -it forge-redis redis-cli
```

### Restart services
```powershell
docker-compose restart
```

### Check service status
```powershell
docker-compose ps
```

---

## Data Persistence

Your data is stored in Docker volumes:
- `postgres_data` - PostgreSQL database files
- `redis_data` - Redis cache files
- `pgadmin_data` - pgAdmin settings

**Data persists even when containers are stopped!**

To view volumes:
```powershell
docker volume ls | Select-String "forge"
```

To completely delete data:
```powershell
docker-compose down -v
```

---

## Troubleshooting

### "Port already in use"

If PostgreSQL port 5432 is already used:

**Option 1:** Stop local PostgreSQL
```powershell
# Check what's using port 5432
netstat -ano | Select-String "5432"

# Stop PostgreSQL service
Stop-Service postgresql-x64-14
```

**Option 2:** Change Docker port
Edit `docker-compose.yml`:
```yaml
postgres:
  ports:
    - "5433:5432"  # Use 5433 externally
```

Then update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/forge
```

### "Connection refused"

Make sure containers are running:
```powershell
docker-compose ps
```

Restart if needed:
```powershell
docker-compose restart
```

### "Can't connect from Spring Boot"

On Windows, Docker uses `host.docker.internal` to access host machine.

If connecting from Spring Boot on **host** to Docker containers:
- Use `localhost` (default in application.yml) ✅

If connecting from **another container** to these containers:
- Use service names: `postgres`, `redis` ✅

### Reset everything

```powershell
# Stop and remove everything
docker-compose down -v

# Remove images
docker-compose down --rmi all -v

# Start fresh
docker-compose up -d
```

---

## Environment Variables

You can customize settings with a `.env` file:

```powershell
# Copy example
Copy-Item .env.example .env

# Edit .env with your values
notepad .env
```

Then restart:
```powershell
docker-compose down
docker-compose up -d
```

---

## Running Spring Boot with Docker

### Option 1: App on host, databases in Docker (Current)
- ✅ Easy debugging
- ✅ Fast code changes
- ✅ Direct IDE integration

```powershell
docker-compose up -d     # Start databases
mvn spring-boot:run      # Run app on host
```

### Option 2: Everything in Docker (Advanced)

Add this to `docker-compose.yml`:

```yaml
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/forge
      REDIS_HOST: redis
    depends_on:
      - postgres
      - redis
    networks:
      - forge-network
```

Then:
```powershell
docker-compose up -d
```

---

## Database Initialization

The `database-schema.sql` file is automatically run when PostgreSQL starts **for the first time**.

To re-initialize:
```powershell
# Remove volume
docker-compose down -v

# Start again (will run init script)
docker-compose up -d
```

Or manually run:
```powershell
docker exec -i forge-postgres psql -U forge -d forge < database-schema.sql
```

---

## Production Considerations

**Don't use this docker-compose.yml for production!**

For production:
- Use managed databases (AWS RDS, Azure Database, etc.)
- Don't expose database ports publicly
- Use secrets management (not plain passwords)
- Enable SSL/TLS
- Set up backups
- Use proper authentication

This is for **local development only**.

---

## Summary

```powershell
# Start everything
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop everything
docker-compose down

# Delete all data and start fresh
docker-compose down -v
docker-compose up -d
```

Now your databases are running in Docker, and your Spring Boot app can connect to them! 🚀

