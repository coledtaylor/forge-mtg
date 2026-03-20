---
phase: 08
slug: gameplay-ux
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 08 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TestNG 7.10.2 (backend) / TypeScript compile check (frontend) |
| **Config file** | forge-gui-web/pom.xml (TestNG via Maven Surefire) |
| **Quick run command** | `cd forge-gui-web && mvn test -q 2>&1 \| tail -5` |
| **Full suite command** | `cd forge-gui-web && mvn test -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~20 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick command
- **After every plan wave:** Run full suite
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Status |
|---------|------|------|-------------|-----------|--------|
| TBD | TBD | TBD | GUX-01 | visual + compile | ⬜ pending |
| TBD | TBD | TBD | GUX-02 | visual + compile | ⬜ pending |
| TBD | TBD | TBD | GUX-03 | visual + compile | ⬜ pending |
| TBD | TBD | TBD | GUX-04 | compile + grep | ⬜ pending |
| TBD | TBD | TBD | GUX-05 | compile | ⬜ pending |
| TBD | TBD | TBD | GUX-07 | compile | ⬜ pending |
| TBD | TBD | TBD | GUX-08 | compile + grep | ⬜ pending |
| TBD | TBD | TBD | CARD-04 | visual + compile | ⬜ pending |

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Priority pulse animation | GUX-01 | CSS animation timing | Start game, observe action bar glow when you have priority |
| Targeting highlight UX | GUX-03 | Interactive visual flow | Cast targeting spell, verify valid targets glow, invalid dim |
| Game log auto-scroll | GUX-04 | Scroll behavior | Play several turns, verify log scrolls to latest entry |
| Keyboard shortcuts | GUX-05 | Key event handling | Press Space to pass, Escape to cancel, verify responsiveness |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Feedback latency < 20s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
