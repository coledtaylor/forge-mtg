# 🐳 Docker Setup Complete!

I've created a complete Docker Compose setup for your database resources.

## What's Included

### 📦 Created Files

1. **`docker-compose.yml`** - Defines all services (PostgreSQL, Redis, pgAdmin, Redis Commander)
2. **`.env.example`** - Example environment variables
3. **`DOCKER_GUIDE.md`** - Complete usage guide and troubleshooting
4. Updated **`.gitignore`** - Prevents committing `.env` files

### 🔧 Services Included

| Service | Port | Purpose |
|---------|------|---------|
| **PostgreSQL** | 5432 | Main database for card storage |
| **Redis** | 6379 | Optional caching layer |
| **pgAdmin** | 5050 | Database GUI (optional) |
| **Redis Commander** | 8081 | Redis GUI (optional) |

---

## 🚀 Quick Start (3 Commands)

### 1. Start Docker services
```powershell
cd forge-web-starter
docker-compose up -d
```

### 2. Verify they're running
```powershell
docker-compose ps
```

You should see:
```
NAME                STATUS
forge-postgres      Up (healthy)
forge-redis         Up (healthy)
```

### 3. Run your Spring Boot app
```powershell
mvn spring-boot:run
```

**That's it!** Your app will automatically connect to the Docker databases.

---

## 📊 What Happens

```
┌─────────────────────────────────────┐
│     Your Spring Boot App            │
│     (localhost:8080)                │
│                                     │
│  Uses application.yml to connect:  │
│  - DATABASE_URL=localhost:5432      │
│  - REDIS_HOST=localhost:6379        │
└──────────┬──────────────────────────┘
           │
           ├─────────────────┐
           ▼                 ▼
    ┌─────────────┐   ┌─────────────┐
    │ PostgreSQL  │   │   Redis     │
    │   Docker    │   │   Docker    │
    │ Container   │   │ Container   │
    │ :5432       │   │ :6379       │
    └─────────────┘   └─────────────┘
```

---

## 🎯 How It Works

### The `docker-compose.yml` file:

1. **Creates PostgreSQL container**
   - Database: `forge`
   - User: `forge` / Password: `forge`
   - Runs initialization SQL from `database-schema.sql`
   - Data persists in Docker volume

2. **Creates Redis container**
   - Ready for caching
   - Data persists in Docker volume

3. **Optional GUI tools**
   - pgAdmin for database management
   - Redis Commander for cache inspection

### Your `application.yml` is already configured!

The environment variables like `${DATABASE_URL:jdbc:postgresql://localhost:5432/forge}` mean:
- Use environment variable `DATABASE_URL` if it exists
- Otherwise, use default: `jdbc:postgresql://localhost:5432/forge`

This works perfectly with Docker Compose! 🎉

---

## 📝 Common Commands

```powershell
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and DELETE all data
docker-compose down -v

# Restart services
docker-compose restart

# Check status
docker-compose ps

# Access PostgreSQL directly
docker exec -it forge-postgres psql -U forge -d forge

# Access Redis directly
docker exec -it forge-redis redis-cli
```

---

## 🌐 Access the GUIs (Optional)

### Start with GUI tools:
```powershell
docker-compose --profile tools up -d
```

### pgAdmin (Database GUI)
- URL: http://localhost:5050
- Email: `admin@forge.local`
- Password: `admin`

**Connect to PostgreSQL:**
- Host: `postgres` (or `host.docker.internal`)
- Port: `5432`
- Database: `forge`
- Username: `forge`
- Password: `forge`

### Redis Commander (Cache GUI)
- URL: http://localhost:8081

---

## 💾 Data Persistence

Your data is stored in Docker volumes:
- PostgreSQL data survives container restarts
- Redis cache survives container restarts

**To see volumes:**
```powershell
docker volume ls | Select-String "forge"
```

**To delete all data:**
```powershell
docker-compose down -v
```

---

## 🔧 Customization

### Change database credentials:

1. Copy `.env.example` to `.env`:
```powershell
Copy-Item .env.example .env
```

2. Edit `.env`:
```
POSTGRES_USER=myuser
POSTGRES_PASSWORD=mypassword
POSTGRES_DB=mydb
```

3. Restart:
```powershell
docker-compose down
docker-compose up -d
```

### Change ports (if 5432 is already in use):

Edit `docker-compose.yml`:
```yaml
postgres:
  ports:
    - "5433:5432"  # Use 5433 externally
```

Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/forge
```

---

## 🆚 Docker vs Application.yml

| File | Purpose |
|------|---------|
| **`docker-compose.yml`** | Creates and runs the database/Redis containers |
| **`application.yml`** | Tells your Spring Boot app how to connect to them |

Think of it as:
- Docker Compose = "Start a PostgreSQL server"
- application.yml = "Here's how to connect to that server"

---

## 🐛 Troubleshooting

### "Port 5432 already in use"

You have PostgreSQL already running locally:

**Option 1:** Stop local PostgreSQL
```powershell
Stop-Service postgresql-x64-14
```

**Option 2:** Change Docker port (see Customization above)

### "Connection refused"

Make sure containers are running:
```powershell
docker-compose ps
```

Both should show "Up (healthy)".

### "Can't connect from Spring Boot"

Check your `application.yml` uses `localhost` (not container names):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/forge  # ✅ Correct
    # NOT: jdbc:postgresql://postgres:5432/forge  # ❌ Wrong (only works inside Docker)
```

### Reset everything
```powershell
docker-compose down -v
docker-compose up -d
```

---

## 📚 Next Steps

1. ✅ **Start Docker services**: `docker-compose up -d`
2. ✅ **Add JPA dependencies** to `pom.xml` (see QUICKSTART_DATABASE.md)
3. ✅ **Run Spring Boot app**: `mvn spring-boot:run`
4. ✅ **Test the API**: Create cards, search, paginate
5. ✅ **View data in pgAdmin**: Use the GUI to inspect your database
6. ✅ **Import existing Forge cards**: Migrate from files to database

---

## 📖 Full Documentation

- **`DOCKER_GUIDE.md`** - Complete Docker usage guide
- **`QUICKSTART_DATABASE.md`** - Spring Boot + Database setup
- **`DATABASE_SOLUTION.md`** - Architecture and design decisions
- **`README_DATABASE_SOLUTION.md`** - Overview and summary

---

## ✨ Summary

You now have:
- ✅ PostgreSQL running in Docker (port 5432)
- ✅ Redis running in Docker (port 6379)
- ✅ Optional GUI tools (pgAdmin, Redis Commander)
- ✅ Data persistence (survives restarts)
- ✅ Spring Boot configured to connect automatically
- ✅ Complete documentation

**Just run `docker-compose up -d` and you're ready to go!** 🚀

