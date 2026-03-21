---
phase: 11
slug: jumpstart-format
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 11 — Validation Strategy

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Maven compile (backend) / TypeScript compile (frontend) |
| **Quick run command** | `cd forge-gui-web && mvn compile -pl forge-gui-web -am -q 2>&1 \| tail -5` |
| **Full suite command** | `cd forge-gui-web && mvn compile -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~15 seconds |

## Manual-Only Verifications

| Behavior | Requirement | Why Manual |
|----------|-------------|------------|
| Pack editor enforces 20 cards | JUMP-01 | Format-specific UI constraints |
| Browse Packs shows Forge packs | JUMP-02 | Live data from engine |
| Dual-pack picker in lobby | JUMP-03 | Interactive UI flow |
| Game starts with merged 40-card deck | JUMP-03 | Full game loop |

**Approval:** pending
