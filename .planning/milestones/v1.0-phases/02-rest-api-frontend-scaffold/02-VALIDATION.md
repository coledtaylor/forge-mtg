---
phase: 02
slug: rest-api-frontend-scaffold
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-18
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (backend)** | TestNG 7.10.2 |
| **Framework (frontend)** | vitest (installed with Vite scaffold) |
| **Config file (backend)** | forge-gui-web/pom.xml (surefire plugin) |
| **Config file (frontend)** | forge-gui-web/frontend/vitest.config.ts |
| **Quick run command** | `cd forge-gui-web && mvn test -pl . && cd frontend && npx vitest run` |
| **Full suite command** | `cd forge-gui-web && mvn test -pl . -am && cd frontend && npx vitest run` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -pl forge-gui-web` or `npx vitest run` depending on layer
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | API-01 | integration | `mvn test -pl forge-gui-web` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | API-02 | integration | `mvn test -pl forge-gui-web` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | DECK-02 | unit | `npx vitest run` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `forge-gui-web/src/test/java/forge/web/api/CardSearchApiTest.java` — stubs for API-01
- [ ] `forge-gui-web/src/test/java/forge/web/api/DeckApiTest.java` — stubs for API-02
- [ ] `forge-gui-web/frontend/src/test/` — vitest setup for DECK-02 (Scryfall URL construction)

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Card images display in browser | DECK-02 | Scryfall CDN redirect + browser rendering | Start server, open browser, search for "Lightning Bolt", verify image loads |
| HMR works during development | N/A | Dev server behavior | Run `npm run dev`, edit a component, verify hot reload |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
