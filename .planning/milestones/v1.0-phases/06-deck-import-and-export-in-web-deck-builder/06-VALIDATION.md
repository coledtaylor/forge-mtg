---
phase: 06
slug: deck-import-and-export-in-web-deck-builder
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 06 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | vitest (frontend) / Maven Surefire (backend) |
| **Config file** | `forge-gui-web/frontend/vitest.config.ts` (if exists, else Wave 0 installs) |
| **Quick run command** | `cd forge-gui-web/frontend && npx vitest run --reporter=verbose` |
| **Full suite command** | `cd forge-gui-web/frontend && npx vitest run` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd forge-gui-web/frontend && npx vitest run --reporter=verbose`
- **After every plan wave:** Run `cd forge-gui-web/frontend && npx vitest run`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DECK-14 | integration | `curl POST /api/decks/parse` | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | DECK-14 | integration | `curl GET /api/decks/{name}/export` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 2 | DECK-14 | manual | Browser: paste deck list, verify preview | N/A | ⬜ pending |
| 06-02-02 | 02 | 2 | DECK-14 | manual | Browser: export deck, verify clipboard | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- Existing infrastructure covers all phase requirements. No new test framework needed.
- Backend endpoints can be verified via curl/httpie against running server.
- Frontend UI verification is manual (browser-based modals with clipboard interaction).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Import modal preview renders color-coded tokens | DECK-14 | UI rendering + visual feedback | Paste deck list, verify ✓/⚠/✗ icons per line |
| Clipboard copy works with toast confirmation | DECK-14 | Browser clipboard API requires user gesture | Click Copy, verify toast appears and clipboard contains text |
| File upload parses .dck/.dec/.txt files | DECK-14 | File input requires browser interaction | Upload test file, verify preview matches |
| Replace vs merge prompt appears | DECK-14 | Dialog interaction flow | Import into existing deck, verify prompt |

*All phase behaviors have automated verification for backend; frontend is manual due to modal/clipboard interaction.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
