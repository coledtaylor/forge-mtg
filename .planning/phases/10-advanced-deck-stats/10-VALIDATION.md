---
phase: 10
slug: advanced-deck-stats
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TypeScript compile check (frontend-only phase after DTO fix) |
| **Quick run command** | `cd forge-gui-web/frontend && npx tsc --noEmit 2>&1 \| tail -10` |
| **Full suite command** | `cd forge-gui-web && mvn compile -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~15 seconds |

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual |
|----------|-------------|------------|
| Removal/ramp/draw counts are reasonable | STATS-01 | Requires loading a real deck and verifying counts |
| Coverage grid shows correct green/red | STATS-02 | Visual verification |
| 4-of ratio and tutor count accurate | STATS-03 | Requires comparing to known deck |

---

## Validation Sign-Off

- [ ] All tasks have automated compile verify
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
