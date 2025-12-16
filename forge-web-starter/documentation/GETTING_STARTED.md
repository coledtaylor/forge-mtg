# 🚀 Getting Started

Get the Forge Web Starter running in 5 minutes!

## Prerequisites

You already have these from Forge:
- ✅ **Java 17**
- ✅ **Maven**
- ✅ A web browser

## Quick Start

### Step 1: Navigate to the Project
```powershell
cd C:\Users\ColeT\Code\java-2D\forge-mtg\forge-web-starter
```

### Step 2: Run the Application
```powershell
mvn spring-boot:run
```

**First time?** Maven will download dependencies (takes 2-3 minutes).

### Step 3: Open Your Browser
- **Main page:** http://localhost:8080
- **Test client:** http://localhost:8080/test.html

You should see:
```
✅ Forge Web Starter is running!
Server: http://localhost:8080
```

---

## First Steps (10 minutes)

### 1. Explore the Test Client

Open **http://localhost:8080/test.html**

#### Try the REST API:
1. Click **"Get All Cards"** - See 4 sample cards
2. Click **"Get Card #1"** - Fetch a specific card
3. Click **"Search 'Bolt'"** - Search by name
4. Click **"Create New Card"** - Add a new card

#### Try WebSocket:
1. Click **"Create New Game"** - Creates a game session
2. Click **"Connect WebSocket"** - Establishes real-time connection
3. Click **"Player1 Draw"** - Watch hand update instantly!
4. Click **"Play"** on a card - Move it to the battlefield
5. Click **"Pass Turn"** - Switch to Player2

**What you're seeing:**
- REST API = Request → Response (one-time)
- WebSocket = Persistent connection (real-time updates)

### 2. Check the Console

Look at your PowerShell window. You'll see log messages like:
```
📋 GET /api/cards - Returning 4 cards
🎮 POST /api/game/new - Created game: game-123
🔌 WebSocket connected: session-456
🎴 Player1 drew a card
```

These show you what's happening behind the scenes!

### 3. Peek at the Code

Open these files in your IDE and read the comments:

1. **`ForgeWebStarterApplication.java`** - Entry point (18 lines)
2. **`CardController.java`** - REST API (90 lines)
3. **`GameWebSocketHandler.java`** - WebSocket (150 lines)

---

## Alternative: Run with Your IDE

### IntelliJ IDEA:
1. Open the `forge-web-starter` folder
2. Wait for Maven to import
3. Right-click `ForgeWebStarterApplication.java`
4. Select **"Run 'ForgeWebStarterApplication.main()'"**

### Eclipse:
1. Import as Maven project
2. Right-click on project → **Run As** → **Spring Boot App**

---

## Troubleshooting

### "Port 8080 is already in use"

**Option 1:** Kill the process using port 8080
```powershell
netstat -ano | findstr :8080
# Note the PID
taskkill /PID <process_id> /F
```

**Option 2:** Change the port

Edit `src/main/resources/application.yml`:
```yaml
server:
  port: 8081
```

Then use: http://localhost:8081

### "Java version mismatch"

Check your Java version:
```powershell
java -version
```

Should show Java 17. If not, update your `JAVA_HOME` environment variable.

### "Maven dependencies won't download"

Clear cache and retry:
```powershell
mvn clean install -U
```

### "Application starts but browser shows error"

1. Check console for errors
2. Make sure you're using http://localhost:8080 (not https)
3. Try a different browser
4. Clear browser cache

### "WebSocket won't connect"

1. Create a game first (click "Create New Game")
2. Check browser console (F12) for errors
3. Make sure the server is running

---

## What's Running?

When you start the application, you get:

### 1. REST API Server
- **Base URL:** http://localhost:8080
- **Card API:** http://localhost:8080/api/cards
- **Game API:** http://localhost:8080/api/game

### 2. WebSocket Server
- **Endpoint:** ws://localhost:8080/game
- Handles real-time game updates

### 3. Static File Server
- **Main page:** http://localhost:8080
- **Test client:** http://localhost:8080/test.html

---

## Next Steps

Now that it's running:

1. **Learn the basics** → [Learning Guide](LEARNING_GUIDE.md)
2. **Understand the architecture** → [Architecture](ARCHITECTURE.md)
3. **Add a database** → [Database Guide](DATABASE_GUIDE.md)
4. **Use Docker** → [Docker Guide](DOCKER_GUIDE.md)

---

## Quick Reference

### Start/Stop Commands
```powershell
# Start
mvn spring-boot:run

# Stop
Ctrl + C

# Clean build
mvn clean install

# Skip tests
mvn spring-boot:run -DskipTests
```

### Important URLs
- Main app: http://localhost:8080
- Test client: http://localhost:8080/test.html
- H2 Console: http://localhost:8080/h2-console (if using H2 database)

### Log Locations
- Console output (live)
- `target/` folder (build output)

---

## Tips for Success

1. **Keep the console visible** - Helpful log messages show what's happening
2. **Use the test client** - Visual way to understand the APIs
3. **Read code comments** - Every file is documented
4. **Experiment** - Break things and fix them!
5. **Check browser dev tools** - F12 shows network requests and WebSocket messages

---

**Ready to learn?** → Continue to the **[Learning Guide](LEARNING_GUIDE.md)** 🎓

