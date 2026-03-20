---
phase: 04
slug: game-board
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-19
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | vitest (frontend), JUnit 5 + Javalin test (backend) |
| **Config file** | `forge-gui-web/frontend/vite.config.ts` (frontend), `forge-gui-web/pom.xml` (backend) |
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
| 04-01-01 | 01 | 1 | GAME-01 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 04-01-02 | 01 | 1 | GAME-03, GAME-04, GAME-05 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 04-02-01 | 02 | 2 | GAME-01, GAME-02, GAME-10 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 04-02-02 | 02 | 2 | GAME-06, GAME-07, GAME-08, GAME-11 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 04-03-01 | 03 | 3 | GAME-09 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 04-03-02 | 03 | 3 | GAME-01 (integration) | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing frontend type-check infrastructure covers all tasks. No additional test framework setup needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Zones render with real-time WebSocket updates | GAME-01 | Requires running game engine | Start game, verify all zones update in real-time |
| Hand fans out with hover-to-raise | GAME-10 | Visual interaction | Hover over hand cards, verify raise + preview |
| Tapped cards rotate 90° | GAME-02 | Visual rendering | Tap a land, verify rotation |
| Combat attacker/blocker assignment | GAME-09 | Multi-step interaction | Declare attackers, assign blockers, verify lines |
| Phase strip highlights current phase | GAME-03 | Visual state | Play through turns, verify phase indicator |
| Stack shows spells in resolution order | GAME-06 | Game state visual | Cast spell with response, verify stack display |
| Priority pauses correctly | GAME-07 | Game flow | Verify game pauses at each priority, auto-passes when no actions |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
