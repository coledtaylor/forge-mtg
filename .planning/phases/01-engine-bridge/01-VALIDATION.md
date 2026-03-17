---
phase: 1
slug: engine-bridge
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-16
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TestNG 7.10.2 (matches existing Forge test infrastructure) |
| **Config file** | `forge-gui-web/src/test/resources/testng.xml` (Wave 0 creates) |
| **Quick run command** | `cd forge-gui-web && mvn test -pl . -Dtest=WebGuiGameTest` |
| **Full suite command** | `cd forge-gui-web && mvn test -pl .` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick test command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | API-06 | integration | `mvn test -Dtest=HeadlessInitTest` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | API-05 | unit | `mvn test -Dtest=WebGuiBaseTest` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 1 | API-03 | integration | `mvn test -Dtest=WebSocketConnectionTest` | ❌ W0 | ⬜ pending |
| 01-02-02 | 02 | 1 | API-04 | unit | `mvn test -Dtest=WebGuiGameTest` | ❌ W0 | ⬜ pending |
| 01-03-01 | 03 | 2 | API-03 | integration | `mvn test -Dtest=GameLoopIntegrationTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `forge-gui-web/pom.xml` — Maven module with TestNG dependency
- [ ] `forge-gui-web/src/test/java/forge/web/` — Test directory structure
- [ ] `forge-gui-web/src/test/resources/testng.xml` — TestNG configuration

*Test stubs created during plan execution, not upfront.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| WebSocket message inspection | API-03 | Need to visually verify JSON structure matches DTO spec | Connect wscat to ws://localhost:8080/game, start match, inspect messages |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
