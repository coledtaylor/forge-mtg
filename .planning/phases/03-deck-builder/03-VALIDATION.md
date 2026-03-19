---
phase: 03
slug: deck-builder
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-19
---

# Phase 03 — Validation Strategy

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
| 03-01-01 | 01 | 1 | DECK-03 | integration | `mvn test -pl forge-gui-web` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | DECK-10 | integration | `mvn test -pl forge-gui-web` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | DECK-01, DECK-06 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 03-02-02 | 02 | 2 | DECK-04 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 03-02-03 | 02 | 2 | DECK-05 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 03-02-04 | 02 | 2 | DECK-11, DECK-12, DECK-13 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 03-03-01 | 03 | 2 | DECK-07 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |
| 03-03-02 | 03 | 2 | DECK-08, DECK-09 | type-check | `npx tsc --noEmit` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Backend integration tests for new format validation endpoint
- [ ] Backend integration tests for commander update in DeckHandler

*Existing frontend type-check infrastructure covers frontend tasks.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Scryfall card images load in search results | DECK-01 | External CDN dependency | Search for "lightning", verify images render |
| Hover preview shows enlarged card near cursor | DECK-04/05 | Visual positioning | Hover over card in search/deck, verify floating preview |
| Mana curve chart renders correctly | DECK-07 | Visual chart rendering | Add 10+ cards, verify histogram bars |
| Color distribution pie renders | DECK-09 | Visual chart rendering | Add multi-color deck, verify pie segments |
| Format validation badges on illegal cards | DECK-10 | Visual inline badges | Add banned card to Standard deck, verify red badge |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
