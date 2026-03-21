---
phase: 09
slug: engine-integration-ux
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 09 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TestNG 7.10.2 (backend) / TypeScript compile check (frontend) |
| **Config file** | forge-gui-web/pom.xml |
| **Quick run command** | `cd forge-gui-web && mvn compile -pl forge-gui-web -am -q 2>&1 \| tail -5` |
| **Full suite command** | `cd forge-gui-web && mvn compile -pl forge-gui-web -am -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick command
- **After every plan wave:** Run full suite
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Requirement | Test Type | Status |
|---------|------|-------------|-----------|--------|
| TBD | TBD | GUX-06 | compile + visual | ⬜ pending |
| TBD | TBD | GUX-09 | compile + visual | ⬜ pending |

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Auto-pass skips when no legal plays | GUX-06 | Runtime game state | Play game, verify phases auto-skip when hand is empty |
| Phase strip flash on auto-pass | GUX-06 | CSS animation | Observe phase strip during auto-pass |
| Undo button appears only when available | GUX-09 | Game state dependent | Cast a spell, verify Undo button shows, press Z, verify it undoes |

---

## Validation Sign-Off

- [ ] All tasks have automated verify
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
