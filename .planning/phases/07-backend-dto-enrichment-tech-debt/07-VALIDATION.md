---
phase: 07
slug: backend-dto-enrichment-tech-debt
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 07 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TestNG 7.10.2 (backend) / TypeScript compile check (frontend) |
| **Config file** | forge-gui-web/pom.xml (TestNG via Maven Surefire) |
| **Quick run command** | `cd forge-gui-web && mvn test -q 2>&1 | tail -5` |
| **Full suite command** | `cd forge-gui-web && mvn test -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd forge-gui-web && mvn test -q 2>&1 | tail -5`
- **After every plan wave:** Run `cd forge-gui-web && mvn test -q && cd frontend && npx tsc --noEmit`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | CARD-01 | integration | Verify CardDto has setCode/collectorNumber fields | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | CARD-02 | integration | Verify preferred printing selection logic | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | CARD-03 | integration | Verify &lang=en in Scryfall URLs | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | DEBT-01 | integration | Verify format validation returns 200 for Casual/Jumpstart | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | DEBT-02 | compile | Verify single GameStartConfig definition | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | DEBT-03 | integration | Verify AI deck resolution for all formats | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- Existing test infrastructure covers backend tests (TestNG + Maven Surefire)
- Frontend type checking via `tsc --noEmit` already configured
- No new test framework needed

*Existing infrastructure covers all phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Card images show correct art | CARD-02 | Visual verification of image quality | Start app, open deck editor, verify card images show recent core set art |
| English-only art | CARD-03 | Visual verification | Search for cards known to have Japanese alt-art (WAR planeswalkers), verify English shown |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
