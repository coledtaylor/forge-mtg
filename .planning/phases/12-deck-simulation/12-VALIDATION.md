---
phase: 12
slug: deck-simulation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 12 — Validation Strategy

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Maven compile (backend) / TypeScript compile (frontend) |
| **Quick run command** | `cd forge-gui-web && mvn compile -pl forge-gui-web -am -q 2>&1 \| tail -5` |
| **Full suite command** | `cd forge-gui-web && mvn compile -q && cd frontend && npx tsc --noEmit` |
| **Estimated runtime** | ~20 seconds |

## Manual-Only Verifications

| Behavior | Requirement | Why Manual |
|----------|-------------|------------|
| Simulation runs headless games | SIM-03 | Requires running engine |
| Live progress updates in dashboard | SIM-12 | SSE streaming behavior |
| Elo calculation produces reasonable values | SIM-10 | Statistical accuracy |
| Playstyle radar matches deck archetype | SIM-11 | Subjective evaluation |
| Results persisted and loadable | SIM-01 | File I/O + API round-trip |

**Approval:** pending
