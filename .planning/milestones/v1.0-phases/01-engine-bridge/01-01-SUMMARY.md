---
phase: 01-engine-bridge
plan: 01
subsystem: infra
tags: [maven, javalin, jackson, pseudo-edt, headless-init]

# Dependency graph
requires: []
provides:
  - forge-gui-web Maven module with Javalin 7, Jackson 2.21, TestNG
  - WebGuiBase IGuiBase implementation with pseudo-EDT (Web-EDT thread)
  - WebServer entry point with correct init ordering
  - Proven headless FModel initialization (30000+ cards loaded)
affects: [01-engine-bridge, 02-rest-api, 03-deck-builder, 04-game-board]

# Tech tracking
tech-stack:
  added: [javalin-7.0.1, jackson-databind-2.21.0, jackson-module-parameter-names-2.21.0, testng-7.10.2]
  patterns: [pseudo-edt-single-thread-executor, headless-iguibase, guibase-before-forgeconstants]

key-files:
  created:
    - forge-gui-web/pom.xml
    - forge-gui-web/src/main/java/forge/web/WebGuiBase.java
    - forge-gui-web/src/main/java/forge/web/WebServer.java
    - forge-gui-web/src/test/java/forge/web/WebGuiBaseTest.java
    - forge-gui-web/src/test/java/forge/web/HeadlessInitTest.java
  modified:
    - pom.xml

key-decisions:
  - "Used tinylog Logger (project standard) instead of SLF4J for WebGuiBase logging"
  - "Javalin 7 routes registered via config.routes.get() (v7 API change from v6)"
  - "JavalinJackson constructor requires (ObjectMapper, boolean) in v7"
  - "Assets dir for tests resolved to ../forge-gui/ when running from forge-gui-web/"

patterns-established:
  - "WebGuiBase pattern: all GUI dialogs log via tinylog and return sensible defaults"
  - "Init sequence: GuiBase.setInterface() MUST precede any ForgeConstants class loading"
  - "Test pattern: @BeforeSuite with static flag for one-time FModel init"

requirements-completed: [API-05, API-06]

# Metrics
duration: 9min
completed: 2026-03-18
---

# Phase 1 Plan 01: Maven Module + Headless Init Summary

**forge-gui-web Maven module with WebGuiBase pseudo-EDT and proven headless FModel initialization loading 30000+ cards**

## Performance

- **Duration:** 9 min
- **Started:** 2026-03-19T02:03:52Z
- **Completed:** 2026-03-19T02:13:38Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- forge-gui-web Maven module compiles with Javalin 7, Jackson 2.21, and all Forge dependencies
- WebGuiBase implements IGuiBase with single-threaded "Web-EDT" executor replacing Swing EDT
- FModel initializes headlessly, loading the full card database without Swing or SoundSystem
- 10 tests pass: 6 for pseudo-EDT threading, 4 for headless initialization proof

## Task Commits

Each task was committed atomically:

1. **Task 1: Create forge-gui-web Maven module and WebGuiBase** - `49c2844aa4` (feat)
2. **Task 2: WebServer entry point and HeadlessInitTest** - `5c408e2c79` (feat)

## Files Created/Modified
- `pom.xml` - Added forge-gui-web module declaration
- `forge-gui-web/pom.xml` - New Maven module with Javalin, Jackson, TestNG dependencies
- `forge-gui-web/src/main/java/forge/web/WebGuiBase.java` - IGuiBase implementation with pseudo-EDT
- `forge-gui-web/src/main/java/forge/web/WebServer.java` - Entry point with GuiBase -> FModel -> Javalin init order
- `forge-gui-web/src/test/java/forge/web/WebGuiBaseTest.java` - 6 tests for EDT threading behavior
- `forge-gui-web/src/test/java/forge/web/HeadlessInitTest.java` - 4 tests proving headless FModel init

## Decisions Made
- Used tinylog Logger (project standard) instead of SLF4J for WebGuiBase -- consistent with rest of Forge codebase
- Javalin 7 API differs from v6: routes registered via config.routes, JavalinJackson requires (ObjectMapper, boolean) constructor
- Jackson module package is `com.fasterxml.jackson.module.paramnames` (not `parameternames`)
- Test assets dir resolved via `../forge-gui/` relative path since Maven runs tests from module dir

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Javalin 7 API incompatibilities**
- **Found during:** Task 2 (WebServer compilation)
- **Issue:** Plan specified Javalin 6-style API (app.get(), new JavalinJackson(mapper), ParameterNamesModule import path)
- **Fix:** Updated to Javalin 7 API: config.routes.get(), JavalinJackson(mapper, false), correct import path
- **Files modified:** forge-gui-web/src/main/java/forge/web/WebServer.java
- **Verification:** mvn compile exits 0
- **Committed in:** 5c408e2c79 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Necessary API adaptation for Javalin 7. No scope creep.

## Issues Encountered
- Jackson 2.21.0 ParameterNamesModule is in package `com.fasterxml.jackson.module.paramnames`, not the expected `parameternames` -- fixed by inspecting the JAR
- Maven `-Dtest` filter applies to all reactor modules, causing failures in upstream modules -- worked around with `-Dsurefire.failIfNoSpecifiedTests=false`

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- forge-gui-web module is ready for REST endpoint development (Plan 02)
- WebGuiBase provides the threading foundation for game session management (Plan 03)
- FModel headless init pattern established for all future web server features

---
*Phase: 01-engine-bridge*
*Completed: 2026-03-18*
