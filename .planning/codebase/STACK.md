# Technology Stack

**Analysis Date:** 2026-03-16

## Languages

**Primary:**
- Java 17 - Core language for entire codebase

**Secondary:**
- Tinylog scripting - Adventure mode map/script format (XML/TSX map files)

## Runtime

**Environment:**
- Java 17 (JDK required)
- Maven 3.8.1+

**Package Manager:**
- Apache Maven 3.x
- Lockfile: pom.xml (parent POM in `/c/Users/ColeT/Code/java-2D/forge-mtg/pom.xml`)

## Frameworks

**Core:**
- Forge Game Engine - Magic: The Gathering rule implementation and game logic (custom framework)
  - Located in `forge-core`, `forge-game`, `forge-ai` modules
  - Version: 2.0.12 (snapshot-based)

**Networking:**
- Jetty 9.4.57 - HTTP server and WebSocket support
  - Client: jetty-client
  - Server: jetty-server
  - Servlet: jetty-servlet

**Networking & Communication:**
- Netty 4.1.115 - Network I/O framework
- UPnP (org.jupnp 3.0.3) - Device discovery and network services

**Serialization:**
- XStream 1.4.21 - Object serialization to XML
- LZ4 compression (at.yawk.lz4-java 1.10.2)

**GUI Platforms:**
- LibGDX - Mobile and cross-platform rendering (referenced but external dependency)
- Swing/AWT - Desktop GUI (Java standard library)

## Key Dependencies

**Critical:**
- Guava 33.3.1-android - Collections, utilities
- Apache Commons Lang3 3.18.0 - String and utility functions
- Apache Commons Text 1.12.0 - Text processing
- Commons Math3 3.6.1 - Mathematical algorithms for AI

**Logging:**
- SLF4J 2.0.16 - Logging facade
- Tinylog 2.7.0 - Logging implementation
  - slf4j-tinylog - SLF4J adapter
  - tinylog-impl - Core implementation

**Monitoring & Error Tracking:**
- Sentry 8.21.1 - Error and exception reporting
  - Used in: `forge-game`, `forge-ai` for breadcrumb tracking and error capture

**Data Structures:**
- JGraphT 1.5.2 - Graph algorithms for game state analysis

**Testing:**
- TestNG 7.10.2 - Test framework (scope: test)
- PowerMock mockito2 - Mocking framework (scope: test)

**Feed & Content:**
- RSS Reader 3.8.2 - RSS feed parsing

**Utilities:**
- JetBrains Annotations 26.0.1 - @NotNull, @Nullable annotations

## Configuration

**Environment:**
- Configured via `forge.profile.properties` (`/ASSETS_DIR/forge.profile.properties`)
- Example template: `forge.profile.properties.example`
- Overrides in user preferences directory: `USER_PREFS_DIR`

**Build:**
- `pom.xml` - Maven build configuration
- Checkstyle 10.18.2 - Code style validation (configured in `checkstyle.xml`)
- Build profiles manage version, snapshot, and platform-specific configurations

**Logging Configuration:**
- Tinylog configuration via properties/YAML
- Application config: `application.properties` and `application.yml`

## Platform Requirements

**Development:**
- Java 17 (enforced via maven-enforcer-plugin)
- Maven 3.8.1+
- Windows, macOS, or Linux

**Production:**
- Java 17 runtime minimum
- Deployment as standalone JAR or platform-specific executable

**Target Platforms:**
- Desktop: Windows, macOS, Linux (via forge-gui-desktop)
- Mobile: Android (forge-gui-android), iOS (forge-gui-ios)
- Mobile Development: MobileVM deployment (forge-gui-mobile, forge-gui-mobile-dev)
- Web: Planned via forge-web-starter (Spring Boot-based, in development)

## Module Dependencies

**Module Hierarchy:**
```
forge-core
  ↓ (depends on)
forge-game
  ↓ (depends on)
forge-ai
  ↓ (depends on)
forge-gui (shared UI interfaces)
  ↓ (depends on)
forge-gui-desktop
forge-gui-mobile
forge-gui-android
forge-gui-ios
adventure-editor
```

**Core Modules:**
- `forge-core` - Card definitions, utilities, logging (2.7.0 tinylog)
- `forge-game` - Game logic, rules engine, Sentry integration (8.21.1)
- `forge-ai` - AI decision making, Sentry integration
- `forge-gui` - Shared UI interfaces (IGuiBase, IGuiGame), Jetty/Netty, XStream
- `forge-gui-desktop` - Swing-based desktop client
- `forge-gui-mobile` - LibGDX mobile client
- `forge-gui-android` - Android-specific GUI
- `forge-gui-ios` - iOS-specific GUI (RoboVM)
- `adventure-editor` - Adventure mode map/scenario editor tool

**Web Stack (in development):**
- `forge-web-starter` - Spring Boot backend
  - WebSocket support for real-time communication
  - REST endpoints for game state and card management
  - Database integration for card import

---

*Stack analysis: 2026-03-16*
