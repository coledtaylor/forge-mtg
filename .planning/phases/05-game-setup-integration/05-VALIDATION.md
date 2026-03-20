---
phase: 05
slug: game-setup-integration
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-19
---

# Phase 05 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | vitest (frontend), JUnit 5 + Javalin test (backend) |
| **Config file** | `forge-gui-web/frontend/vite.config.ts`, `forge-gui-web/pom.xml` |
| **Quick run command** | `cd forge-gui-web/frontend && npx tsc --noEmit` |
| **Full suite command** | `cd forge-gui-web && mvn test -pl forge-gui-web` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd forge-gui-web/frontend && npx tsc --noEmit`
- **After every plan wave:** Run `cd forge-gui-web && mvn test -pl forge-gui-web`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | SETUP-02 | compile | `mvn compile -pl forge-gui-web` | ✅ | ⬜ pending |
| 05-01-02 | 01 | 1 | SETUP-01, SETUP-02, SETUP-03, SETUP-04 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all tasks. No additional test framework setup needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Format filter shows matching decks only | SETUP-01 | Visual filtering | Select Standard, verify only Standard decks shown |
| Start Game launches game with selected deck | SETUP-02 | Full stack integration | Select deck, start game, verify game begins |
| "Play This Deck" from editor pre-fills lobby | SETUP-04 | Navigation flow | Edit deck, click Play, verify lobby has deck selected |
| Return to Lobby preserves deck selection | SETUP-04 | Post-game flow | Finish game, return to lobby, verify same deck |
| AI difficulty setting works | SETUP-02 | AI behavior | Set Hard difficulty, verify AI plays more aggressively |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
